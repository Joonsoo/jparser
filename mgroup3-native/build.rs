use std::io::Result;

fn main() -> Result<()> {
    prost_build::compile_protos(
        &[
            "../mgroup3/schema/proto/Mgroup3ParserData.proto",
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
