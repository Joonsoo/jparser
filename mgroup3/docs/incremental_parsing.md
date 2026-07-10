# mgroup3 incremental parsing — 수렴 프로브 결과와 트랙 계획

작성: 2026-07-10. 상태: **I0~I3 구현 완료 (native + 생성-crate FFI), mulang LSP 배선은
노트 (미배선).** 소비자: mulang 리포의 LSP (현재 bibix4 빌드파일 전용) — 키스트로크
편집마다 재파스.
프로브: `mgroup3-native/src/bin/incremental_probe.rs` (read-only, lib 무변경).

## 0. 아이디어

- mgroup3 는 엄격한 좌→우 단일 패스 + gen-인덱스 상태라 **prefix 체크포인트 재개
  (Phase I) 가 자명하게 건전**하고, 상태가 불변·구조공유라 체크포인트가 싸다.
- **Phase II (suffix 재사용)**: 편집 구간 재파스가 구 파스 상태와 "gen-shift
  modulo 재일치"하는 지점부터 구 상태·기록을 splice. CDG/ACP 는 비국소 의무가
  **명시적 조건 집합**으로 상태에 드러나므로, "조건 집합 포함 상태 완전 일치"가
  건전한 재사용 기준이 된다 (숨은 비국소성이 없음 — 이 형식론의 구조적 이점).

## 1. 수렴 거리 프로브 결과 (2026-07-10)

방법: 원본/편집 파스의 per-gen 지문 비교. gen 앵커는 편집위치-인지 인코딩
(g≤p 절대 / g>p 는 cur−g 오프셋 — shift-동치가 지문 일치가 되도록). semantic
(보고 shadow 제외) / strict (포함) 이중 기준. **자기검증 통과** (identity 5,523
gen 전일치, no-op 편집 즉시 수렴). 코퍼스: jar.bbx, cc.bbx, ccgen.mu,
chain_boundaries.mu × 편집 44종 (삽입/삭제/치환 × 위치 + 주석/문자열 내부).

| 지표 | semantic | strict |
|---|---|---|
| 수렴 거리 median | **0** | 1 |
| mean / p90 / max | 18.5 / 22 / 229 | 19.0 / 22 / 229 |
| 즉시 수렴 (=0) | 24/38 | — |
| 미수렴 / 재발산 | **0 / 0** | 0 / 0 |

- **핵심 답**: block-spanning 워처가 수렴을 수백~수천 gen 으로 늘릴 것이라는
  우려는 실측으로 반박됨 — 편집 후 **수십 자 안에 상태가 재정렬**되고, 늦는
  케이스의 블로커도 워처 폭발이 아니라 main 체인/조건 앵커의 재정렬 지연이다.
- **strict−semantic 격차 ≈ 1 gen**: 보고 shadow 는 라이브 상태 재정렬 한 스텝
  뒤에 따라온다 → **history splice 의 추가 비용이 거의 없다.**
- Phase I 단독의 잔여 비용 (재파스 꼬리): 편집 위치 25%/50%/75% → 평균 14.1k /
  6.3k / 3.9k 자 — 초중반 편집의 긴 꼬리가 Phase II 의 회수 대상.

**판정: Phase II 까지 착수 가치 확정.** 편집당 재파스가 "체크포인트→편집
구간→수십 자"로 떨어질 수 있는 헤드룸이 실측으로 존재한다.

## 2. 계획 (설계 문서: `incremental_parsing_design.md`)

- **Phase I0** — ParseSession + 주기 체크포인트 (K gen 링) + prefix 재개.
  게이트: 전체 재파스와 kernels history·accept **byte-identical** (편집 퍼징
  차등 오라클 — 완벽한 정확성 게이트가 공짜로 존재).
