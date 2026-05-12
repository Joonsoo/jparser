//! Recursive-descent parser for the `Display` format of `AcceptCondition` and
//! `PathRoot`. Accepts both Kotlin's auto-generated output and Rust's
//! `Display` (they share format by design).
//!
//! Grammar:
//! ```text
//! cond      := "Always" | "Never"
//!            | "And(" condList ")" | "Or(" condList ")"
//!            | leafName "(" fieldList ")"
//! condList  := cond ("," " " cond)*
//! leafName  := "NoLongerMatch" | "NeedLongerMatch"
//!            | "NotExists" | "Exists" | "Unless" | "OnlyIf"
//! fieldList := field ("," " " field)*
//! field     := ident "=" (signedInt | "true" | "false")
//! ```
//!
//! `PathRoot` follows the same shape: `"PathRoot(symbolId=N, startGen=M)"`.

use crate::path_root::PathRoot;

use super::AcceptCondition;

#[derive(Debug)]
pub struct ParseError {
    pub pos: usize,
    pub msg: String,
}

impl std::fmt::Display for ParseError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "parse error at {}: {}", self.pos, self.msg)
    }
}
impl std::error::Error for ParseError {}

pub fn parse_condition(s: &str) -> Result<AcceptCondition, ParseError> {
    let mut p = Parser::new(s);
    let c = p.parse_cond()?;
    p.skip_ws();
    if !p.at_end() {
        return Err(p.err(format!("trailing input: {:?}", &p.input[p.pos..])));
    }
    Ok(c)
}

pub fn parse_path_root(s: &str) -> Result<PathRoot, ParseError> {
    let mut p = Parser::new(s);
    let r = p.parse_path_root()?;
    p.skip_ws();
    if !p.at_end() {
        return Err(p.err(format!("trailing input: {:?}", &p.input[p.pos..])));
    }
    Ok(r)
}

struct Parser<'a> {
    input: &'a str,
    pos: usize,
}

impl<'a> Parser<'a> {
    fn new(input: &'a str) -> Self {
        Self { input, pos: 0 }
    }

    fn at_end(&self) -> bool {
        self.pos >= self.input.len()
    }

    fn peek(&self) -> Option<char> {
        self.input[self.pos..].chars().next()
    }

    fn skip_ws(&mut self) {
        while let Some(c) = self.peek() {
            if c.is_whitespace() {
                self.pos += c.len_utf8();
            } else {
                break;
            }
        }
    }

    fn consume_str(&mut self, s: &str) -> Result<(), ParseError> {
        if self.input[self.pos..].starts_with(s) {
            self.pos += s.len();
            Ok(())
        } else {
            Err(self.err(format!("expected {:?}", s)))
        }
    }

    fn try_consume_str(&mut self, s: &str) -> bool {
        if self.input[self.pos..].starts_with(s) {
            self.pos += s.len();
            true
        } else {
            false
        }
    }

    fn err(&self, msg: String) -> ParseError {
        ParseError { pos: self.pos, msg }
    }

    fn parse_cond(&mut self) -> Result<AcceptCondition, ParseError> {
        // Order matters: longer prefixes first.
        if self.try_consume_str("Always") {
            return Ok(AcceptCondition::Always);
        }
        if self.try_consume_str("Never") {
            return Ok(AcceptCondition::Never);
        }
        if self.try_consume_str("And(") {
            let items = self.parse_cond_list()?;
            self.consume_str(")")?;
            // Items are already canonical from the source side, but re-canonicalize
            // so equality holds regardless of input order.
            return Ok(AcceptCondition::and_from(items));
        }
        if self.try_consume_str("Or(") {
            let items = self.parse_cond_list()?;
            self.consume_str(")")?;
            return Ok(AcceptCondition::or_from(items));
        }
        // Leaf: longest first ("NoLongerMatch" / "NeedLongerMatch" before "NotExists")
        if self.try_consume_str("NoLongerMatch(") {
            let (sym, gen_v, fng) = self.parse_three_field_leaf()?;
            self.consume_str(")")?;
            return Ok(AcceptCondition::NoLongerMatch {
                symbol_id: sym,
                start_gen: gen_v,
                from_next_gen: fng,
            });
        }
        if self.try_consume_str("NeedLongerMatch(") {
            let (sym, gen_v, fng) = self.parse_three_field_leaf()?;
            self.consume_str(")")?;
            return Ok(AcceptCondition::NeedLongerMatch {
                symbol_id: sym,
                start_gen: gen_v,
                from_next_gen: fng,
            });
        }
        if self.try_consume_str("NotExists(") {
            let (sym, gen_v) = self.parse_two_field_leaf()?;
            self.consume_str(")")?;
            return Ok(AcceptCondition::NotExists { symbol_id: sym, start_gen: gen_v });
        }
        if self.try_consume_str("Exists(") {
            let (sym, gen_v) = self.parse_two_field_leaf()?;
            self.consume_str(")")?;
            return Ok(AcceptCondition::Exists { symbol_id: sym, start_gen: gen_v });
        }
        if self.try_consume_str("Unless(") {
            let (sym, gen_v) = self.parse_two_field_leaf()?;
            self.consume_str(")")?;
            return Ok(AcceptCondition::Unless { symbol_id: sym, start_gen: gen_v });
        }
        if self.try_consume_str("OnlyIf(") {
            let (sym, gen_v) = self.parse_two_field_leaf()?;
            self.consume_str(")")?;
            return Ok(AcceptCondition::OnlyIf { symbol_id: sym, start_gen: gen_v });
        }
        Err(self.err("expected AcceptCondition".to_string()))
    }

