# bibix4 프로젝트에서 mgroup3 + Rust FFI 파서 사용하기

작성: 2026-07-04. CDG 문법 하나로 "Rust 네이티브 파서 (cdylib) + Kotlin typed
AST + FFM 브릿지" 전체를 bibix4 로 생성·사용하는 방법. 살아있는 실사용 예는
**mulang 리포의 `build.bbx4` `parser` 블록** — 이 문서의 예제는 그것을 일반화한
것이다.

## 0. 무엇이 생성되나

`jparser.mgroup3.genRustParser(cdgFile, ...)` 는 `RustParser` 를 반환한다
(jparser `build.bbx4` mgroup3 네임스페이스, :363-447):

| 필드 | 내용 |
|---|---|
| `parserData` | mgroup3 parserdata (`.gz` 면 gzip; `trimParserData=true` 기본 — 런타임이 안 읽는 NGrammar 디버그 정보 제거) |
| `protoSchema` | AST 직렬화용 `ast.proto` (패키지 = `<astifier 패키지>.ast`) |
| `rustSrcsRoot` | mgroup3-native 에 path-dependency 를 갖는 Rust crate (파싱 + AST walk + proto encode; `ffi` feature 로 cdylib) |
| `kotlinAdtRoot` | `<Ast>.kt` (typed AST + kernels-history astifier — mg2/mg3 겸용) + `<Ast>ProtoBinding.kt` (proto bytes → typed AST 디코더) |

`jparser.mgroup3.cargoDylib(rustSrcsRoot, crateName)` 가 그 crate 를
`cargo build --features ffi --release` 로 빌드해 cdylib 파일을 반환한다.

런타임 흐름: Kotlin 이 FFM 으로 cdylib 로드 → `mgroup3_gen_parse_ast` 가
parse→walk→proto-encode 를 Rust 에서 수행 → Kotlin 은 proto bytes 를
`<Ast>ProtoBinding.fromProtoBytes` 로 디코드.

## 1. 전제

- `bibix.deps` 에 jparser: `jp = local("../jparser")` (또는 git external).
  jparser 쪽 리포에 `mgroup3-native/` crate 가 있어야 한다 (생성 crate 의
  path dependency).
- `cargo` 가 PATH 에 (rustup 표준 설치).
- JVM 22+ (FFM). 경고 없애려면 실행 시 `--enable-native-access=ALL-UNNAMED`.
- protobuf: 생성된 `ast.proto` 를 java 로 컴파일할 rule
  (`protobuf.schema`/`protobuf.java`) + `protobuf-java` 런타임.

## 2. build.bbx4 배선 (예제)

