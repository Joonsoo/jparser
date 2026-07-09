use std::fmt;

#[derive(Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord, Debug)]
pub struct PathRoot {
    pub symbol_id: i32,
    pub start_gen: i32,
}

impl PathRoot {
    pub fn new(symbol_id: i32, start_gen: i32) -> Self {
        Self { symbol_id, start_gen }
    }
}

impl fmt::Display for PathRoot {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "PathRoot(symbolId={}, startGen={})", self.symbol_id, self.start_gen)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn display_format() {
        assert_eq!(PathRoot::new(3, 7).to_string(), "PathRoot(symbolId=3, startGen=7)");
        assert_eq!(PathRoot::new(0, 0).to_string(), "PathRoot(symbolId=0, startGen=0)");
        assert_eq!(PathRoot::new(-1, 2_147_483_647).to_string(), "PathRoot(symbolId=-1, startGen=2147483647)");
    }

    #[test]
    fn equality_and_ordering() {
        let a = PathRoot::new(1, 2);
        let b = PathRoot::new(1, 2);
        let c = PathRoot::new(1, 3);
        assert_eq!(a, b);
        assert_ne!(a, c);
        assert!(a < c);
    }

    #[test]
    fn copy_is_value_semantics() {
        let a = PathRoot::new(5, 10);
        let b = a;
        let _ = a;
        assert_eq!(a, b);
    }
}
