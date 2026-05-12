use std::fmt;

use super::AcceptCondition;

impl fmt::Display for AcceptCondition {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            AcceptCondition::Always => f.write_str("Always"),
            AcceptCondition::Never => f.write_str("Never"),
            AcceptCondition::NoLongerMatch { symbol_id, start_gen, from_next_gen } => write!(
                f,
                "NoLongerMatch(symbolId={}, startGen={}, fromNextGen={})",
                symbol_id, start_gen, from_next_gen
            ),
            AcceptCondition::NeedLongerMatch { symbol_id, start_gen, from_next_gen } => write!(
                f,
                "NeedLongerMatch(symbolId={}, startGen={}, fromNextGen={})",
                symbol_id, start_gen, from_next_gen
            ),
            AcceptCondition::NotExists { symbol_id, start_gen } => {
                write!(f, "NotExists(symbolId={}, startGen={})", symbol_id, start_gen)
            }
            AcceptCondition::Exists { symbol_id, start_gen } => {
                write!(f, "Exists(symbolId={}, startGen={})", symbol_id, start_gen)
            }
            AcceptCondition::Unless { symbol_id, start_gen } => {
                write!(f, "Unless(symbolId={}, startGen={})", symbol_id, start_gen)
            }
            AcceptCondition::OnlyIf { symbol_id, start_gen } => {
                write!(f, "OnlyIf(symbolId={}, startGen={})", symbol_id, start_gen)
            }
            AcceptCondition::And { items } => write_composite(f, "And", items),
            AcceptCondition::Or { items } => write_composite(f, "Or", items),
        }
    }
}

fn write_composite(
    f: &mut fmt::Formatter<'_>,
    name: &str,
    items: &[AcceptCondition],
) -> fmt::Result {
    write!(f, "{}(", name)?;
    for (i, c) in items.iter().enumerate() {
        if i > 0 {
            f.write_str(", ")?;
        }
        write!(f, "{}", c)?;
    }
    f.write_str(")")
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn display_always_never() {
        assert_eq!(AcceptCondition::Always.to_string(), "Always");
        assert_eq!(AcceptCondition::Never.to_string(), "Never");
    }

    #[test]
    fn display_no_longer_match() {
        let c = AcceptCondition::NoLongerMatch { symbol_id: 3, start_gen: 7, from_next_gen: false };
        assert_eq!(c.to_string(), "NoLongerMatch(symbolId=3, startGen=7, fromNextGen=false)");
        let c = AcceptCondition::NoLongerMatch { symbol_id: 1, start_gen: 2, from_next_gen: true };
        assert_eq!(c.to_string(), "NoLongerMatch(symbolId=1, startGen=2, fromNextGen=true)");
    }

    #[test]
    fn display_need_longer_match() {
        let c = AcceptCondition::NeedLongerMatch { symbol_id: 0, start_gen: 0, from_next_gen: false };
        assert_eq!(c.to_string(), "NeedLongerMatch(symbolId=0, startGen=0, fromNextGen=false)");
    }

    #[test]
    fn display_exists_family() {
        let cases = [
            (AcceptCondition::NotExists { symbol_id: 3, start_gen: 5 }, "NotExists(symbolId=3, startGen=5)"),
            (AcceptCondition::Exists { symbol_id: 3, start_gen: 5 }, "Exists(symbolId=3, startGen=5)"),
            (AcceptCondition::Unless { symbol_id: 3, start_gen: 5 }, "Unless(symbolId=3, startGen=5)"),
            (AcceptCondition::OnlyIf { symbol_id: 3, start_gen: 5 }, "OnlyIf(symbolId=3, startGen=5)"),
        ];
        for (c, expected) in cases {
            assert_eq!(c.to_string(), expected);
        }
    }

    #[test]
    fn display_negative_and_extremes() {
        let c = AcceptCondition::NoLongerMatch {
            symbol_id: -1,
            start_gen: i32::MAX,
            from_next_gen: false,
        };
        assert_eq!(
            c.to_string(),
            "NoLongerMatch(symbolId=-1, startGen=2147483647, fromNextGen=false)"
        );
    }

    #[test]
    fn display_and_two() {
        let c = AcceptCondition::And {
            items: vec![
                AcceptCondition::Always,
                AcceptCondition::Never,
            ],
        };
        assert_eq!(c.to_string(), "And(Always, Never)");
    }

    #[test]
    fn display_or_three() {
        let c = AcceptCondition::Or {
            items: vec![
                AcceptCondition::Always,
                AcceptCondition::Exists { symbol_id: 1, start_gen: 2 },
                AcceptCondition::Never,
            ],
        };
        assert_eq!(c.to_string(), "Or(Always, Exists(symbolId=1, startGen=2), Never)");
    }

    #[test]
    fn display_nested() {
        let inner = AcceptCondition::And {
            items: vec![AcceptCondition::Always, AcceptCondition::Never],
        };
        let outer = AcceptCondition::Or {
            items: vec![inner, AcceptCondition::Exists { symbol_id: 0, start_gen: 0 }],
        };
        assert_eq!(outer.to_string(), "Or(And(Always, Never), Exists(symbolId=0, startGen=0))");
    }
}