```
import jp.jparser

parser(basePath="parser") {
  // === 파서 생성 ===
  // 문법 변경 시 `bibix4 parser.generate` 로 재생성 후 산출물 커밋.
  gen3 = jparser.mgroup3.genRustParser(
    cdgFile = "../grammar/mylang.cdg",
    // ★ 명시 필수: 생성 crate 가 path dependency 로 참조할 jparser 의
    // mgroup3-native 위치. external def 안의 string→path coercion 이
    // caller basePath 기준으로 붙는 문제가 있어 default 를 둘 수 없다.
    mgroup3NativeDir = "../../jparser/mgroup3-native",
    // 생성 Kotlin AST 클래스 FQCN — 마지막 컴포넌트가 클래스명.
    // proto 패키지는 "<패키지>.ast", 바인딩은 "<클래스명>ProtoBinding".
    astifierClassName = "com.example.mylang.MylangAst",
    // ★ 문법별 고유하게 — dylib 파일명/심볼이 crate 이름에서 나오므로
    // 여러 문법의 파서가 한 JVM 에 공존하려면 달라야 한다.
    crateName = "mylang-parser",
    parserDataFileName = "mylang-mg3-parserdata.pb.gz",
  )
  nativeDylib = jparser.mgroup3.cargoDylib(gen3.rustSrcsRoot, crateName = "mylang-parser")

  // 산출물을 리포에 커밋되는 위치로 복사 (리소스 규약은 §3).
  @action
  def generate() {
    file.clearDirectory("generated/kotlin")
    file.clearDirectory("generated/proto")
    file.copyDirectory(gen3.kotlinAdtRoot.await(), "generated/kotlin")
    file.copyFile(gen3.protoSchema.await(), "generated/proto/ast.proto")
    file.copyFile(gen3.parserData.await(), "generated/resources/mylang-mg3-parserdata.pb.gz")
    let dylibDir = "generated/resources/native/${jparser.mgroup3.nativeResourceDirName()}"
    file.copyFile(nativeDylib.await(), "${dylibDir}/${jparser.mgroup3.dylibFileNameOf("mylang-parser")}")
  }

  // 생성된 ast.proto → java (ProtoBinding 이 참조하는 메시지들).
  astProto = protobuf.schema(
    srcs = withBasePath("generated/proto") { glob("**.proto") },
  )
  astProtoLib = java.library(
    srcs = protobuf.java(schema=astProto).javaFiles,
    deps = [maven.javaLib("com.google.protobuf", "protobuf-java", "4.31.1")],
  )

  // 생성 Kotlin AST + astifier. jparser.ktparser.main 은 mg2 런타임
  // (kernels-history astifier 용 — mg2 fallback 안 쓸 거면도 AST 코드가 참조).
  generated = ktjvm.library(
    srcs = withBasePath("generated/kotlin") { glob("**.kt") },
    deps = [jparser.ktparser.main, parser.astProtoLib],
    resourceDirs = ["generated/resources"],
  )

  // 손으로 쓰는 로더/파사드. jparser.mgroup3.nativeParser 가
  // GeneratedAstNativeBridge (FFM) 를 제공한다.
  main = ktjvm.library(
    srcs = withBasePath("main/kotlin") { glob("**.kt") },
    deps = [parser.generated, jparser.mgroup3.nativeParser],
  )
}
```

## 3. 리소스 규약과 런타임 로더

`generate` 액션이 채우는 리소스 (classpath 루트 기준):

```
/<parserDataFileName>                  예: /mylang-mg3-parserdata.pb.gz
/native/<os>-<arch>/<dylib>            예: /native/darwin-aarch64/libmylang_parser.dylib
```

`<os>` ∈ darwin/linux/windows, `<arch>` 는 JVM `os.arch` (예: aarch64).
dylib 파일명은 crate 이름의 `-`→`_` 치환에 플랫폼 접두/확장자
(`lib*.dylib`/`lib*.so`/`*.dll`) — 빌드측 `dylibFileNameOf` 와 런타임 로더가
같은 규칙을 써야 한다.

런타임 로더는 mulang 의 `NativeMulangParser.kt` 를 그대로 본뜨면 된다.
**영구 content-addressed 캐시 디렉토리** 에 추출하는 것이 핵심이다 (FFM
libraryLookup 과 parserdata 로더가 파일 경로를 요구하고, Rust 파서가 첫
로드에서 parserdata 옆에 `.rkyv` 캐시를 구워 이후 로드를 mmap 으로
가속(~수백 ms → ~125ms)하므로 추출본이 실행 간에 살아남아야 한다 — 매 실행
fresh temp dir 로 추출하면 rkyv 캐시가 매번 버려진다):

```kotlin
val dylibBytes = cls.getResourceAsStream("/native/${platformDir()}/$libName")?.readAllBytes() ?: return null
val dataBytes = cls.getResourceAsStream("/mylang-mg3-parserdata.pb.gz")?.readAllBytes() ?: return null
// ${user.home}/.cache/<프로젝트>-native/<두 리소스 내용 해시>/ 에 추출.
//  - parserdata 는 .gz 원본 그대로 (JVM 에서 gunzip 하지 않음 — Rust 가 .gz 처리;
//    파일명이 .gz 로 끝나야 그 브랜치를 탄다. rkyv 캐시는 Rust 가 옆에 굽는다)
//  - temp 파일 + atomic move 로 동시 실행 경합 안전
//  - 캐시 디렉토리 생성/쓰기 실패 시 fresh temp dir 로 폴백 (빌드툴을 깨지 않게)
val bridge = GeneratedAstNativeBridge(dylibPath)   // jparser.mgroup3.nativeParser
val handle = bridge.newParserFromFileCached(dataPath)  // rkyv 캐시 경유 로드
// ...
fun parse(text: String): MylangAst.CompileUnit =
  MylangAstProtoBinding.fromProtoBytes(bridge.parseAst(handle, text))
```

