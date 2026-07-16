// Global allocator override (feature-gated). The parse hot path is dominated by
// small, short-lived allocations (AcceptCondition `and_from` composites,
// MilestonePath `Rc`, evolve's small `Vec`s) that malloc/free was showing at
// ~17% self-time under the system allocator. mimalloc's segment/free-list
// design handles this class markedly better on macOS. Selecting an allocator is
// semantically invisible — output is byte-identical (guarded by hist_ab
// COMBINED_FP). Only one `#[global_allocator]` may exist in a dependency graph;
// the two features are mutually exclusive by construction, and the generated
// FFI crate that links this one defines no allocator of its own, so the cdylib
// inherits this one without conflict.
#[cfg(feature = "mimalloc")]
#[global_allocator]
static GLOBAL: mimalloc::MiMalloc = mimalloc::MiMalloc;

#[cfg(all(feature = "jemalloc", not(feature = "mimalloc")))]
#[global_allocator]
static GLOBAL: tikv_jemallocator::Jemalloc = tikv_jemallocator::Jemalloc;

pub mod accept_condition;
pub mod ffi;
pub mod fingerprint;
pub mod history;
pub mod parser;
pub mod parser_cache;
pub mod parser_data;
pub mod parsing_ctx;
pub mod path_root;
pub mod rebase;
pub mod session;
pub mod term_group;

pub mod proto {
    pub mod com {
        pub mod giyeok {
            pub mod jparser {
                pub mod proto {
                    include!(concat!(env!("OUT_DIR"), "/com.giyeok.jparser.proto.rs"));
                }
                pub mod mgroup3 {
                    pub mod proto {
                        include!(concat!(env!("OUT_DIR"), "/com.giyeok.jparser.mgroup3.proto.rs"));
                    }
                }
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::proto::com::giyeok::jparser::mgroup3::proto::Mgroup3ParserData;
    use prost::Message;

    #[test]
    fn default_roundtrip() {
        let data = Mgroup3ParserData::default();
        let mut buf = Vec::new();
        data.encode(&mut buf).expect("encode");
        let decoded = Mgroup3ParserData::decode(buf.as_slice()).expect("decode");
        assert_eq!(decoded.start_symbol_id, 0);
        assert_eq!(decoded.path_roots.len(), 0);
    }
}
