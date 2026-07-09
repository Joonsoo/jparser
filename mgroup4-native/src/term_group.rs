//! Term-group matching. Port of `ktlib/main/kotlin/.../TermGroupUtil.kt`.
//!
//! The Kotlin matcher checks `Char.category.value` against
//! `CharsGroup.unicodeCategoriesList` (Java `Character.UNASSIGNED..FINAL_QUOTE_PUNCTUATION`
//! integer codes). We use the `unicode-general-category` crate and map its
//! `GeneralCategory` enum to the matching Java integer.

use unicode_general_category::{get_general_category, GeneralCategory};

use crate::proto::com::giyeok::jparser::proto::term_group::TermGroup as TermGroupOneof;
use crate::proto::com::giyeok::jparser::proto::{AllCharsExcluding, CharsGroup, TermGroup};

/// True iff `input` matches `tg`. Mirrors `TermGroupUtil.isMatch` plus its
/// CharsGroup helper.
pub fn is_match(tg: &TermGroup, input: char) -> bool {
    match tg.term_group.as_ref() {
        Some(TermGroupOneof::AllCharsExcluding(ace)) => is_match_all_excluding(ace, input),
        Some(TermGroupOneof::CharsGroup(cg)) => is_match_chars(cg, input),
        Some(TermGroupOneof::VirtualsGroup(_)) => {
            unreachable!("term_group::is_match called with VirtualsGroup (matches Kotlin TODO)")
        }
        None => {
            unreachable!("term_group::is_match called with no term_group set (matches Kotlin TODO)")
        }
    }
}

fn is_match_all_excluding(ace: &AllCharsExcluding, input: char) -> bool {
    let Some(excluding) = ace.excluding.as_ref() else {
        // Kotlin treats an empty excluding the same as matching everything.
        return true;
    };
    !is_match_chars(excluding, input)
}

fn is_match_chars(cg: &CharsGroup, input: char) -> bool {
    if cg.excluding_chars.chars().any(|c| c == input) {
        return false;
    }
    let cat = java_general_category_value(input);
    if cg.unicode_categories.iter().copied().any(|v| v == cat) {
        return true;
    }
    cg.chars.chars().any(|c| c == input)
}

/// Java `Character.getType(int)` integer codes — the values that
/// `CharsGroup.unicode_categories` contains. Matches JDK constants
/// (UNASSIGNED=0 … FINAL_QUOTE_PUNCTUATION=30). The unicode-general-category
/// crate exposes the same categorization under different names.
pub fn java_general_category_value(input: char) -> i32 {
    match get_general_category(input) {
        GeneralCategory::Unassigned => 0,
        GeneralCategory::UppercaseLetter => 1,
        GeneralCategory::LowercaseLetter => 2,
        GeneralCategory::TitlecaseLetter => 3,
        GeneralCategory::ModifierLetter => 4,
        GeneralCategory::OtherLetter => 5,
        GeneralCategory::NonspacingMark => 6,
        GeneralCategory::EnclosingMark => 7,
        GeneralCategory::SpacingMark => 8, // = COMBINING_SPACING_MARK
        GeneralCategory::DecimalNumber => 9,
        GeneralCategory::LetterNumber => 10,
        GeneralCategory::OtherNumber => 11,
        GeneralCategory::SpaceSeparator => 12,
        GeneralCategory::LineSeparator => 13,
        GeneralCategory::ParagraphSeparator => 14,
        GeneralCategory::Control => 15,
        GeneralCategory::Format => 16,
        // Java skips 17 — no GeneralCategory variant maps to 17.
        GeneralCategory::PrivateUse => 18,
        GeneralCategory::Surrogate => 19,
        GeneralCategory::DashPunctuation => 20,
        GeneralCategory::OpenPunctuation => 21,  // = START_PUNCTUATION
        GeneralCategory::ClosePunctuation => 22, // = END_PUNCTUATION
        GeneralCategory::ConnectorPunctuation => 23,
        GeneralCategory::OtherPunctuation => 24,
        GeneralCategory::MathSymbol => 25,
        GeneralCategory::CurrencySymbol => 26,
        GeneralCategory::ModifierSymbol => 27,
        GeneralCategory::OtherSymbol => 28,
        GeneralCategory::InitialPunctuation => 29, // = INITIAL_QUOTE_PUNCTUATION
        GeneralCategory::FinalPunctuation => 30,   // = FINAL_QUOTE_PUNCTUATION
        // GeneralCategory is marked non_exhaustive. If the crate ever adds a
        // new variant, fall through to UNASSIGNED — the parser will mismatch
        // for that codepoint but won't UB.
        _ => 0,
    }
}

