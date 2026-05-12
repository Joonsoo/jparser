pub mod accept_condition;
pub mod path_root;

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
