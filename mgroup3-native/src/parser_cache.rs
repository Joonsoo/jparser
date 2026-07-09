//! rkyv 캐시: `ParserDataPlain` (= from_proto 결과물) 을 zero-copy archive 로 구워
//! 두 번째 실행부터 prost decode(~450–720ms)를 우회한다.
//!
//! 로드 경로 요약:
//! - `load_cached(path)`: `<path>.rkyv` 캐시가 있고 헤더(magic/버전/소스 해시)가
//!   맞으면 캐시를 **mmap** 해 rkyv 로 `ParserDataPlain` 을 복원해 반환 (proto 완전
//!   우회). 없거나 불일치/손상이면 `None` — 호출자가 proto 경로로 폴백한 뒤
//!   `write_cache` 로 best-effort 재작성한다.
//! - 캐시 무결성: 헤더에 소스 파일 내용 해시(xxh3) + payload 해시(xxh3)를 넣는다.
//!   소스가 바뀌면 소스 해시 불일치로 자동 무효화. **payload 전수 해시 검증은
//!   기본 OFF** — 대형 아카이브(300MB+)의 payload xxh3 는 전 페이지를 터치해
//!   mmap 의 lazy-fault 이점을 무너뜨린다. 캐시는 temp+atomic-rename 으로 쓰므로
//!   partial write 가 관측되지 않고, 소스 해시+버전 게이트로 stale 을 잡는다.
//!   payload 검증이 필요하면 env `MG3_CACHE_VERIFY_PAYLOAD=1` 또는 상수
//!   `FORCE_VERIFY_PAYLOAD=true` 로 opt-in.
//!
//! 접근 방식: mmap 한 바이트열에서 payload 는 64바이트 정렬 오프셋(HEADER_LEN)에서
//! 시작한다. mmap base 는 페이지(4K) 정렬이므로 payload 는 메모리상 64바이트 정렬 —
//! rkyv 의 16바이트 정렬 요구를 만족해 AlignedVec 복사 없이 in-place 로 접근한다.
//! `rkyv::access_unchecked` 로 아카이브를 잡은 뒤 `deserialize` 로 owned
//! `ParserDataPlain` 을 복원한다. bytecheck 기반 `rkyv::access` 도 지원
//! (`VERIFY_WITH_BYTECHECK`) — 동등성은 양쪽 모두 보장.

use std::fs;
use std::io::{Read, Write};
use std::path::{Path, PathBuf};

use memmap2::Mmap;
use prost::Message;
use rkyv::rancor;

use crate::parser_data::{ArchivedParserDataPlain, ParserDataPlain};
use crate::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;

/// 캐시 파일 magic (ASCII "MG3RKYV\0" 스타일 — 8바이트). payload 레이아웃/헤더
/// 포맷이 바뀌면 magic 도 함께 바뀌어 옛 캐시가 확실히 거부되도록 한다.
const MAGIC: [u8; 8] = *b"MG3RKYV2";

/// Plain 스키마 버전. `ParserDataPlain` 및 그 하위(임베드 prost 템플릿 포함)의
/// **아카이브 레이아웃**이 바뀌면 수동으로 bump 할 것. bump 하면 기존 캐시 파일은
/// 버전 불일치로 자동 무효화되어 proto 경로로 폴백 + 재작성된다.
///
/// bump 가 필요한 변경 예:
/// - parser_data.rs 의 Plain 구조체 필드 추가/삭제/타입 변경 (archived 필드 기준 —
///   `#[rkyv(with = Skip)]` 필드 추가/삭제 포함).
/// - build.rs 의 rkyv derive 대상 prost 타입 집합 변경.
/// - rkyv 메이저/아카이브 포맷 변경 (예: rkyv 0.8 → 0.9).
/// - 헤더 포맷/정렬 변경 (v1: 40B 헤더+fs::read → v2: 64B 정렬 헤더+mmap).
const PLAIN_SCHEMA_VERSION: u32 = 2;

/// bytecheck 검증(`rkyv::access`) 대신 `access_unchecked` 를 쓴다.
/// true 로 바꾸면 warm 로드가 전수 bytecheck 검증을 수행한다 (더 안전, 더 느림 —
/// 게다가 전 페이지를 fault-in 하므로 mmap 이점을 없앤다).
const VERIFY_WITH_BYTECHECK: bool = false;

