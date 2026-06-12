// Hand-written Rust analog of `examples/generated/kotlin/.../AsdlAst.kt`.
//
// This file is a *target model* for Stage4RustEmit's per-grammar code emitter:
// the goal is to show what shape of code the generator should produce so the
// Kotlin-side `KotlinOptCodeGen` walk translates faithfully into Rust.
//
// Conventions:
//   - One concrete `data class` (Kotlin) → one `pub struct`.
//   - Sealed interface (Kotlin) → `pub enum`. Variants Box their inner struct
//     because the sealed children participate in self-referential cycles
//     elsewhere in the AST. (See README design principle #4 in mgroup3-native.)
//   - Optional field → `Option<T>`. List → `Vec<T>`.
//   - `nextId()` is threaded through a `&mut IdIssuer` so the AST walk is a
//     mutable borrow rather than an interior-mutable cell (closer to Rust style;
//     equivalent semantics).
//   - Each `matchX` function takes `&mut Ctx` so it can borrow history+source+
//     ids without re-passing all three.
//
// References:
//   - kotlin original: examples/generated/kotlin/.../AsdlAst.kt
//   - shared helpers:  examples/generated/rust/ktlib_ast/mod.rs
//   - ktlib upstream:  ktlib/main/kotlin/com/giyeok/jparser/ktlib/AstifierUtil.kt

use super::ktlib_ast::{
    get_sequence_elems, has_single_true, unroll_repeat0, unroll_repeat1, IdIssuer, KernelSet,
};

// ---- AST types ---------------------------------------------------------------

