// Hand-written Rust analog of `examples/generated/kotlin/.../PyObjOptAst.kt`.
//
// Companion to `asdl_ast.rs`. This grammar exercises:
//   - nested sealed interfaces (`Value` with one variant — `ListValue` —
//     itself wrapping a `Vec<Value>`).
//   - an explicit `enum` (`BoolEnum { False, True }`).
//   - inputs-as-char access via `Inputs.Character`, which becomes a `chars()`
//     index in Rust. Storing `source_chars: Vec<char>` keeps lookups O(1)
//     regardless of UTF-8 boundary placement.
//   - a peculiar test of the generator's expression handling — `matchIntLiteral`
//     materializes the same `unroll_repeat0` twice and uses an if-empty branch
//     on the second copy to choose between literal strings. The Rust output
//     mirrors that shape literally so equivalence is obvious.
//
// References:
//   - kotlin original: examples/generated/kotlin/.../PyObjOptAst.kt
//   - shared helpers:  examples/generated/rust/ktlib_ast/mod.rs

use super::ktlib_ast::{
    get_sequence_elems, has_single_true, unroll_repeat0, unroll_repeat1, IdIssuer, KernelSet,
};

// ---- AST types ---------------------------------------------------------------

#[derive(Debug, Clone)]
pub struct BoolValue {
    pub value: BoolEnum,
    pub symbol_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct IntLiteral {
    pub value: String,
    pub symbol_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct IntValue {
    pub value: IntLiteral,
    pub symbol_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct ListValue {
    pub elems: Option<Vec<Value>>,
    pub symbol_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct ObjField {
    pub name: StrLiteral,
    pub value: Value,
    pub symbol_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct PyObj {
    pub fields: Option<Vec<ObjField>>,
    pub symbol_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct StrLiteral {
    pub value: String,
    pub symbol_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct StrValue {
    pub value: StrLiteral,
    pub symbol_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct TupleValue {
    pub elems: Vec<Value>,
    pub symbol_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub enum Value {
    BoolValue(Box<BoolValue>),
    IntValue(Box<IntValue>),
    StrValue(Box<StrValue>),
    ListValue(Box<ListValue>),
    TupleValue(Box<TupleValue>),
    // `ObjField` is `: Value, AstNode` in the original. We mirror that here so
    // any caller treating `Value` as the sum type still sees it.
    ObjField(Box<ObjField>),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BoolEnum {
    False,
    True,
}

// ---- Walk context ------------------------------------------------------------

pub struct PyObjKtOptAst<'a> {
    /// Pre-materialized char view of the input. Indices in the parse history
    /// are character indices, not byte indices.
    pub source_chars: &'a [char],
    pub history: &'a [KernelSet],
    pub ids: IdIssuer,
}

impl<'a> PyObjKtOptAst<'a> {
    pub fn new(source_chars: &'a [char], history: &'a [KernelSet]) -> Self {
        Self {
            source_chars,
            history,
            ids: IdIssuer::new(0),
        }
    }

    fn next_id(&mut self) -> i32 {
        self.ids.next_id()
    }

    fn char_at(&self, gen: i32) -> char {
        self.source_chars[gen as usize]
    }

    pub fn match_start(&mut self) -> PyObj {
        let last_gen = self.source_chars.len() as i32;
        let kernel = self.history[last_gen as usize].get_single(1, 1, 0, last_gen);
        self.match_py_obj(kernel.begin_gen, kernel.end_gen)
    }

    fn match_py_obj(&mut self, begin_gen: i32, end_gen: i32) -> PyObj {
        let var1 = get_sequence_elems(self.history, 3, &[4, 5, 11, 88], begin_gen, end_gen);
        let var2 = self.history[var1[2].1 as usize].find_by_begin_gen_opt(12, 1, var1[2].0);
        let var3 = self.history[var1[2].1 as usize].find_by_begin_gen_opt(73, 1, var1[2].0);
        assert!(has_single_true(&[var2.is_some(), var3.is_some()]));
        let fields = if var2.is_some() {
            let var5 = get_sequence_elems(
                self.history,
                14,
                &[15, 83, 69, 5],
                var1[2].0,
                var1[2].1,
            );
            let head = self.match_obj_field(var5[0].0, var5[0].1);
            let tail: Vec<ObjField> =
                unroll_repeat0(self.history, 83, 85, 8, 84, var5[1].0, var5[1].1)
                    .into_iter()
                    .map(|k| {
                        let inner = get_sequence_elems(
                            self.history,
                            87,
                            &[5, 68, 5, 15],
                            k.0,
                            k.1,
                        );
                        self.match_obj_field(inner[3].0, inner[3].1)
                    })
                    .collect();
            let mut out = Vec::with_capacity(1 + tail.len());
            out.push(head);
            out.extend(tail);
            Some(out)
        } else {
            None
        };
        let id = self.next_id();
        PyObj {
            fields,
            symbol_id: id,
            start: begin_gen,
            end: end_gen,
        }
    }

    fn match_obj_field(&mut self, begin_gen: i32, end_gen: i32) -> ObjField {
        let var11 = get_sequence_elems(self.history, 16, &[17, 5, 27, 5, 28], begin_gen, end_gen);
        let name = self.match_str_literal(var11[0].0, var11[0].1);
        let value = self.match_value(var11[4].0, var11[4].1);
        let id = self.next_id();
        ObjField {
            name,
            value,
            symbol_id: id,
            start: begin_gen,
            end: end_gen,
        }
    }

    fn match_str_literal(&mut self, begin_gen: i32, end_gen: i32) -> StrLiteral {
        let var15 = self.history[end_gen as usize].find_by_begin_gen_opt(18, 3, begin_gen);
        let var16 = self.history[end_gen as usize].find_by_begin_gen_opt(25, 3, begin_gen);
        assert!(has_single_true(&[var15.is_some(), var16.is_some()]));
        let (chars, sym): (Vec<char>, i32) = if var15.is_some() {
            let inner = get_sequence_elems(self.history, 18, &[19, 20, 19], begin_gen, end_gen);
            let chars =
                unroll_repeat0(self.history, 20, 22, 8, 21, inner[1].0, inner[1].1)
                    .into_iter()
                    .map(|k| self.match_str_char(k.0, k.1))
                    .collect();
            (chars, self.next_id())
        } else {
            let inner = get_sequence_elems(self.history, 25, &[26, 20, 26], begin_gen, end_gen);
            let chars =
                unroll_repeat0(self.history, 20, 22, 8, 21, inner[1].0, inner[1].1)
                    .into_iter()
                    .map(|k| self.match_str_char(k.0, k.1))
                    .collect();
            (chars, self.next_id())
        };
        StrLiteral {
            value: chars.into_iter().collect(),
            symbol_id: sym,
            start: begin_gen,
            end: end_gen,
        }
    }

    fn match_value(&mut self, begin_gen: i32, end_gen: i32) -> Value {
        let var26 = self.history[end_gen as usize].find_by_begin_gen_opt(29, 1, begin_gen);
        let var27 = self.history[end_gen as usize].find_by_begin_gen_opt(45, 1, begin_gen);
        let var28 = self.history[end_gen as usize].find_by_begin_gen_opt(54, 1, begin_gen);
        let var29 = self.history[end_gen as usize].find_by_begin_gen_opt(55, 1, begin_gen);
        let var30 = self.history[end_gen as usize].find_by_begin_gen_opt(75, 1, begin_gen);
        assert!(has_single_true(&[
            var26.is_some(),
            var27.is_some(),
            var28.is_some(),
            var29.is_some(),
            var30.is_some(),
        ]));
        if var26.is_some() {
            let var32 = get_sequence_elems(self.history, 29, &[30], begin_gen, end_gen);
            let inner = self.match_bool_value(var32[0].0, var32[0].1);
            let id = self.next_id();
            Value::BoolValue(Box::new(BoolValue {
                value: inner,
                symbol_id: id,
                start: begin_gen,
                end: end_gen,
            }))
        } else if var27.is_some() {
            let var35 = get_sequence_elems(self.history, 45, &[46], begin_gen, end_gen);
            let inner = self.match_int_literal(var35[0].0, var35[0].1);
            let id = self.next_id();
            Value::IntValue(Box::new(IntValue {
                value: inner,
                symbol_id: id,
                start: begin_gen,
                end: end_gen,
            }))
        } else if var28.is_some() {
            let var38 = get_sequence_elems(self.history, 54, &[17], begin_gen, end_gen);
            let inner = self.match_str_literal(var38[0].0, var38[0].1);
            let id = self.next_id();
            Value::StrValue(Box::new(StrValue {
                value: inner,
                symbol_id: id,
                start: begin_gen,
                end: end_gen,
            }))
        } else if var29.is_some() {
            let var41 = get_sequence_elems(self.history, 55, &[56], begin_gen, end_gen);
            Value::ListValue(Box::new(self.match_list_value(var41[0].0, var41[0].1)))
        } else {
            let var43 = get_sequence_elems(self.history, 75, &[76], begin_gen, end_gen);
            Value::TupleValue(Box::new(self.match_tuple_value(var43[0].0, var43[0].1)))
        }
    }

    fn match_int_literal(&mut self, begin_gen: i32, end_gen: i32) -> IntLiteral {
        let var45 = self.history[end_gen as usize].find_by_begin_gen_opt(47, 1, begin_gen);
        let var46 = self.history[end_gen as usize].find_by_begin_gen_opt(49, 2, begin_gen);
        assert!(has_single_true(&[var45.is_some(), var46.is_some()]));
        if var45.is_some() {
            let id = self.next_id();
            IntLiteral {
                value: "0".to_string(),
                symbol_id: id,
                start: begin_gen,
                end: end_gen,
            }
        } else {
            let var49 = get_sequence_elems(self.history, 49, &[50, 51], begin_gen, end_gen);
            let var50: Vec<char> =
                unroll_repeat0(self.history, 51, 53, 8, 52, var49[1].0, var49[1].1)
                    .into_iter()
                    .map(|k| self.char_at(k.0))
                    .collect();
            // Kotlin original materializes `var51` as a second identical
            // unroll_repeat0 call and uses its emptiness to pick a literal
            // string. We mirror the redundancy so a future cleanup of the
            // Kotlin codegen (collapsing duplicate exprs) can be the trigger
            // for cleaning this up on the Rust side too.
            let var51: Vec<char> =
                unroll_repeat0(self.history, 51, 53, 8, 52, var49[1].0, var49[1].1)
                    .into_iter()
                    .map(|k| self.char_at(k.0))
                    .collect();
            let var52: &str = if var51.is_empty() { "hello" } else { "world" };
            let mut value = String::new();
            value.push(self.char_at(var49[0].0));
            value.extend(var50.into_iter());
            value.push_str(var52);
            let id = self.next_id();
            IntLiteral {
                value,
                symbol_id: id,
                start: begin_gen,
                end: end_gen,
            }
        }
    }

    fn match_list_value(&mut self, begin_gen: i32, end_gen: i32) -> ListValue {
        let var55 = get_sequence_elems(self.history, 57, &[58, 5, 59, 74], begin_gen, end_gen);
        let var56 = self.history[var55[2].1 as usize].find_by_begin_gen_opt(60, 1, var55[2].0);
        let var57 = self.history[var55[2].1 as usize].find_by_begin_gen_opt(73, 1, var55[2].0);
        assert!(has_single_true(&[var56.is_some(), var57.is_some()]));
        let elems_opt = if var56.is_some() {
            let inner = get_sequence_elems(
                self.history,
                62,
                &[28, 63, 69, 5],
                var55[2].0,
                var55[2].1,
            );
            let head = self.match_value(inner[0].0, inner[0].1);
            let tail: Vec<Value> = unroll_repeat0(self.history, 63, 65, 8, 64, inner[1].0, inner[1].1)
                .into_iter()
                .map(|k| {
                    let pair = get_sequence_elems(
                        self.history,
                        67,
                        &[5, 68, 5, 28],
                        k.0,
                        k.1,
                    );
                    self.match_value(pair[3].0, pair[3].1)
                })
                .collect();
            let mut out = Vec::with_capacity(1 + tail.len());
            out.push(head);
            out.extend(tail);
            Some(out)
        } else {
            None
        };
        // Kotlin: `ListValue(var54 ?: listOf(), ...)`. Defaults to empty Vec.
        let elems = elems_opt.or(Some(Vec::new()));
        let id = self.next_id();
        ListValue {
            elems,
            symbol_id: id,
            start: begin_gen,
            end: end_gen,
        }
    }

    fn match_tuple_value(&mut self, begin_gen: i32, end_gen: i32) -> TupleValue {
        let var65 = self.history[end_gen as usize].find_by_begin_gen_opt(77, 7, begin_gen);
        let var66 = self.history[end_gen as usize].find_by_begin_gen_opt(80, 7, begin_gen);
        assert!(has_single_true(&[var65.is_some(), var66.is_some()]));
        if var65.is_some() {
            let var68 = get_sequence_elems(
                self.history,
                77,
                &[78, 5, 28, 5, 68, 5, 79],
                begin_gen,
                end_gen,
            );
            let only = self.match_value(var68[2].0, var68[2].1);
            let id = self.next_id();
            TupleValue {
                elems: vec![only],
                symbol_id: id,
                start: begin_gen,
                end: end_gen,
            }
        } else {
            let var71 = get_sequence_elems(
                self.history,
                80,
                &[78, 5, 28, 81, 69, 5, 79],
                begin_gen,
                end_gen,
            );
            let head = self.match_value(var71[2].0, var71[2].1);
            let tail: Vec<Value> = unroll_repeat1(self.history, 81, 65, 65, 82, var71[3].0, var71[3].1)
                .into_iter()
                .map(|k| {
                    let pair = get_sequence_elems(
                        self.history,
                        67,
                        &[5, 68, 5, 28],
                        k.0,
                        k.1,
                    );
                    self.match_value(pair[3].0, pair[3].1)
                })
                .collect();
            let mut elems = Vec::with_capacity(1 + tail.len());
            elems.push(head);
            elems.extend(tail);
            let id = self.next_id();
            TupleValue {
                elems,
                symbol_id: id,
                start: begin_gen,
                end: end_gen,
            }
        }
    }

    fn match_bool_value(&mut self, begin_gen: i32, end_gen: i32) -> BoolEnum {
        let var77 = self.history[end_gen as usize].find_by_begin_gen_opt(31, 1, begin_gen);
        let var78 = self.history[end_gen as usize].find_by_begin_gen_opt(38, 1, begin_gen);
        assert!(has_single_true(&[var77.is_some(), var78.is_some()]));
        if var77.is_some() {
            BoolEnum::True
        } else {
            BoolEnum::False
        }
    }

    fn match_str_char(&mut self, begin_gen: i32, _end_gen: i32) -> char {
        let var80 = get_sequence_elems(self.history, 23, &[24], begin_gen, _end_gen);
        self.char_at(var80[0].0)
    }
}