/// payload xxh3 전수 검증을 상수로 강제(opt-in). 기본 false — 소스 해시+버전
/// 게이트 + atomic rename 으로 무결성을 확보하고, payload 검증은 필요 시에만 켠다.
/// 런타임으로도 env `MG3_CACHE_VERIFY_PAYLOAD=1` 로 켤 수 있다.
const FORCE_VERIFY_PAYLOAD: bool = false;

/// 캐시 헤더. 고정 크기, little-endian. payload 는 헤더 바로 뒤(64바이트 오프셋)에
/// 온다 — 그 오프셋은 16바이트 정렬이라, 페이지 정렬된 mmap base 위에서 payload 가
/// rkyv 의 정렬 요구를 만족한다.
///
/// 레이아웃(64바이트로 패딩):
///   [0..8)   magic
///   [8..12)  schema_version (u32 LE)
///   [12..16) flags (u32 LE) — bit0: payload_hash 가 유효(계산됨)한지
///   [16..24) source_hash (xxh3 of source file bytes, u64 LE)
///   [24..32) payload_hash (xxh3 of the rkyv payload, u64 LE; flags bit0=0 이면 무의미)
///   [32..40) payload_len (u64 LE)
///   [40..64) reserved (0) — 정렬 패딩/향후 확장
///   payload: 64..(64+payload_len)  ← 64바이트(=16의 배수) 정렬 오프셋
const HEADER_LEN: usize = 64;

/// flags bit0: payload_hash 필드가 계산되어 유효함.
const FLAG_PAYLOAD_HASH_PRESENT: u32 = 1;

fn cache_path_for(source: &Path) -> PathBuf {
    // sibling `<원본파일명>.rkyv` — 예: mulang-mg3-parserdata.pb.gz.rkyv
    let mut name = source.file_name().map(|s| s.to_os_string()).unwrap_or_default();
    name.push(".rkyv");
    source.with_file_name(name)
}

fn hash_bytes(bytes: &[u8]) -> u64 {
    xxhash_rust::xxh3::xxh3_64(bytes)
}

/// warm 로드에서 payload xxh3 를 검증할지 (기본 off; 상수 또는 env 로 opt-in).
fn should_verify_payload() -> bool {
    FORCE_VERIFY_PAYLOAD
        || std::env::var_os("MG3_CACHE_VERIFY_PAYLOAD")
            .map(|v| v != "0" && !v.is_empty())
            .unwrap_or(false)
}

fn write_u32(buf: &mut [u8], off: usize, v: u32) {
    buf[off..off + 4].copy_from_slice(&v.to_le_bytes());
}
fn write_u64(buf: &mut [u8], off: usize, v: u64) {
    buf[off..off + 8].copy_from_slice(&v.to_le_bytes());
}
fn read_u32(buf: &[u8], off: usize) -> u32 {
    u32::from_le_bytes(buf[off..off + 4].try_into().unwrap())
}
fn read_u64(buf: &[u8], off: usize) -> u64 {
    u64::from_le_bytes(buf[off..off + 8].try_into().unwrap())
}

