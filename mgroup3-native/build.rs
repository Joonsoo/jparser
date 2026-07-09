use std::io::Result;

fn main() -> Result<()> {
    // rkyv 캐시(parser_cache.rs)는 ParserDataPlain 을 zero-copy archive 로 굽는다.
    // ParserDataPlain 이 그대로 임베드하는 prost 생성 타입들(AcceptConditionTemplate
    // 계열, KernelTemplate, *KernelTemplate, TermGroup 계열)에 rkyv derive 를
    // 붙여야 한다 — 필드 타입은 바꾸지 않으므로 파싱 코드(template.rs/core.rs)에는
    // 무영향. 도달 가능한 타입만 정확히 스코프해서 나머지 생성 타입은 건드리지 않는다.
    //
    // 스키마가 바뀌어 여기 타입 집합이 달라지면 parser_cache::PLAIN_SCHEMA_VERSION
    // 을 bump 할 것 (캐시 헤더 규약).
    const RKYV_DERIVE: &str =
        "#[derive(::rkyv::Archive, ::rkyv::Serialize, ::rkyv::Deserialize)]";

    // ParserDataPlain 에서 도달 가능한 prost 메시지 / oneof 전부.
    let rkyv_types = [
        // com.giyeok.jparser.mgroup3.proto — accept condition 템플릿 계열
        "com.giyeok.jparser.mgroup3.proto.AcceptConditionTemplate",
        "com.giyeok.jparser.mgroup3.proto.AcceptConditionTemplate.condition", // oneof enum
        "com.giyeok.jparser.mgroup3.proto.MultiAcceptConditions",
        "com.giyeok.jparser.mgroup3.proto.NoLongerMatchTemplate",
        "com.giyeok.jparser.mgroup3.proto.LookaheadFoundTemplate",
        "com.giyeok.jparser.mgroup3.proto.LookaheadNotFoundTemplate",
        "com.giyeok.jparser.mgroup3.proto.ExceptTemplate",
        "com.giyeok.jparser.mgroup3.proto.JoinTemplate",
        // kernel 템플릿 계열
        "com.giyeok.jparser.mgroup3.proto.KernelTemplate",
        "com.giyeok.jparser.mgroup3.proto.ProgressedKernelTemplate",
        "com.giyeok.jparser.mgroup3.proto.FinishedKernelTemplate",
        "com.giyeok.jparser.mgroup3.proto.AddedKernelTemplate",
        // com.giyeok.jparser.proto — TermGroup 계열
        "com.giyeok.jparser.proto.TermGroup",
        "com.giyeok.jparser.proto.TermGroup.TermGroup", // oneof enum (proto oneof name is `TermGroup`)
        "com.giyeok.jparser.proto.AllCharsExcluding",
        "com.giyeok.jparser.proto.CharsGroup",
        "com.giyeok.jparser.proto.VirtualsGroup",
    ];

    let mut config = prost_build::Config::new();
    for ty in rkyv_types {
        config.type_attribute(ty, RKYV_DERIVE);
    }

    // AcceptConditionTemplate 는 재귀적이다:
    //   AcceptConditionTemplate → condition(oneof) → And/Or(MultiAcceptConditions)
    //     → MultiAcceptConditions.conditions: Vec<AcceptConditionTemplate>
    // rkyv derive 가 만드는 where-절이 이 사이클에서 무한 재귀(E0275)한다. 재귀 엣지인
    // MultiAcceptConditions.conditions 에 omit_bounds 를 달아 derive 가 그 필드로부터
    // 자동 bound 를 만들지 않게 하고, 대신 container 에 필요한 bound 를 수동 지정한다
    // (rkyv 재귀 타입 표준 패턴 — rkyv/src/impls/mod.rs 의 Node/LinkedList 예시).
    config.field_attribute(
        "com.giyeok.jparser.mgroup3.proto.MultiAcceptConditions.conditions",
        "#[rkyv(omit_bounds)]",
    );
    // NOTE: bound 경로에 leading `::` 를 쓰면 prost-build 가 돌리는 rustfmt 가
    // `: ::` 를 `:::` 로 붕괴시켜 컴파일이 깨진다. edition 2021 에서 bare `rkyv::`
    // 는 crate-root 로 resolve 되므로 leading `::` 없이 쓴다.
    config.type_attribute(
        "com.giyeok.jparser.mgroup3.proto.MultiAcceptConditions",
        "#[rkyv(serialize_bounds(__S: rkyv::ser::Writer + rkyv::ser::Allocator, \
         __S::Error: rkyv::rancor::Source), \
         deserialize_bounds(__D::Error: rkyv::rancor::Source), \
         bytecheck(bounds(__C: rkyv::validation::ArchiveContext, \
         __C::Error: rkyv::rancor::Source)))]",
    );

    config.compile_protos(
        &[
            "../mgroup3/schema/proto/Mgroup3ParserData.proto",
            "../mgroup3/schema/proto/Mgroup3ParserResult.proto",
            "../base/proto/TermGroupProto.proto",
            "../base/proto/GrammarProto.proto",
        ],
        &[
            "../mgroup3/schema/proto",
            "../base/proto",
        ],
    )?;
    Ok(())
}
