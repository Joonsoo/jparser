// Hand-written target models for `Stage4RustEmit` (mgroup3 Phase B).
//
// These are not consumed by any build yet — they exist purely to nail down the
// shape of code that the per-grammar Rust emitter must produce. Pair each file
// with its Kotlin counterpart under `../kotlin/com/giyeok/jparser/ktlib/test/`
// when iterating on the emitter design:
//
//   ktlib_ast/mod.rs   ←→  ktlib/main/kotlin/com/giyeok/jparser/ktlib/AstifierUtil.kt
//                          ktlib/main/kotlin/com/giyeok/jparser/ktlib/KernelSet.kt
//   asdl_ast.rs        ←→  AsdlAst.kt
//   pyobj_opt_ast.rs   ←→  PyObjOptAst.kt
//
// The `ktlib_ast` module is meant to graduate into a stable crate
// (`mgroup3-native` or a new `mgroup3-ktlib`) once the emitter is wired.

pub mod ktlib_ast;
pub mod asdl_ast;
pub mod pyobj_opt_ast;