/// 소스 파일(`source_path`, 예: *.pb.gz)에 대응하는 rkyv 캐시가 유효하면
/// `ParserDataPlain` 을 복원해 반환한다. 없거나(부재), 헤더 불일치(버전/소스 해시),
/// 손상(파싱 실패, 또는 payload 검증 opt-in 시 해시 불일치)이면 `None` — 호출자는
/// proto 경로로 폴백한다.
///
/// `source_bytes` 는 소스 파일의 원본(압축된 채) 바이트 — 소스 해시 대조에 쓴다.
/// 호출자가 이미 파일을 읽었다면 재사용해 중복 IO 를 피한다.
///
/// 복원된 `transitive_initial_cond_symbols` 는 아카이브에서 스킵되므로(파생 데이터)
/// 여기서 `recompute_derived` 로 다시 채운다.
pub fn load_cached(source_path: &Path, source_bytes: &[u8]) -> Option<ParserDataPlain> {
    let cache_path = cache_path_for(source_path);
    let file = fs::File::open(&cache_path).ok()?;
    // SAFETY: mmap 은 파일이 로드 중 외부에서 truncate/수정되면 UB 다. 캐시는
    // temp+atomic-rename 으로만 갱신되고(같은 프로세스/신뢰 경로), 소스 해시가
    // 바뀌면 재작성이 rename 으로 원자적으로 교체하므로, 우리 쓰기 규약 하에서는
    // 로드 중 in-place 변조가 일어나지 않는다.
    let mmap = unsafe { Mmap::map(&file).ok()? };
    let raw: &[u8] = &mmap;

    if raw.len() < HEADER_LEN {
        return None;
    }
    if raw[0..8] != MAGIC {
        return None;
    }
    if read_u32(raw, 8) != PLAIN_SCHEMA_VERSION {
        return None;
    }
    let flags = read_u32(raw, 12);
    let source_hash = read_u64(raw, 16);
    let payload_hash = read_u64(raw, 24);
    let payload_len = read_u64(raw, 32) as usize;
    if HEADER_LEN + payload_len != raw.len() {
        return None;
    }
    // 소스 해시 게이트 — 이것만은 항상 검증한다(소스 8B→110MB 를 6.8MB gz 로 읽는
    // 비용은 어차피 호출자가 냈고, 소스가 바뀌면 반드시 무효화해야 한다). 소스
    // 바이트는 이미 손에 있으므로 추가 IO 없음.
    if hash_bytes(source_bytes) != source_hash {
        return None;
    }

    let payload = &raw[HEADER_LEN..];
    // payload 정렬 확인: mmap base 는 페이지 정렬, HEADER_LEN 은 16의 배수이므로
    // 정상 경로에선 항상 성립한다. 만약(비정상) 어긋나면 access_unchecked 가 UB 이니
    // 캐시 무효로 취급하고 폴백한다.
    if (payload.as_ptr() as usize) % 16 != 0 {
        return None;
    }

    // payload 전수 해시 검증은 opt-in (기본 off). 켜져 있고 헤더에 유효한 해시가
    // 없으면(옛 규약) 보수적으로 무효 처리.
    if should_verify_payload() {
        if flags & FLAG_PAYLOAD_HASH_PRESENT == 0 {
            return None;
        }
        if hash_bytes(payload) != payload_hash {
            return None;
        }
    }

    let mut plain = if VERIFY_WITH_BYTECHECK {
        let archived = rkyv::access::<ArchivedParserDataPlain, rancor::Error>(payload).ok()?;
        rkyv::deserialize::<ParserDataPlain, rancor::Error>(archived).ok()?
    } else {
        // SAFETY: payload 는 우리가 구운 정확한 바이트열이며(소스 해시 게이트 통과,
        // 신뢰 경로), 위에서 16바이트 정렬을 확인했다. bytecheck 없이 접근한다.
        let archived = unsafe { rkyv::access_unchecked::<ArchivedParserDataPlain>(payload) };
        rkyv::deserialize::<ParserDataPlain, rancor::Error>(archived).ok()?
    };

    // 파생 필드는 아카이브에서 스킵됐다 — path_roots 로부터 재계산.
    plain.recompute_derived();
    Some(plain)
}

/// `plain` 을 rkyv 로 직렬화해 `<source_path>.rkyv` 에 best-effort 로 쓴다.
/// temp 파일 + atomic rename 으로 동시 실행 프로세스 경합에 안전하다. 디렉토리가
/// 읽기 전용이거나 어떤 이유로든 실패하면 조용히 스킵한다 (Err 반환하지 않음 —
/// 캐시는 순수 최적화).
pub fn write_cache(source_path: &Path, source_bytes: &[u8], plain: &ParserDataPlain) {
    let _ = try_write_cache(source_path, source_bytes, plain);
}