    fn parse_cond_list(&mut self) -> Result<Vec<AcceptCondition>, ParseError> {
        let mut out = vec![self.parse_cond()?];
        while self.try_consume_str(", ") {
            out.push(self.parse_cond()?);
        }
        Ok(out)
    }

    fn parse_two_field_leaf(&mut self) -> Result<(i32, i32), ParseError> {
        self.consume_str("symbolId=")?;
        let sym = self.parse_signed_int()?;
        self.consume_str(", startGen=")?;
        let gen_v = self.parse_signed_int()?;
        Ok((sym, gen_v))
    }

    fn parse_three_field_leaf(&mut self) -> Result<(i32, i32, bool), ParseError> {
        let (sym, gen_v) = self.parse_two_field_leaf()?;
        self.consume_str(", fromNextGen=")?;
        let fng = self.parse_bool()?;
        Ok((sym, gen_v, fng))
    }

    fn parse_path_root(&mut self) -> Result<PathRoot, ParseError> {
        self.consume_str("PathRoot(")?;
        let (sym, gen_v) = self.parse_two_field_leaf()?;
        self.consume_str(")")?;
        Ok(PathRoot::new(sym, gen_v))
    }

    fn parse_signed_int(&mut self) -> Result<i32, ParseError> {
        let start = self.pos;
        if self.peek() == Some('-') {
            self.pos += 1;
        }
        let digit_start = self.pos;
        while let Some(c) = self.peek() {
            if c.is_ascii_digit() {
                self.pos += 1;
            } else {
                break;
            }
        }
        if self.pos == digit_start {
            return Err(self.err("expected digit".to_string()));
        }
        let slice = &self.input[start..self.pos];
        slice.parse::<i32>().map_err(|e| self.err(format!("parse_int: {}", e)))
    }

    fn parse_bool(&mut self) -> Result<bool, ParseError> {
        if self.try_consume_str("true") {
            Ok(true)
        } else if self.try_consume_str("false") {
            Ok(false)
        } else {
            Err(self.err("expected bool".to_string()))
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn rt(c: &AcceptCondition) {
        let s = c.to_string();
        let parsed = parse_condition(&s).expect(&format!("parse failed for {}", s));
        assert_eq!(&parsed, c, "roundtrip mismatch for {}", s);
    }

    fn ex(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::Exists { symbol_id: s, start_gen: g }
    }
    fn nex(s: i32, g: i32) -> AcceptCondition {
        AcceptCondition::NotExists { symbol_id: s, start_gen: g }
    }

    #[test]
    fn roundtrip_constants() {
        rt(&AcceptCondition::Always);
        rt(&AcceptCondition::Never);
    }

    #[test]
    fn roundtrip_leaves() {
        rt(&AcceptCondition::NoLongerMatch { symbol_id: 3, start_gen: 7, from_next_gen: false });
        rt(&AcceptCondition::NoLongerMatch { symbol_id: 1, start_gen: 2, from_next_gen: true });
        rt(&AcceptCondition::NeedLongerMatch { symbol_id: 0, start_gen: 0, from_next_gen: false });
        rt(&AcceptCondition::NotExists { symbol_id: 3, start_gen: 5 });
        rt(&ex(3, 5));
        rt(&AcceptCondition::Unless { symbol_id: 3, start_gen: 5 });
        rt(&AcceptCondition::OnlyIf { symbol_id: 3, start_gen: 5 });
    }

    #[test]
    fn roundtrip_negative_and_extreme() {
        rt(&AcceptCondition::NoLongerMatch {
            symbol_id: -1,
            start_gen: i32::MAX,
            from_next_gen: false,
        });
    }

    #[test]
    fn roundtrip_composites() {
        rt(&AcceptCondition::and_from([ex(1, 0), nex(2, 0)]));
        rt(&AcceptCondition::or_from([ex(1, 0), nex(2, 0), ex(3, 0)]));
        rt(&AcceptCondition::and_from((0..5).map(|i| ex(i, 0)))); // size 5
    }

    #[test]
    fn roundtrip_nested() {
        let inner = AcceptCondition::or_from([ex(1, 0), nex(2, 0)]);
        let outer = AcceptCondition::and_from([inner, ex(3, 0)]);
        rt(&outer);
    }

    #[test]
    fn accepts_kotlin_style_strings_directly() {
        // String shaped exactly like Kotlin's data-class toString.
        let s = "NoLongerMatch(symbolId=2, startGen=4, fromNextGen=false)";
        let parsed = parse_condition(s).unwrap();
        assert_eq!(
            parsed,
            AcceptCondition::NoLongerMatch { symbol_id: 2, start_gen: 4, from_next_gen: false }
        );
    }

    #[test]
    fn parse_path_root_basic() {
        assert_eq!(parse_path_root("PathRoot(symbolId=2, startGen=4)").unwrap(), PathRoot::new(2, 4));
        assert_eq!(parse_path_root("PathRoot(symbolId=-1, startGen=0)").unwrap(), PathRoot::new(-1, 0));
    }

    #[test]
    fn rejects_trailing_garbage() {
        assert!(parse_condition("Always XX").is_err());
        assert!(parse_path_root("PathRoot(symbolId=1, startGen=2)x").is_err());
    }

    #[test]
    fn rejects_unknown_leaf() {
        assert!(parse_condition("Banana(symbolId=1, startGen=2)").is_err());
    }
}