(`newParserFromFile` 도 여전히 존재한다 — 캐시 파일을 만들면 안 되는 환경이면
그쪽을 쓰면 된다. 내용 해시를 디렉토리 이름에 쓰므로 리소스가 갱신되면
자동으로 새 디렉토리가 되고 stale 캐시 문제가 없다.)

파사드 패턴 (mulang `MulangParser.kt`): native 를 `tryLoad()` 로 우선 사용,
(a) 리소스 없음/로드 실패 (미지원 플랫폼) → mg2 Kotlin 파서로 fallback,
(b) 입력 거부 (`GeneratedAstParseException` — **위치 정보 없음**) → mg2 로
재파싱해 line/col 있는 상세 에러를 던지게 한다 (거부는 드물어 재파싱 비용
무시 가능). mg2 fallback 은 mg2 parserdata 리소스가 별도로 필요하며, 생성된
`<Ast>.kt` 는 mgroup3 kernelsHistory 가 mg2 와 좌표 호환이라 양쪽 겸용이다.

## 4. 재생성 워크플로

문법(.cdg) 변경 시:

```
bibix4 parser.generate        # 재생성 + generated/ 교체
# → generated/kotlin, generated/proto, generated/resources 커밋
```

- 산출물은 커밋한다 (사용측 빌드가 cargo/genCli 를 요구하지 않도록).
- dylib 은 **빌드한 플랫폼 것만** 갱신된다 — 다른 플랫폼 지원이 필요하면
  각 플랫폼에서 `generate` 를 돌려 해당 `native/<os>-<arch>/` 를 커밋.
- 검증: mulang 은 `NativeParserDiffTest` (실코퍼스에서 native AST == mg2 AST)
  를 게이트로 둔다 — 같은 패턴 권장.

## 5. 주의사항 모음

- **`mgroup3NativeDir` 는 반드시 명시** (§2 주석 — bibix4 external def 의
  string→path coercion 이슈). 경로는 호출측 basePath 기준 상대경로.
- **`crateName` 은 문법별 고유하게**. 기본값 그대로 두 문법을 쓰면 dylib
  파일명이 충돌한다.
- 생성 crate 는 jparser 리포의 `mgroup3-native` 를 **path dependency** 로
  참조한다 — 사용측 리포를 옮기면 `mgroup3NativeDir` 도 따라 조정.
  (커밋하는 것은 산출물뿐이라 crate 자체는 빌드 캐시에만 존재.)
- FFM: JVM 22+, `--enable-native-access=ALL-UNNAMED` 권장.
- `GeneratedAstParseException` 은 위치 정보가 없다 — 상세 에러가 필요하면
  §3 의 mg2 재파싱 패턴.
- proto 스키마를 바꾸는 것은 문법 재생성뿐 — `ast.proto` 는 손으로 수정하지
  말 것 (ProtoBinding/Rust encoder 와 함께 생성됨).
- jparser 쪽 mgroup3 런타임/생성기가 바뀌면 (parserdata 포맷 등) 사용측도
  `parser.generate` 재실행이 필요할 수 있다 — jparser 커밋 로그의 mgroup3
  항목 참고.

## 6. 관련 코드/문서

- 규칙 정의: jparser `build.bbx4` mgroup3 네임스페이스 (`genRustParser`,
  `cargoDylib`, `dylibFileNameOf`, `nativeResourceDirName`).
- FFM 브릿지: `mgroup3/nativeParser/kotlin/.../GeneratedAstNativeBridge.kt`.
- 실사용 예: mulang 리포 `build.bbx4` `parser` 블록,
  `parser/main/kotlin/com/giyeok/mulang/{NativeMulangParser,MulangParser}.kt`.
- 생성 파이프라인 내부 (GenCli stage 들): `mgroup3/docs/phase_b_proto_design.md`.
- 테스트로 보는 e2e: `Mgroup3GenAstFfiTest` (`bibix4 runMgroup3GenAstFfiTest`,
  env `MGROUP3_GEN_FFI=1`) — GenCli 로 crate 생성 → cargo build → FFM 로드 →
  Rust == Kotlin 교차 검증.