fn try_write_cache(
    source_path: &Path,
    source_bytes: &[u8],
    plain: &ParserDataPlain,
) -> std::io::Result<()> {
    let payload = rkyv::to_bytes::<rancor::Error>(plain)
        .map_err(|e| std::io::Error::new(std::io::ErrorKind::Other, e.to_string()))?;
    let source_hash = hash_bytes(source_bytes);
    // payload 해시는 항상 계산해 헤더에 저장한다(쓰기는 어차피 전 바이트를 만지므로
    // 추가 비용이 미미하고, opt-in 검증을 켰을 때 대조 대상이 된다).
    let payload_hash = hash_bytes(&payload);

    let mut header = [0u8; HEADER_LEN];
    header[0..8].copy_from_slice(&MAGIC);
    write_u32(&mut header, 8, PLAIN_SCHEMA_VERSION);
    write_u32(&mut header, 12, FLAG_PAYLOAD_HASH_PRESENT);
    write_u64(&mut header, 16, source_hash);
    write_u64(&mut header, 24, payload_hash);
    write_u64(&mut header, 32, payload.len() as u64);
    // [40..64) reserved, already zero.

    let cache_path = cache_path_for(source_path);
    let dir = cache_path.parent().unwrap_or_else(|| Path::new("."));
    // 같은 디렉토리의 고유 temp 파일 (pid 로 프로세스 간 충돌 회피) → atomic rename.
    let tmp_path = dir.join(format!(
        ".{}.tmp.{}",
        cache_path.file_name().and_then(|s| s.to_str()).unwrap_or("cache"),
        std::process::id()
    ));
    {
        let mut f = fs::File::create(&tmp_path)?;
        f.write_all(&header)?;
        f.write_all(&payload)?;
        f.flush()?;
    }
    match fs::rename(&tmp_path, &cache_path) {
        Ok(()) => Ok(()),
        Err(e) => {
            let _ = fs::remove_file(&tmp_path);
            Err(e)
        }
    }
}

/// 파일에서 `ParserDataPlain` 을 로드하되 rkyv 캐시를 먼저 시도한다.
///
/// 1) 소스 파일을 읽는다 (`.gz` 면 그대로 압축 바이트 — 캐시 소스 해시는 압축된
///    원본 바이트 기준).
/// 2) 유효한 `<path>.rkyv` 캐시가 있으면 mmap+rkyv 로 복원해 반환 (proto 완전 우회).
/// 3) 없으면 proto 경로: (gunzip →) prost decode → from_proto. 그 결과를
///    best-effort 로 캐시에 쓴 뒤 반환한다.
///
/// 반환된 `ParserDataPlain` 은 어느 경로든 `from_proto` 결과와 동일하다.
pub fn load_plain_from_file(source_path: &Path) -> std::io::Result<ParserDataPlain> {
    let source_bytes = fs::read(source_path)?;

    if let Some(plain) = load_cached(source_path, &source_bytes) {
        return Ok(plain);
    }

    // 캐시 미스/무효 — proto 경로로 폴백.
    let pb_bytes: Vec<u8> = if source_path
        .extension()
        .map(|e| e.eq_ignore_ascii_case("gz"))
        .unwrap_or(false)
    {
        let mut buf = Vec::new();
        flate2::read::GzDecoder::new(source_bytes.as_slice()).read_to_end(&mut buf)?;
        buf
    } else {
        source_bytes.clone()
    };
    let data = Mgroup3ParserData::decode(pb_bytes.as_slice())
        .map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e.to_string()))?;
    let plain = ParserDataPlain::from_proto(data);

    // best-effort 캐시 작성 (실패해도 무시).
    write_cache(source_path, &source_bytes, &plain);

    Ok(plain)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn cache_path_appends_rkyv() {
        let p = Path::new("/a/b/mulang-mg3-parserdata.pb.gz");
        assert_eq!(
            cache_path_for(p),
            PathBuf::from("/a/b/mulang-mg3-parserdata.pb.gz.rkyv")
        );
    }

    #[test]
    fn load_missing_cache_is_none() {
        let dir = std::env::temp_dir().join(format!("mg3_cache_test_{}", std::process::id()));
        let _ = fs::create_dir_all(&dir);
        let src = dir.join("nonexistent.pb");
        assert!(load_cached(&src, b"anything").is_none());
        let _ = fs::remove_dir_all(&dir);
    }

    #[test]
    fn header_len_is_16_aligned() {
        // payload 가 16바이트 정렬 오프셋에서 시작해야 mmap in-place access 가 UB 를
        // 피한다. mmap base 는 페이지 정렬이므로 HEADER_LEN 이 16의 배수면 충분.
        assert_eq!(HEADER_LEN % 16, 0);
    }
}