#[derive(Debug, Clone)]
pub struct ModuleDef {
    pub name: String,
    pub defs: Vec<SuperClassDef>,
    pub node_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct Attributes {
    pub attrs: Vec<Param>,
    pub node_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct Param {
    pub type_name: String,
    pub type_attr: TypeAttr,
    pub name: String,
    pub node_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct SubClassDef {
    pub name: String,
    pub params: Option<Vec<Param>>,
    pub node_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct SealedClassDefs {
    pub subs: Vec<SubClassDef>,
    pub node_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct SuperClassDef {
    pub name: String,
    pub body: SuperClassDefBody,
    pub attrs: Option<Attributes>,
    pub node_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub struct TupleDef {
    pub body: Vec<Param>,
    pub node_id: i32,
    pub start: i32,
    pub end: i32,
}

#[derive(Debug, Clone)]
pub enum SuperClassDefBody {
    SealedClassDefs(Box<SealedClassDefs>),
    TupleDef(Box<TupleDef>),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TypeAttr {
    Optional,
    Plain,
    Repeated,
}

// ---- Walk context ------------------------------------------------------------

pub struct AsdlAst<'a> {
    /// Pre-materialized char view of the input. The parser already consumes
    /// `text.chars()` to drive `parse_step`, so the same `Vec<char>` is what
    /// the gen indices in `history` refer to. Passing it in avoids re-indexing
    /// a UTF-8 `&str` (where byte index != char index).
    pub source_chars: &'a [char],
    pub history: &'a [KernelSet],
    pub ids: IdIssuer,
}

impl<'a> AsdlAst<'a> {
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

    pub fn match_start(&mut self) -> ModuleDef {
        let last_gen = self.source_chars.len() as i32;
        let kernel = self.history[last_gen as usize].get_single(2, 1, 0, last_gen);
        self.match_defs(kernel.begin_gen, kernel.end_gen)
    }

    fn match_defs(&mut self, begin_gen: i32, end_gen: i32) -> ModuleDef {
        let var1 = get_sequence_elems(self.history, 3, &[4, 47, 4], begin_gen, end_gen);
        self.match_module_def(var1[1].0, var1[1].1)
    }

    fn match_module_def(&mut self, begin_gen: i32, end_gen: i32) -> ModuleDef {
        let var3 = get_sequence_elems(
            self.history,
            48,
            &[49, 4, 67, 4, 68, 4, 69, 126, 4, 131],
            begin_gen,
            end_gen,
        );
        let name = self.match_name(var3[2].0, var3[2].1);
        let first_def = self.match_super_class_def(var3[6].0, var3[6].1);
        let rest: Vec<SuperClassDef> =
            unroll_repeat0(self.history, 126, 128, 7, 127, var3[7].0, var3[7].1)
                .into_iter()
                .map(|k| {
                    let inner = get_sequence_elems(self.history, 130, &[4, 69], k.0, k.1);
                    self.match_super_class_def(inner[1].0, inner[1].1)
                })
                .collect();
        let mut defs = Vec::with_capacity(1 + rest.len());
        defs.push(first_def);
        defs.extend(rest);
        let node_id = self.next_id();
        ModuleDef {
            name,
            defs,
            node_id,
            start: begin_gen,
            end: end_gen,
        }
    }

    fn match_name(&mut self, begin_gen: i32, end_gen: i32) -> String {
        let var10 = get_sequence_elems(self.history, 59, &[60], begin_gen, end_gen);
        let var11 = get_sequence_elems(self.history, 63, &[64], var10[0].0, var10[0].1);
        let chars: Vec<char> = unroll_repeat1(self.history, 64, 65, 65, 66, var11[0].0, var11[0].1)
            .into_iter()
            .map(|k| self.char_at(k.0))
            .collect();
        chars.into_iter().collect()
    }

    fn match_super_class_def(&mut self, begin_gen: i32, end_gen: i32) -> SuperClassDef {
        let var13 = get_sequence_elems(
            self.history,
            70,
            &[67, 4, 71, 4, 72, 111],
            begin_gen,
            end_gen,
        );
        let name = self.match_name(var13[0].0, var13[0].1);
        let body = self.match_super_class_def_body(var13[4].0, var13[4].1);
        let var16 = self.history[var13[5].1 as usize].find_by_begin_gen_opt(96, 1, var13[5].0);
        let var17 = self.history[var13[5].1 as usize].find_by_begin_gen_opt(112, 1, var13[5].0);
        assert!(has_single_true(&[var16.is_some(), var17.is_some()]));
        let attrs = if var16.is_some() {
            None
        } else {
            let inner = get_sequence_elems(self.history, 114, &[4, 115], var13[5].0, var13[5].1);
            Some(self.match_attributes_def(inner[1].0, inner[1].1))
        };
        let node_id = self.next_id();
        SuperClassDef {
            name,
            body,
            attrs,
            node_id,
            start: begin_gen,
            end: end_gen,
        }
    }

    fn match_super_class_def_body(
        &mut self,
        begin_gen: i32,
        end_gen: i32,
    ) -> SuperClassDefBody {
        let var22 = self.history[end_gen as usize].find_by_begin_gen_opt(73, 2, begin_gen);
        let var23 = self.history[end_gen as usize].find_by_begin_gen_opt(110, 1, begin_gen);
        assert!(has_single_true(&[var22.is_some(), var23.is_some()]));
        if var22.is_some() {
            let var25 = get_sequence_elems(self.history, 73, &[74, 104], begin_gen, end_gen);
            let head = self.match_sub_class_def(var25[0].0, var25[0].1);
            let tail: Vec<SubClassDef> =
                unroll_repeat0(self.history, 104, 106, 7, 105, var25[1].0, var25[1].1)
                    .into_iter()
                    .map(|k| {
                        let inner = get_sequence_elems(
                            self.history,
                            108,
                            &[4, 109, 4, 74],
                            k.0,
                            k.1,
                        );
                        self.match_sub_class_def(inner[3].0, inner[3].1)
                    })
                    .collect();
            let mut subs = Vec::with_capacity(1 + tail.len());
            subs.push(head);
            subs.extend(tail);
            let node_id = self.next_id();
            SuperClassDefBody::SealedClassDefs(Box::new(SealedClassDefs {
                subs,
                node_id,
                start: begin_gen,
                end: end_gen,
            }))
        } else {
            let var31 = get_sequence_elems(self.history, 110, &[80], begin_gen, end_gen);
            let body = self.match_params(var31[0].0, var31[0].1);
            let node_id = self.next_id();
            SuperClassDefBody::TupleDef(Box::new(TupleDef {
                body,
                node_id,
                start: begin_gen,
                end: end_gen,
            }))
        }
    }

    fn match_sub_class_def(&mut self, begin_gen: i32, end_gen: i32) -> SubClassDef {
        let var34 = get_sequence_elems(self.history, 75, &[67, 76], begin_gen, end_gen);
        let name = self.match_name(var34[0].0, var34[0].1);
        let var36 = self.history[var34[1].1 as usize].find_by_begin_gen_opt(77, 1, var34[1].0);
        let var37 = self.history[var34[1].1 as usize].find_by_begin_gen_opt(96, 1, var34[1].0);
        assert!(has_single_true(&[var36.is_some(), var37.is_some()]));
        let params = if var36.is_some() {
            let inner = get_sequence_elems(self.history, 79, &[4, 80], var34[1].0, var34[1].1);
            Some(self.match_params(inner[1].0, inner[1].1))
        } else {
            None
        };
        let node_id = self.next_id();
        SubClassDef {
            name,
            params,
            node_id,
            start: begin_gen,
            end: end_gen,
        }
    }

    fn match_params(&mut self, begin_gen: i32, end_gen: i32) -> Vec<Param> {
        let var42 = get_sequence_elems(
            self.history,
            81,
            &[82, 4, 83, 97, 4, 103],
            begin_gen,
            end_gen,
        );
        let head = self.match_param(var42[2].0, var42[2].1);
        let tail: Vec<Param> = unroll_repeat0(self.history, 97, 99, 7, 98, var42[3].0, var42[3].1)
            .into_iter()
            .map(|k| {
                let inner = get_sequence_elems(self.history, 101, &[4, 102, 4, 83], k.0, k.1);
                self.match_param(inner[3].0, inner[3].1)
            })
            .collect();
        let mut out = Vec::with_capacity(1 + tail.len());
        out.push(head);
        out.extend(tail);
        out
    }

    fn match_param(&mut self, begin_gen: i32, end_gen: i32) -> Param {
        let var47 = get_sequence_elems(self.history, 84, &[67, 85, 4, 67], begin_gen, end_gen);
        let type_name = self.match_name(var47[0].0, var47[0].1);
        let var50 = self.history[var47[1].1 as usize].find_by_begin_gen_opt(86, 1, var47[1].0);
        let var51 = self.history[var47[1].1 as usize].find_by_begin_gen_opt(96, 1, var47[1].0);
        assert!(has_single_true(&[var50.is_some(), var51.is_some()]));
        let attr = if var50.is_some() {
            let inner = get_sequence_elems(self.history, 88, &[4, 89], var47[1].0, var47[1].1);
            let var54 = self.history[inner[1].1 as usize].find_by_begin_gen_opt(90, 1, inner[1].0);
            let var55 = self.history[inner[1].1 as usize].find_by_begin_gen_opt(93, 1, inner[1].0);
            assert!(has_single_true(&[var54.is_some(), var55.is_some()]));
            Some(if var54.is_some() {
                TypeAttr::Repeated
            } else {
                TypeAttr::Optional
            })
        } else {
            None
        };
        let name = self.match_name(var47[3].0, var47[3].1);
        let node_id = self.next_id();
        Param {
            type_name,
            type_attr: attr.unwrap_or(TypeAttr::Plain),
            name,
            node_id,
            start: begin_gen,
            end: end_gen,
        }
    }

    fn match_attributes_def(&mut self, begin_gen: i32, end_gen: i32) -> Attributes {
        let var59 = get_sequence_elems(self.history, 116, &[117, 4, 80], begin_gen, end_gen);
        let attrs = self.match_params(var59[2].0, var59[2].1);
        let node_id = self.next_id();
        Attributes {
            attrs,
            node_id,
            start: begin_gen,
            end: end_gen,
        }
    }
}