- **Phase I1** — per-gen strict 지문으로 수렴 감지 (splice 없이 "재사용
  가능했던 양" 카운터 — 실현 이득 측정). 구 파스 지문은 `BaselineWalker` 로 lazy
  산출 (upfront 전체 트레이스 아님 — design §2.1).
- **Phase I2** — splice: 동기화 지점에서 구조적 완전 일치 1회 검증 (해시 충돌
  차단) 후 suffix 상태 gen-rebase + history 를 세그먼트+오프셋 rope 로 splice.
- **Phase I3** — FFI 세션 API + mulang LSP 배선.

## 3. FFI 세션 표면 (I3 — 구현 완료)

세션은 두 계층에서 C-ABI 로 노출된다. 둘 다 **additive** — 기존 심볼
(`mgroup3_parser_*`, `mgroup3_gen_parse_ast`) 무변경. 결과는 각 계층의 one-shot
파스가 내는 것과 **동일 포맷**이고, 세션 결과는 전체 재파스와 byte-identical
(게이트 §3.1: `session_diff.rs` 퍼징 + `ffi_smoke.rs` 세션 대조).

**mgroup3-native (`ffi.rs`) — `Mgroup3ParseResult` proto 방출** (kernels_history +
accept, `mgroup3_parser_parse` 와 동일 인코딩):

```
mgroup3_session_new(parser: *mut Mgroup3Parser, err: *mut i32) -> *mut ParseSession
mgroup3_session_parse_full(session, input_bytes, input_len, out_ptr, out_len) -> i32
mgroup3_session_edit(session, pos_char, old_len_char, new_bytes, new_len,
                     out_ptr, out_len) -> i32
mgroup3_session_destroy(session)
```

**생성 crate (`Stage4RustEmit` 의 `ffi.rs` 임베드 템플릿) — ast.proto `ParseResult`
방출** (kernels_history → 생성 AST walk → encode, `mgroup3_gen_parse_ast` 의 세션판):

```
mgroup3_gen_session_new(parser, err) -> *mut ParseSession
mgroup3_gen_session_parse_full(session, input_bytes, input_len, out_ptr, out_len) -> i32
mgroup3_gen_session_edit(session, pos_char, old_len_char, new_bytes, new_len,
                         out_ptr, out_len) -> i32
mgroup3_gen_session_destroy(session)
```

계약:
- **핸들 수명**: 세션은 파서 핸들을 **빌린다** (소유 안 함). 파서 핸들 하나가
  **여러 문서 세션**을 뒷받침한다 (LSP 는 열린 문서 전체가 파서 1개 공유 —
  `NativeMulangParser` 가 핸들 1개 보유). 파서는 모든 세션보다 오래 살아야 한다:
  `*_session_destroy` 를 전부 부른 뒤 `mgroup3_parser_free`. (native 쪽은
  `SessionParser::Borrowed(*const)` 로, 파스 중에만 역참조 — 세션은 파생 참조를
  보관하지 않는다.)
- **스레딩**: 세션 = 문서당 1개. 같은 세션 핸들 동시 호출 금지. 서로 다른 세션은
  독립 (각자 문서·체크포인트 링) 이라 문서별 세션을 다른 스레드가 소유해도 안전
  — 공유 파서를 **읽기만** 한다 (설계 §1.4).
- **오프셋 단위**: `pos_char`/`old_len_char` 는 **코드포인트 오프셋** (UTF-16 아님,
  바이트 아님). UTF-16→코드포인트 변환은 호스트 책임 (아래 §4 R4).
- **에러 후 안전**: 파스 실패 편집 후에도 세션은 다음 edit 에 안전 (I2 리셋 규약 —
  실패 시 baseline 폐기, 다음 edit 은 scratch 재파스). `ffi_smoke` 가 parse-break →
  recover 시퀀스로 검증.

## 4. mulang LSP 통합 노트 (배선 지점 — mulang 코드 미수정)

*(조사 기반. mulang 소스 수정은 이 트랙 범위 밖. 배선 지점·오프셋 계약만.
상세 설계는 `incremental_parsing_design.md` §4.2.)*

**현행 (증분 없음):** LSP (lsp4j) 는 `TextDocumentSyncKind.Full` — `didChange` 가
매번 전체 교체 텍스트를 준다 (`MulangTextDocumentService.didChange` →
`lastChange.text` → `onDocumentChanged(uri, text)`). 200ms 디바운스 후
`Bibix4Workspace.load` 가 전체 재파스. 유일한 절약은 `MulangAstCache`
(SHA-256(sourceId+text) 전문-키 디스크 캐시) — 키스트로크마다 미스. native 파스는
`NativeMulangParser.parseToProtoBytes` → `GeneratedAstNativeBridge.parseAst`
(= `mgroup3_gen_parse_ast`, **ast.proto `ParseResult`**) 로, 소비자가 AST proto 를
쓴다 → 세션도 AST proto 를 내는 `mgroup3_gen_session_*` 이 맞다 (§3).

**배선 지점 (JVM seam):**
1. **`GeneratedAstNativeBridge`** — `mgroup3_gen_session_new/parse_full/edit/destroy`
   4심볼을 기존 `mgroup3_gen_parse_ast` 옆에 FFM downcall 로 바인딩. 시그니처는 §3
   (핸들 = `ADDRESS`, `pos_char`/`old_len_char`/`new_len` = `JAVA_LONG`,
   `parse_full`/`edit` 는 `mgroup3_gen_parse_ast` 와 동형 out-param 규약).
2. **`NativeMulangParser`** — 현재 stateless `parse(text)` 만 노출. **문서(sourceId)당
   세션**을 들 자연스러운 자리 (파서 핸들 1개 위에 세션 N개). `parseToProtoBytes`
   옆에 `openSession(sourceId)`/`editSession(sourceId, pos, oldLen, newText)` 추가.
3. **call-site seam** — `Bibix4Workspace.load` 의 `parseAst: (sourceId, text) ->
   CompileUnit` 클로저 (Bibix4Workspace.kt:93). 모든 파일 파스가 지나는 단일
   간접점. 증분 경로는 전문 캐시 조회 대신 sourceId 당 세션을 유지하고 편집을 먹임.

**편집 공급 (Full 동기화 하에서):** 세션은 char-오프셋 `edit(pos, oldLen, newText)`
하나만 먹는다. mulang 은 구 텍스트를 이미 보유 (`openDocuments`/`DocumentState`)
하므로, `onDocumentChanged` 에서 (구 full-text, 신 full-text) 를 **diff 해 단일
치환 편집으로 정규화** (공통 prefix/suffix 제거 → (pos, oldLen, newText)) 하는 게
mulang 변경 최소. (대안: `TextDocumentSyncKind.Incremental` 로 바꿔 range 직접 수신 —
더 정확하나 프로토콜 변경.) 어느 쪽이든 FFI 계약은 코드포인트-오프셋 edit 하나로 고정.

**오프셋 변환 (R4 — 핵심 주의):** 세 단위가 있다. (i) LSP Position = **UTF-16
코드유닛**, (ii) 현행 JVM offset (`SourceIndex.positionToOffset`) = Kotlin String
인덱스 = **UTF-16 코드유닛**, (iii) **세션 FFI = 코드포인트 오프셋** (Rust 파서 gen
단위, `core.rs:1057` `text.chars()`). 현행 스택은 (i)=(ii)=(iii) 을 **암묵 동일시** —
BMP 전용 빌드파일엔 정확, astral-plane 문자엔 틀림. 세션 도입은 이 identity 를
**드러나게** 한다: 편집을 세션에 먹일 때 UTF-16 인덱스 → 코드포인트 오프셋 변환을
FFI 경계에서 넣어야 한다 (호스트 책임). mulang 이 이 변환을 명시화할 지점 = 위
배선 2번 (`NativeMulangParser` 의 세션 edit 진입부).

**캐시 관계:** 세션은 편집당 상태를 들고 있으므로 전문-키 `MulangAstCache` 를
대체·보완한다 (같은 문서의 연속 편집엔 세션이 캐시보다 강함 — 캐시는 콜드 스타트/
크로스-세션 재사용용). 세션 도입 시 캐시는 콜드 파스에만 걸고, 웜 편집은 세션 경로로.

**배포 절차:** jparser ref 갱신 (`bibix.deps`) → mulang `parser.generate` (새
parserdata + `mulang-parser` cdylib 리소스 재생성 — 세션 심볼이 cdylib 에 함께
export 됨) → bundle. 생성 crate 의 `ffi` feature 빌드가 세션 심볼을 자동 포함
(`Stage4RustEmit` 템플릿) 하므로 별도 빌드 플래그 불요.

## 5. 참고

- 프로브 재현: `incremental_probe <mulang pb> [--json] [--edits-per-file N]`.
- 관련: rooted_watcher_gc.md (상태 구조·step6 의미론), PR #6 (eager EOF fold —
  suffix 재사용과의 상호작용은 설계 문서에서).
- FFI 세션 스모크: `mgroup3-native/tests/ffi_smoke.rs`
  (`ffi_smoke_session_lifecycle` — new→parse_full→edit×5→destroy, 결과 동일성 +
  무누수 + 파서 공유; `ffi_smoke_session_bad_utf8_is_safe`).