/// Stub `TermSet` — minimum surface needed to compile and run parser tests.
/// The Kotlin `TermSet` is richer; we'll grow this as actual tests demand.
#[derive(Clone, Debug, Default, PartialEq, Eq)]
pub struct TermSet {
    pub term_groups: Vec<TermGroup>,
}

impl TermSet {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn is_empty(&self) -> bool {
        self.term_groups.is_empty()
    }
}

/// Stub builder. Mirrors Kotlin's `TermGroupUtil.TermGroupBuilder` surface; the
/// internal merging logic is deferred (the Kotlin code has `TODO()`s too, e.g.
/// `addUnicodeCategory`). For now, `add` just appends — call sites can dedup.
pub struct TermGroupBuilder {
    groups: Vec<TermGroup>,
}

impl Default for TermGroupBuilder {
    fn default() -> Self {
        Self::new()
    }
}

impl TermGroupBuilder {
    pub fn new() -> Self {
        Self { groups: Vec::new() }
    }

    pub fn add(&mut self, tg: TermGroup) {
        self.groups.push(tg);
    }

    pub fn build(self) -> TermSet {
        TermSet { term_groups: self.groups }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn chars_group(chars: &str, excluding: &str, categories: Vec<i32>) -> TermGroup {
        TermGroup {
            term_group: Some(TermGroupOneof::CharsGroup(CharsGroup {
                unicode_categories: categories,
                excluding_chars: excluding.to_string(),
                chars: chars.to_string(),
            })),
        }
    }

    fn all_excluding(inner: CharsGroup) -> TermGroup {
        TermGroup {
            term_group: Some(TermGroupOneof::AllCharsExcluding(AllCharsExcluding {
                excluding: Some(inner),
            })),
        }
    }

    #[test]
    fn chars_only() {
        let g = chars_group("abc", "", vec![]);
        assert!(is_match(&g, 'a'));
        assert!(is_match(&g, 'b'));
        assert!(!is_match(&g, 'd'));
        assert!(!is_match(&g, '0'));
    }

    #[test]
    fn unicode_category_only_lowercase() {
        // category 2 == LOWERCASE_LETTER
        let g = chars_group("", "", vec![2]);
        assert!(is_match(&g, 'a'));
        assert!(is_match(&g, 'z'));
        assert!(!is_match(&g, 'A'));
        assert!(!is_match(&g, '1'));
    }

    #[test]
    fn excluding_chars_on_top_of_category() {
        // Lowercase letters EXCEPT 'a'.
        let g = chars_group("", "a", vec![2]);
        assert!(!is_match(&g, 'a'));
        assert!(is_match(&g, 'b'));
        assert!(!is_match(&g, 'A'));
    }

    #[test]
    fn all_chars_excluding_is_inverse() {
        // All chars except 'a'/'b'/'c'.
        let inner = CharsGroup {
            unicode_categories: vec![],
            excluding_chars: String::new(),
            chars: "abc".to_string(),
        };
        let g = all_excluding(inner);
        assert!(!is_match(&g, 'a'));
        assert!(!is_match(&g, 'b'));
        assert!(is_match(&g, 'd'));
        assert!(is_match(&g, '1'));
    }

    #[test]
    fn java_category_values_basic() {
        assert_eq!(java_general_category_value('A'), 1); // UPPERCASE_LETTER
        assert_eq!(java_general_category_value('a'), 2); // LOWERCASE_LETTER
        assert_eq!(java_general_category_value('0'), 9); // DECIMAL_DIGIT_NUMBER
        assert_eq!(java_general_category_value(' '), 12); // SPACE_SEPARATOR
        assert_eq!(java_general_category_value('!'), 24); // OTHER_PUNCTUATION
        assert_eq!(java_general_category_value('('), 21); // START_PUNCTUATION
        assert_eq!(java_general_category_value(')'), 22); // END_PUNCTUATION
        assert_eq!(java_general_category_value('-'), 20); // DASH_PUNCTUATION
        assert_eq!(java_general_category_value('$'), 26); // CURRENCY_SYMBOL
    }
}
