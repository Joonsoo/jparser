package com.giyeok.jparser.metalang3.codegen

import com.giyeok.jparser.NGrammar.{NNonterminal, NRepeat, NStart}
import com.giyeok.jparser.Symbols
import com.giyeok.jparser.metalang3.MetaLanguage3.{ProcessedGrammar, check}
import com.giyeok.jparser.metalang3.{ClassHierarchyItem, ClassRelationCollector, Type, ValuefyExpr}

import java.io.StringWriter
import scala.annotation.tailrec

/**
 * DRAFT — Rust counterpart of [[KotlinOptCodeGen]].
 *
 * Generates a Rust module that walks a `Vec<KernelSet>` (kernels_history) into
 * a grammar-specific typed AST, mirroring what `KotlinOptCodeGen` produces on
 * the JVM side. The *structure* of the walk is identical (it recurses over the
 * same `ValuefyExpr` tree); only the emitted syntax differs.
 *
 * Differences from the Kotlin generator that drive the syntax mapping:
 *   - `val x = e`                  → `let x = e;`
 *   - `when { c -> a; else -> b }` → `if c { a } else { b }`
 *   - `listOf(a, b)`               → `vec![a, b]`
 *   - `xs.map { k -> e }`          → `xs.into_iter().map(|k| e).collect()`
 *   - `x ?: y`                     → `x.unwrap_or_else(|| y)` (or `.unwrap_or(y)`)
 *   - `e?.let { x -> f(x) }`       → `e.map(|x| f(x))`
 *   - `source[g]`                  → `self.source_chars[g as usize]`
 *   - `source.length`             → `self.source_chars.len() as i32`
 *   - `nextId()`                  → `self.next_id()`  (requires `&mut self`)
 *   - nullable `T?`                → `Option<T>`
 *   - construct `Foo(a, .., id, b, e)` → `Foo { f1: a, .., node_id: id, start: b, end: e }`
 *       (field names come from `analysis.classParamTypes`)
 *
 * Self-reference is broken with `Box<T>` on every `Msg`-typed field, matching
 * the PoC under `examples/generated/rust/asdl_ast.rs`. A precise SCC analysis
 * can unbox acyclic references later; always-Box is correct, just not minimal.
 *
 * Source access: the parser drives `parse_step` from `text.chars()`, so gen
 * indices index a `&[char]`, NOT a UTF-8 `&str`. The generated walker holds
 * `source_chars: &[char]`. See mgroup3/docs/phase_b_proto_design.md.
 *
 * NOT YET IMPLEMENTED here (separate stages): the typed-AST → proto ID-table
 * encoder (see [[RustProtoEncoderGen]]) and the AST type definitions (structs/
 * enums + AstNode trait, see [[RustAstTypeGen]]).
 */
class RustOptCodeGen(val analysis: ProcessedGrammar) {
  private var varId: Int = 0

  def newVar(): String = {
    varId += 1
    s"var$varId"
  }

  var _requiredNonterms: Set[String] = Set()
  var _symbolsOfInterest: Set[Int] = Set()

  def symbolsOfInterest: Set[Int] = _symbolsOfInterest

  // ---- expression / statement blob (same shape as KotlinOptCodeGen.ExprBlob) -

  /**
   * A chunk of generated code: a list of preparatory statements plus a final
   * result expression. `required` carries the set of nonterminal names that
   * the result references and therefore must also be generated.
   */
  case class ExprBlob(prepares: List[String], result: String, required: Set[String]) {
    // Result wrapped in a block when used where a single expression is needed
    // but `prepares` is non-empty. In Rust a `{ stmts; expr }` block is itself
    // an expression, so we can always inline.
    def asBlockExpr: String =
      if (prepares.isEmpty) result
      else s"{ ${prepares.mkString(" ")} $result }"
  }

  object ExprBlob {
    def code(result: String): ExprBlob = ExprBlob(List(), result, Set())
  }

  // ---- char escaping (Rust syntax) -------------------------------------------

  def escapeChar(c: Char): String = c match {
    case '\b' => "\\u{8}"
    case '\n' => "\\n"
    case '\r' => "\\r"
    case '\t' => "\\t"
    case '\\' => "\\\\"
    case '\'' => "\\'"
    case c if c.toInt < 0x20 || c.toInt > 0x7e =>
      f"\\u{${c.toInt}%x}"
    case c => c.toString
  }

  def escapeString(s: String): String = s.flatMap {
    case '\n' => "\\n"
    case '\r' => "\\r"
    case '\t' => "\\t"
    case '\\' => "\\\\"
    case '"' => "\\\""
    case c if c.toInt < 0x20 || c.toInt > 0x7e => f"\\u{${c.toInt}%x}"
    case c => c.toString
  }

  // ---- naming ----------------------------------------------------------------

  private def nonterminalMatchFuncName(nonterminal: String): String =
    s"match_${camelToSnake(nonterminal)}"

  def camelToSnake(s: String): String = {
    val sb = new StringBuilder
    s.zipWithIndex.foreach { case (c, i) =>
      if (c.isUpper) {
        if (i > 0) sb.append('_')
        sb.append(c.toLower)
      } else sb.append(c)
    }
    sb.toString
  }

  /** snake_case + reserved-word avoidance for struct field names. */
  def rustFieldName(name: String): String = {
    val snake = camelToSnake(name)
    if (RustOptCodeGen.RustKeywords.contains(snake)) s"${snake}_" else snake
  }

  /**
   * Type-position-safe class name. `Self` cannot even be a raw identifier, and
   * prelude/support names (Box, Option, Ctx, ...) would shadow what the
   * generated code references unqualified. Renamed to `<name>Node` — applied to
   * Rust type names AND (via SchemaBuilder.rustSafeName, keep the lists in
   * sync) the proto message names, so prost output matches. Kotlin AST keeps
   * the original name; to_short_string also prints the original.
   */
  def rustClassName(name: String): String =
    if (RustOptCodeGen.RustReservedTypeNames.contains(name)) name + "Node" else name

  // ---- type rendering --------------------------------------------------------
  //
  // Mirrors Stage4RustEmit.rustType so the walker and the AST type definitions
  // agree on field types. `Msg` is always `Box<T>` to break cycles.

  def rustType(t: Type): String = t match {
    case Type.NodeType => "Vec<u8>"
    case Type.ClassType(name) => s"Box<${rustClassName(name)}>"
    case Type.OptionalOf(typ) => s"Option<${rustType(typ)}>"
    case Type.ArrayOf(typ) => s"Vec<${rustTypeUnboxed(typ)}>"
    case unionType: Type.UnionOf =>
      analysis.reduceUnionType(unionType) match {
        case reduced: Type.UnionOf =>
          throw new Exception(s"irreducible union type: ${Type.readableNameOf(reduced)}")
        case reduced => rustType(reduced)
      }
    case Type.EnumType(enumName) => enumName
    case Type.UnspecifiedEnumType(uniqueId) => analysis.shortenedEnumTypesMap(uniqueId)
    case Type.NullType => "Vec<u8>"
    case Type.AnyType => "Vec<u8>"
    case Type.BoolType => "bool"
    case Type.CharType => "char"
    case Type.StringType => "String"
    case Type.NothingType => "Vec<u8>"
  }

  // Vec<T> inner: Vec already heap-allocates, so the inner Msg need not be Boxed.
  private def rustTypeUnboxed(t: Type): String = t match {
    case Type.ClassType(name) => rustClassName(name)
    case _ => rustType(t)
  }

  /**
   * Return type for `match_*` functions. Message values are returned UNBOXED at
   * every level the walker controls: a bare `ModuleDef`, `Option<Attributes>`
   * (not `Option<Box<Attributes>>`), `Vec<Param>`. Boxing is added only when a
   * value is *stored into a struct field* (ConstructCall), so there is exactly
   * one place that introduces `Box`. This avoids double-boxing, since the
   * metalang type system has no notion of `Box` to detect "already boxed".
   */
  private def rustReturnType(t: Type): String = t match {
    case Type.ClassType(name) => rustClassName(name)
    case Type.OptionalOf(inner) => s"Option<${rustReturnType(inner)}>"
    case Type.ArrayOf(inner) => s"Vec<${rustReturnType(inner)}>"
    case _ => rustType(t)
  }

  // ---- coercion --------------------------------------------------------------
  //
  // Rust makes explicit three things the Kotlin codegen gets for free:
  //   - nullable promotion: `x`        used where `T?`        → `Some(x)`
  //   - boxing:             `T`        stored in `Box<T>`     → `Box::new(x)`
  //   - sealed up-cast:     child `C`  used where parent `P`  → `P::C(Box::new(x))`
  //
  // `coerce(expr, exprType, expectedRustShape)` rewrites the expression. We do
  // NOT chase the source type through bindings — we drive purely off `exprType`
  // (the inferred type of the producing ValuefyExpr) and the expected shape.

  private def classNameOf(t: Type): Option[String] = reduceType(t) match {
    case Type.ClassType(name) => Some(name)
    case _ => None
  }

  /** class -> ALL direct sealed parents (a class can belong to multiple unions). */
  private lazy val parentsOf: Map[String, Set[String]] = {
    val hierarchy = analysis.classRelations.toHierarchy
    (for {
      item <- hierarchy.allTypes.values.toList
      sub <- item.subclasses
    } yield sub -> item.className).groupBy(_._1).view.mapValues(_.map(_._2).toSet).toMap
  }

  /**
   * Direct-edge inheritance path from `child` up to `parent` (BFS, shortest).
   * Returned head-first from parent: List(parent, ..., child). None if not an
   * ancestor.
   */
  private def upcastPath(child: String, parent: String): Option[List[String]] = {
    if (child == parent) return Some(List(child))
    val visited = scala.collection.mutable.Set[String](child)
    val queue = scala.collection.mutable.Queue[List[String]](List(child))
    while (queue.nonEmpty) {
      val path = queue.dequeue()
      for (p <- parentsOf.getOrElse(path.head, Set())) {
        if (p == parent) return Some(p :: path)
        if (visited.add(p)) queue.enqueue(p :: path)
      }
    }
    None
  }

  private def reduceType(t: Type): Type = t match {
    case u: Type.UnionOf => analysis.reduceUnionType(u) match {
      case u2: Type.UnionOf =>
        // metalang 의 reduceUnionType 이 못 줄이는 union 이라도 class hierarchy 상
        // 공통 sealed 조상이 있으면 그 타입으로 — 중첩 choice 의 arm 들이 그 부모로
        // up-cast 되어야 Rust 의 if/else 타입이 통일된다.
        commonParentOf(u2).map(Type.ClassType).getOrElse(u2)
      case r => reduceType(r)
    }
    case _ => t
  }

  /**
   * Least common sealed ancestor of all members of an (irreducible) union,
   * via parentsOf BFS. Members must all be class types. Minimal total distance,
   * name-ordered for determinism.
   */
  private def commonParentOf(u: Type.UnionOf): Option[String] = {
    val members = u.types.toList.map(classNameOf)
    if (members.exists(_.isEmpty)) None
    else {
      def ancestorsOf(c: String): Map[String, Int] = {
        val dist = scala.collection.mutable.Map[String, Int]()
        val queue = scala.collection.mutable.Queue[(String, Int)]((c, 0))
        while (queue.nonEmpty) {
          val (cur, d) = queue.dequeue()
          for (p <- parentsOf.getOrElse(cur, Set.empty[String])) {
            if (!dist.contains(p)) {
              dist(p) = d + 1
              queue.enqueue((p, d + 1))
            }
          }
        }
        dist.toMap
      }
      val ancMaps = members.map(m => ancestorsOf(m.get))
      val common = ancMaps.map(_.keySet).reduce(_ intersect _)
      if (common.isEmpty) None
      else Some(common.toList.map(p => (ancMaps.map(_.apply(p)).sum, p)).sorted.head._2)
    }
  }

  /**
   * The expected target for a coercion. We only need a few shapes:
   *   - `FieldOf(declaredType)` — storing into a struct field (Box<T> for Msg)
   *   - `ReturnOf(nontermType)` — returning from a match_* fn (unboxed T)
   *   - `ElemOf(arrayElemType)` — an element of a Vec<unboxed>
   */
  sealed trait Target
  case class FieldOf(t: Type) extends Target
  case class ReturnOf(t: Type) extends Target
  case class ElemOf(t: Type) extends Target

  /**
   * Coerce `expr`, whose producing expression has inferred type `exprType`,
   * to satisfy `target`. The key facts:
   *   - match_* functions return UNBOXED class values, so the producer of a
   *     class is a bare `T`.
   *   - struct fields are `Box<T>` for Msg, `Option<Box<T>>` for Opt(Msg),
   *     `Vec<T>` (unboxed) for Arr(Msg).
   *   - sealed parents are enums; a child value must be wrapped in `P::C(Box)`.
   */
  private def coerce(expr: String, exprType: Type, target: Target): String = target match {
    case ReturnOf(t) =>
      // Return type is unboxed. Adjust for sealed up-cast, and for a bare value
      // flowing into an Optional return (`Some(...)`).
      reduceType(t) match {
        case Type.OptionalOf(inner) => coerceBranch(expr, exprType, Type.OptionalOf(inner))
        case other => sealedUpcast(expr, exprType, other)
      }
    case ElemOf(t) =>
      // Vec elements are unboxed; sealed up-cast only.
      sealedUpcast(expr, exprType, reduceType(t))
    case FieldOf(decl) =>
      reduceType(decl) match {
        case Type.ClassType(name) =>
          // Box<name>. Up-cast first (yields the parent enum value), then box —
          // the field itself is Box<Parent> regardless of whether an up-cast
          // happened.
          s"Box::new(${sealedUpcast(expr, exprType, Type.ClassType(name))})"
        case Type.OptionalOf(inner) =>
          // Field is `Option<Box<inner>>` (Msg) or `Option<inner>` (scalar).
          // The match value is `Option<inner-unboxed>` or a bare value. We add
          // the Box here (the single boxing site) and wrap bare values in Some.
          reduceType(exprType) match {
            case Type.OptionalOf(_) | Type.NullType =>
              // Already an Option; box the inner Msg via map.
              reduceType(inner) match {
                case Type.ClassType(_) => s"$expr.map(|x| Box::new(x))"
                case _ => expr
              }
            case _ =>
              // Bare value: up-cast/Box the inner, then wrap in Some.
              s"Some(${boxInnerForField(expr, inner)})"
          }
        case Type.ArrayOf(_) =>
          // Vec<unboxed>; elements coerced at construction (ArrayExpr/unroll).
          expr
        case _ => expr
      }
  }

  /** Box / up-cast a bare value being stored as the inner of an `Option<Box<_>>` field. */
  private def boxInnerForField(expr: String, inner: Type): String = reduceType(inner) match {
    case Type.ClassType(name) =>
      s"Box::new(${sealedUpcast(expr, inner, Type.ClassType(name))})"
    case _ => expr
  }

  /**
   * If `expr` (type `exprType`) is a (transitive) child of sealed `expected`,
   * wrap it level by level: P::M(Box::new(M::C(Box::new(expr)))).
   */
  private def sealedUpcast(expr: String, exprType: Type, expected: Type): String = {
    (classNameOf(exprType), reduceType(expected)) match {
      case (Some(child), Type.ClassType(parent)) if child != parent =>
        upcastPath(child, parent) match {
          case Some(path) =>
            // path = [parent, ..., child]; wrap from the child end outward.
            var acc = expr
            path.sliding(2).toList.reverse.foreach {
              case List(p, c) => acc = s"${rustClassName(p)}::${rustClassName(c)}(Box::new($acc))"
              case _ =>
            }
            acc
          case None => expr
        }
      case _ => expr
    }
  }

  /**
   * Coerce one branch of an if/ternary to the branch-result type. Match values
   * are unboxed at this level (boxing happens only at field stores), so we only
   * wrap a bare value flowing into an Optional result in `Some(...)`, applying a
   * sealed up-cast to the inner if needed. A branch already Option/Null passes
   * through.
   */
  private def coerceBranch(expr: String, from: Type, resultType: Type): String =
    reduceType(resultType) match {
      case Type.OptionalOf(inner) =>
        reduceType(from) match {
          case Type.OptionalOf(_) | Type.NullType => expr
          case _ => s"Some(${sealedUpcast(expr, from, inner)})"
        }
      case other => sealedUpcast(expr, from, other)
    }

  // ---- top-level entry: matchStart -------------------------------------------

  def matchStartFunc(): String = {
    varId = 0
    val startSymbol = analysis.ngrammar
      .symbolOf(analysis.ngrammar.nsymbols(analysis.ngrammar.startSymbol).asInstanceOf[NStart].produce)
      .asInstanceOf[NNonterminal]
    val returnType = rustReturnType(analysis.nonterminalTypes(analysis.startNonterminalName))
    _requiredNonterms += startSymbol.symbol.name
    _symbolsOfInterest += startSymbol.id
    s"""    pub fn match_start(&mut self) -> $returnType {
       |        let last_gen = self.source_chars.len() as i32;
       |        let kernel = self.history[last_gen as usize].get_single(${startSymbol.id}, 1, 0, last_gen);
       |        ${nonterminalMatchFuncName(startSymbol.symbol.name)}_(self, kernel.begin_gen, kernel.end_gen)
       |    }""".stripMargin
  }

  // ---- per-nonterminal match function ----------------------------------------
  //
  // Free functions taking `&mut Ctx` (named `match_x_`) rather than methods, to
  // sidestep Rust's restriction on calling `self.match_x()` while another field
  // of self is borrowed. The trailing underscore keeps them distinct from the
  // public `match_start` method on the Ctx struct.

  def nonterminalMatchFunc(nonterminal: String): String = {
    varId = 0
    val valuefyExpr = analysis.nonterminalValuefyExprs(nonterminal)
    val nontermType = analysis.nonterminalTypes(nonterminal)
    val body = unrollChoicesExpr(valuefyExpr.choices, "begin_gen", "end_gen", nonterminal, nontermType)
    val returnType = rustReturnType(nontermType)
    // The body's result type equals the nonterminal type; the only reshaping is
    // a sealed up-cast when each choice produced a concrete child but the
    // declared return is the parent. unrollChoicesExpr already up-casts per-arm
    // when there are multiple choices, so this top-level coerce is a no-op in
    // that case and only fires for single-choice bodies.
    val finalResult = coerce(body.result, nontermType, ReturnOf(nontermType))
    val bodyLines = (body.prepares :+ finalResult).map("        " + _).mkString("\n")
    s"""fn ${nonterminalMatchFuncName(nonterminal)}_(ctx: &mut Ctx, begin_gen: i32, end_gen: i32) -> $returnType {
       |$bodyLines
       |}""".stripMargin
  }

  def unrollChoicesExpr(
    choicesMap: Map[Symbols.Symbol, ValuefyExpr],
    beginGen: String,
    endGen: String,
    parentSymbolName: String,
    expectedType: Type,
  ): ExprBlob = {
    if (choicesMap.size == 1) {
      val (choiceSymbol, choiceExpr) = choicesMap.head
      val singleHint = reduceType(expectedType) match {
        case Type.ClassType(_) => Some(expectedType)
        case _ => None
      }
      val inner = valuefyExprToCode(choiceExpr, beginGen, endGen, choiceSymbol, SequenceVarName(None), singleHint)
      // Even with a single choice, the produced concrete type may be a child of
      // the declared (parent) type — up-cast the result. (e.g. JSON `Element = Value`.)
      val coerced = coerce(inner.result, producedType(choiceExpr), ReturnOf(expectedType))
      if (coerced == inner.result) inner
      else inner.copy(result = coerced)
    } else {
      val choiceSymbols = choicesMap.keys.toList.sortBy(analysis.ngrammar.idOf)
      val choiceVars = choiceSymbols.map(_ => newVar())
      val choices = choiceVars.zip(choiceSymbols)

      val tryCodes = choices.map { case (varName, choiceSymbol) =>
        val symbolId = analysis.ngrammar.idOf(choiceSymbol)
        val lastPointer = analysis.ngrammar.lastPointerOf(symbolId)
        _symbolsOfInterest += symbolId
        s"let $varName = ctx.history[$endGen as usize].find_by_begin_gen_opt($symbolId, $lastPointer, $beginGen);"
      }

      val assertCode = List(
        s"assert!(has_single_true(&[${choiceVars.map(_ + ".is_some()").mkString(", ")}]));"
      )

      val v = newVar()
      var requires = Set[String]()
      // Build an if / else if / else chain. Each arm is a Rust block expression,
      // up-cast to the expected (parent) type when the arm produced a concrete
      // sealed child.
      val armExprs = choices.zipWithIndex.map { case ((varName, choiceSymbol), index) =>
        val choiceExpr = choicesMap(choiceSymbol)
        val armHint = reduceType(expectedType) match {
          case Type.ClassType(_) => Some(expectedType)
          case _ => None
        }
        val exprCode = valuefyExprToCode(choiceExpr, beginGen, endGen, choiceSymbol, SequenceVarName(None), armHint)
        requires ++= exprCode.required
        val armType = producedType(choiceExpr)
        (classNameOf(armType), reduceType(expectedType)) match {
          case (Some(c), Type.ClassType(par)) if c != par && upcastPath(c, par).isEmpty =>
            System.err.println(s"DBGNOUP arm=$c expected=$par")
          case (None, Type.ClassType(par)) =>
            System.err.println(s"DBGNOUP nonclass-arm=${Type.readableNameOf(reduceType(armType))} expected=$par")
          case _ =>
        }
        val coerced = coerce(exprCode.asBlockExpr, armType, ReturnOf(expectedType))
        val isLast = index == choices.size - 1
        val cond = if (isLast) None else Some(s"$varName.is_some()")
        (cond, coerced)
      }
      val chain = armExprs.zipWithIndex.map { case ((cond, blockExpr), idx) =>
        cond match {
          case Some(c) if idx == 0 => s"if $c { $blockExpr }"
          case Some(c) => s"else if $c { $blockExpr }"
          case None => s"else { $blockExpr }"
        }
      }.mkString(" ")

      ExprBlob(tryCodes ++ assertCode ++ List(s"let $v = $chain;"), v, requires)
    }
  }

  private def typeOf(expr: ValuefyExpr): Type = analysis.typeInferer.typeOfValuefyExpr(expr).get

  /**
   * The type the generated Rust expression ACTUALLY produces. The type
   * inferer may type a choice arm by its enclosing union (the parent class),
   * which hides the need for a sealed up-cast — derive the concrete type
   * structurally where the expression shape determines it.
   */
  private def producedType(expr: ValuefyExpr): Type = expr match {
    case ValuefyExpr.ConstructCall(className, _) => Type.ClassType(className)
    case ValuefyExpr.MatchNonterminal(nonterminalName) =>
      analysis.nonterminalTypes(nonterminalName)
    case ValuefyExpr.Unbind(_, e) => producedType(e)
    case ValuefyExpr.SeqElemAt(_, e) => producedType(e)
    case ValuefyExpr.JoinBody(e) => producedType(e)
    case ValuefyExpr.JoinCond(e) => producedType(e)
    case _ => typeOf(expr)
  }

  case class SequenceVarName(var name: Option[String])

  // ---- the central recursion -------------------------------------------------

  def valuefyExprToCode(
    valuefyExpr: ValuefyExpr,
    beginGen: String,
    endGen: String,
    symbol: Symbols.Symbol,
    sequenceVarName: SequenceVarName,
    // 바깥 문맥이 기대하는 타입 — 중첩 UnrollChoices 의 arm 통일 타입을 문맥에
    // 맞추기 위해 전파한다 (typeInferer 의 union 은 irreducible 일 수 있고, LCA
    // 휴리스틱은 문맥과 다른 조상을 고를 수 있다).
    expectedHint: Option[Type] = None,
  ): ExprBlob = valuefyExpr match {
    case ValuefyExpr.InputNode =>
      // Reconstruct a partial parse tree. Not used by the PoC grammars; defer.
      ???
    case ValuefyExpr.MatchNonterminal(nonterminalName) =>
      val v = newVar()
      _requiredNonterms += nonterminalName
      ExprBlob(List(s"let $v = ${nonterminalMatchFuncName(nonterminalName)}_(ctx, $beginGen, $endGen);"), v, Set())
    case ValuefyExpr.Unbind(sym, expr) =>
      valuefyExprToCode(expr, beginGen, endGen, sym, sequenceVarName, expectedHint)
    case ValuefyExpr.JoinBody(bodyProcessor) =>
      val joinSymbol = symbol.asInstanceOf[Symbols.Join]
      valuefyExprToCode(bodyProcessor, beginGen, endGen, joinSymbol.sym, SequenceVarName(None), expectedHint)
    case ValuefyExpr.JoinCond(condProcessor) =>
      val joinSymbol = symbol.asInstanceOf[Symbols.Join]
      valuefyExprToCode(condProcessor, beginGen, endGen, joinSymbol.join, SequenceVarName(None))
    case ValuefyExpr.SeqElemAt(index, expr) =>
      val sequenceId = analysis.ngrammar.idOf(symbol)
      val sequence = analysis.ngrammar.nsequences(sequenceId)
      sequenceVarName.name match {
        case Some(seqVar) =>
          _symbolsOfInterest += sequence.sequence(index)
          valuefyExprToCode(expr, s"$seqVar[$index].0", s"$seqVar[$index].1", sequence.symbol.seq(index), sequenceVarName)
        case None =>
          val seqVar = newVar()
          _symbolsOfInterest += sequenceId
          _symbolsOfInterest ++= sequence.sequence
          val getSequenceElems =
            s"let $seqVar = get_sequence_elems(&ctx.history, $sequenceId, &[${sequence.sequence.mkString(", ")}], $beginGen, $endGen);"
          sequenceVarName.name = Some(seqVar)
          val elemValuefy = valuefyExprToCode(expr, s"$seqVar[$index].0", s"$seqVar[$index].1", sequence.symbol.seq(index), sequenceVarName)
          ExprBlob(getSequenceElems +: elemValuefy.prepares, elemValuefy.result, elemValuefy.required)
      }
    case ValuefyExpr.UnrollRepeatFromZero(elemProcessor) =>
      unrollRepeatCode("unroll_repeat0", elemProcessor, beginGen, endGen, symbol, arrayElemType(valuefyExpr))
    case ValuefyExpr.UnrollRepeatFromZeroNoUnbind(repeatSymbol, elemProcessor) =>
      assert(symbol == repeatSymbol)
      unrollRepeatCode("unroll_repeat0", elemProcessor, beginGen, endGen, symbol, arrayElemType(valuefyExpr))
    case ValuefyExpr.UnrollRepeatFromOne(elemProcessor) =>
      unrollRepeatCode("unroll_repeat1", elemProcessor, beginGen, endGen, symbol, arrayElemType(valuefyExpr))
    case ValuefyExpr.UnrollRepeatFromOneNoUnbind(repeatSymbol, elemProcessor) =>
      assert(symbol == repeatSymbol)
      unrollRepeatCode("unroll_repeat1", elemProcessor, beginGen, endGen, symbol, arrayElemType(valuefyExpr))
    case ValuefyExpr.UnrollChoices(choices) =>
      unrollChoicesExpr(choices, beginGen, endGen, symbol.toShortString, expectedHint.getOrElse(typeOf(valuefyExpr)))
    case ValuefyExpr.ConstructCall(className, params) =>
      val paramCodes = params.map(valuefyExprToCode(_, beginGen, endGen, symbol, sequenceVarName))
      val declParams = analysis.classParamTypes.getOrElse(className, List())
      check(declParams.size == paramCodes.size,
        s"param count mismatch for $className: ${declParams.size} fields vs ${paramCodes.size} args")
      val v = newVar()
      val fieldAssigns = declParams.zip(paramCodes).zip(params).map { case (((pname, fieldType), pc), paramExpr) =>
        val coerced = coerce(pc.result, producedType(paramExpr), FieldOf(fieldType))
        s"${rustFieldName(pname)}: $coerced"
      }
      // Include node_id/start/end in the same list so an empty param list does
      // not produce a leading comma (case objects / no-field case classes).
      val allAssigns = fieldAssigns ++ List(
        s"node_id: ctx.next_id()", s"start: $beginGen", s"end: $endGen")
      val construct =
        s"let $v = ${rustClassName(className)} { ${allAssigns.mkString(", ")} };"
      ExprBlob(paramCodes.flatMap(_.prepares) :+ construct, v, paramCodes.flatMap(_.required).toSet)
    case ValuefyExpr.FuncCall(funcType, params) =>
      funcCallToCode(funcType, params, beginGen, endGen, symbol, sequenceVarName)
    case ValuefyExpr.ArrayExpr(elems) =>
      val elemType = arrayElemType(valuefyExpr)
      val elemCodes = elems.map(valuefyExprToCode(_, beginGen, endGen, symbol, sequenceVarName))
      val coercedElems = elemCodes.zip(elems).map { case (ec, elem) =>
        coerce(ec.result, typeOf(elem), ElemOf(elemType))
      }
      ExprBlob(
        elemCodes.flatMap(_.prepares),
        s"vec![${coercedElems.mkString(", ")}]",
        elemCodes.flatMap(_.required).toSet)
    case ValuefyExpr.BinOp(op, lhs, rhs) =>
      val lhsCode = valuefyExprToCode(lhs, beginGen, endGen, symbol, sequenceVarName)
      val rhsCode = valuefyExprToCode(rhs, beginGen, endGen, symbol, sequenceVarName)
      val opExpr = op match {
        case ValuefyExpr.BinOpType.ADD =>
          (typeOf(lhs), typeOf(rhs)) match {
            // String + String: build a new String. Array + Array: chain + collect.
            case (Type.StringType, Type.StringType) =>
              s"format!(\"{}{}\", ${lhsCode.result}, ${rhsCode.result})"
            case (Type.ArrayOf(_), Type.ArrayOf(_)) =>
              s"{ let mut v = ${lhsCode.result}; v.extend(${rhsCode.result}); v }"
          }
        case ValuefyExpr.BinOpType.EQ =>
          check(typeOf(lhs) == typeOf(rhs), "lhs and rhs of == must be same type")
          s"(${lhsCode.result} == ${rhsCode.result})"
        case ValuefyExpr.BinOpType.NE =>
          check(typeOf(lhs) == typeOf(rhs), "lhs and rhs of != must be same type")
          s"(${lhsCode.result} != ${rhsCode.result})"
        case ValuefyExpr.BinOpType.BOOL_AND =>
          s"(${lhsCode.result} && ${rhsCode.result})"
        case ValuefyExpr.BinOpType.BOOL_OR =>
          s"(${lhsCode.result} || ${rhsCode.result})"
      }
      ExprBlob(lhsCode.prepares ++ rhsCode.prepares, opExpr, lhsCode.required ++ rhsCode.required)
    case ValuefyExpr.PreOp(op, expr) =>
      op match {
        case ValuefyExpr.PreOpType.NOT =>
          check(typeOf(expr) == Type.BoolType, "not can be applied only to boolean expression")
          val exprCode = valuefyExprToCode(expr, beginGen, endGen, symbol, sequenceVarName)
          val resultVar = newVar()
          ExprBlob(exprCode.prepares :+ s"let $resultVar = !${exprCode.result};", resultVar, exprCode.required)
      }
    case ValuefyExpr.ElvisOp(expr, ifNull) =>
      // `expr ?: ifNull`  → `expr.unwrap_or_else(|| ifNull)`. The Option here is
      // the typed AST's Option<T>; ifNull may itself need prepares so wrap in a
      // closure body block.
      val exprVar = newVar()
      val exprCode = valuefyExprToCode(expr, beginGen, endGen, symbol, sequenceVarName)
      val ifNullCode = valuefyExprToCode(ifNull, beginGen, endGen, symbol, sequenceVarName)
      ExprBlob(
        exprCode.prepares :+ s"let $exprVar = ${exprCode.result};",
        s"$exprVar.unwrap_or_else(|| ${ifNullCode.asBlockExpr})",
        exprCode.required ++ ifNullCode.required)
    case ValuefyExpr.TernaryOp(condition, ifTrue, ifFalse) =>
      // Both branches must produce the ternary's result type. When that type is
      // Optional and a branch produced a bare value (e.g. `cond ? value : null`
      // lowered here), the branch is coerced to `Some(value)`; a `null` branch
      // already became `None`.
      val resultType = typeOf(valuefyExpr)
      val conditionCode = valuefyExprToCode(condition, beginGen, endGen, symbol, sequenceVarName)
      val thenCode = valuefyExprToCode(ifTrue, beginGen, endGen, symbol, sequenceVarName)
      val elseCode = valuefyExprToCode(ifFalse, beginGen, endGen, symbol, sequenceVarName)
      val thenExpr = coerceBranch(thenCode.asBlockExpr, typeOf(ifTrue), resultType)
      val elseExpr = coerceBranch(elseCode.asBlockExpr, typeOf(ifFalse), resultType)
      val resultVar = newVar()
      ExprBlob(
        conditionCode.prepares ++
          List(s"let $resultVar = if ${conditionCode.result} { $thenExpr } else { $elseExpr };"),
        resultVar,
        conditionCode.required ++ thenCode.required ++ elseCode.required)
    case literal: ValuefyExpr.Literal =>
      literal match {
        case ValuefyExpr.NullLiteral => ExprBlob.code("None")
        case ValuefyExpr.BoolLiteral(value) => ExprBlob.code(s"$value")
        case ValuefyExpr.CharLiteral(value) => ExprBlob.code(s"'${escapeChar(value)}'")
        case ValuefyExpr.CharFromTerminalLiteral =>
          ExprBlob(List(), s"ctx.source_chars[$beginGen as usize]", Set())
        case ValuefyExpr.StringLiteral(value) => ExprBlob.code("\"" + escapeString(value) + "\".to_string()")
      }
    case enumValue: ValuefyExpr.EnumValue =>
      enumValue match {
        case ValuefyExpr.CanonicalEnumValue(enumName, ev) => ExprBlob.code(s"$enumName::$ev")
        case ValuefyExpr.ShortenedEnumValue(unspecifiedEnumTypeId, ev) =>
          val enumName = analysis.shortenedEnumTypesMap(unspecifiedEnumTypeId)
          ExprBlob.code(s"$enumName::$ev")
      }
  }

  private def unrollRepeatCode(
    helperFn: String,
    elemProcessor: ValuefyExpr,
    beginGen: String,
    endGen: String,
    symbol: Symbols.Symbol,
    elemType: Type,
  ): ExprBlob = {
    val v = newVar()
    val symbolId = analysis.ngrammar.idOf(symbol)
    val repeat = analysis.ngrammar.symbolOf(symbolId).asInstanceOf[NRepeat]
    val itemSymId = analysis.ngrammar.idOf(repeat.symbol.sym)
    val elemCode = valuefyExprToCode(elemProcessor, "k.0", "k.1", repeat.symbol.sym, SequenceVarName(None))
    _symbolsOfInterest ++= Set(symbolId, itemSymId, repeat.baseSeq, repeat.repeatSeq)
    // Each element is coerced to the Vec's element type (sealed up-cast when the
    // processor produced a concrete child of a sealed element type).
    val coercedElem = coerce(elemCode.result, typeOf(elemProcessor), ElemOf(elemType))
    // `xs.map { k -> prepares; result }` → into_iter().map(|k| { prepares result }).collect()
    val closureBody =
      if (elemCode.prepares.isEmpty) coercedElem
      else s"${elemCode.prepares.mkString(" ")} $coercedElem"
    ExprBlob(
      List(
        s"let $v: Vec<_> = $helperFn(&ctx.history, $symbolId, $itemSymId, ${repeat.baseSeq}, ${repeat.repeatSeq}, $beginGen, $endGen)",
        s"    .into_iter().map(|k| { $closureBody }).collect();"),
      v,
      elemCode.required)
  }

  /** elemType of an Array-typed ValuefyExpr (after union reduction). */
  private def arrayElemType(valuefyExpr: ValuefyExpr): Type = reduceType(typeOf(valuefyExpr)) match {
    case Type.ArrayOf(elemType) => elemType
    case other => throw new Exception(s"expected array type, got ${Type.readableNameOf(other)}")
  }

  def funcCallToCode(
    funcType: ValuefyExpr.FuncType.Value,
    params: List[ValuefyExpr],
    beginGen: String,
    endGen: String,
    symbol: Symbols.Symbol,
    sequenceVarName: SequenceVarName,
  ): ExprBlob = funcType match {
    case ValuefyExpr.FuncType.IsPresent =>
      check(params.size == 1, "ispresent function only can have exactly one parameter")
      val param = valuefyExprToCode(params.head, beginGen, endGen, symbol, sequenceVarName)

      @tailrec def isPresentCode(paramType: Type): String = paramType match {
        case Type.ArrayOf(_) => s"!${param.result}.is_empty()"
        case Type.OptionalOf(_) => s"${param.result}.is_some()"
        case Type.StringType => s"!${param.result}.is_empty()"
        case unionType: Type.UnionOf =>
          val reduced = analysis.reduceUnionType(unionType)
          check(!reduced.isInstanceOf[Type.UnionOf], "union type not supported in ispresent function")
          isPresentCode(reduced)
      }

      ExprBlob(param.prepares, isPresentCode(typeOf(params.head)), param.required)
    case ValuefyExpr.FuncType.IsEmpty =>
      check(params.size == 1, "isempty function only can have exactly one parameter")
      val param = valuefyExprToCode(params.head, beginGen, endGen, symbol, sequenceVarName)

      @tailrec def isEmptyCode(paramType: Type): String = paramType match {
        case Type.ArrayOf(_) => s"${param.result}.is_empty()"
        case Type.OptionalOf(_) => s"${param.result}.is_none()"
        case Type.StringType => s"${param.result}.is_empty()"
        case unionType: Type.UnionOf =>
          val reduced = analysis.reduceUnionType(unionType)
          check(!reduced.isInstanceOf[Type.UnionOf], "union type not supported in isempty function")
          isEmptyCode(reduced)
      }

      ExprBlob(param.prepares, isEmptyCode(typeOf(params.head)), param.required)
    case ValuefyExpr.FuncType.Chr =>
      check(params.size == 1, "chr function only can have exactly one parameter")
      val paramCode = valuefyExprToCode(params.head, beginGen, endGen, symbol, sequenceVarName)
      val result = typeOf(params.head) match {
        case Type.CharType => paramCode.result
      }
      ExprBlob(paramCode.prepares, result, paramCode.required)
    case ValuefyExpr.FuncType.Str =>
      val paramCodes = params.map(valuefyExprToCode(_, beginGen, endGen, symbol, sequenceVarName))

      // Produces a String. Each piece contributes a `&str`/`String` fragment;
      // we concatenate with format! / push. To keep it simple and uniform we
      // build via a block that pushes each fragment into a String.
      def toStringCode(input: String, inputType: Type): String = inputType match {
        case Type.ArrayOf(elemType) =>
          s"""$input.into_iter().map(|it| ${toStringCode("it", elemType)}).collect::<String>()"""
        case Type.OptionalOf(valueType) =>
          s"""$input.map(|it| ${toStringCode("it", valueType)}).unwrap_or_default()"""
        case Type.BoolType => s"$input.to_string()"
        case Type.CharType => s"$input.to_string()"
        case Type.StringType => input
        case unionType: Type.UnionOf =>
          val reduced = analysis.reduceUnionType(unionType)
          check(!reduced.isInstanceOf[Type.UnionOf], "union type not supported in str function")
          toStringCode(input, reduced)
      }

      val pieces = paramCodes.zip(params).map { case (paramCode, param) =>
        toStringCode(paramCode.result, typeOf(param))
      }
      // Concatenate pieces: `{ let mut s = String::new(); s.push_str(&p0); ... s }`
      val result =
        if (pieces.size == 1) pieces.head
        else s"""[${pieces.map(p => s"($p)").mkString(", ")}].concat()"""
      ExprBlob(paramCodes.flatMap(_.prepares), result, paramCodes.flatMap(_.required).toSet)
  }

  // ---- helpers ---------------------------------------------------------------

  /** Ordered snake_case field names for a constructor's declared params. */
  private def classParamFieldNames(className: String): List[String] =
    analysis.classParamTypes.getOrElse(className, List()).map(p => rustFieldName(p._1))

  // ---- AST type definitions (structs / enums / AstNode trait) ----------------

  /**
   * Emit one `pub struct` per concrete class and one `pub enum` per sealed
   * (abstract) class. Mirrors the PoC under examples/generated/rust/asdl_ast.rs.
   *
   * Every concrete struct carries `node_id`, `start`, `end`. The `AstNode`
   * trait exposes those three; each concrete type gets a mechanical impl. We
   * deliberately do NOT model the Kotlin `AstNode` sealed interface as an enum
   * (it would be a giant variant set); a trait is the natural Rust shape.
   */
  def classDefs(): String = {
    val hierarchy = analysis.classRelations.toHierarchy
    hierarchy.allTypes.values.toList.sortBy(_.className).map(classDef).mkString("\n")
  }

  private def classDef(cls: ClassHierarchyItem): String = {
    if (cls.subclasses.isEmpty) {
      // Concrete type → struct + a to_short_string() inherent impl matching
      // KotlinOptCodeGen's toShortString() (payload fields only; no id/span).
      val params = analysis.classParamTypes.getOrElse(cls.className, List())
      val fields = params.map { case (pname, ptype) =>
        s"    pub ${rustFieldName(pname)}: ${rustType(ptype)},"
      }
      // Field label uses the original (non-snake) param name to match Kotlin.
      val fmtPieces = params.map { case (pname, _) => s"$pname={}" }
      val fmtArgs = params.map { case (pname, ptype) =>
        shortStringFieldExpr(s"self.${rustFieldName(pname)}", ptype)
      }
      val fmtStr = s"${cls.className}(${fmtPieces.mkString(", ")})"
      val toShort =
        if (params.isEmpty)
          s"""    pub fn to_short_string(&self) -> String {
             |        "${cls.className}()".to_string()
             |    }""".stripMargin
        else
          s"""    pub fn to_short_string(&self) -> String {
             |        format!("$fmtStr", ${fmtArgs.mkString(", ")})
             |    }""".stripMargin
      s"""#[derive(Debug, Clone)]
         |pub struct ${rustClassName(cls.className)} {
         |${fields.mkString("\n")}
         |    pub node_id: i32,
         |    pub start: i32,
         |    pub end: i32,
         |}
         |
         |impl ${rustClassName(cls.className)} {
         |$toShort
         |}
         |""".stripMargin
    } else {
      // Abstract type → enum over its (sorted) subclasses, each Boxed.
      // to_short_string dispatches to the active variant.
      val subs = cls.subclasses.toList.sorted
      val variants = subs.map(sub => s"    ${rustClassName(sub)}(Box<${rustClassName(sub)}>),")
      val arms = subs.map(sub => s"            ${rustClassName(cls.className)}::${rustClassName(sub)}(x) => x.to_short_string(),")
      s"""#[derive(Debug, Clone)]
         |pub enum ${rustClassName(cls.className)} {
         |${variants.mkString("\n")}
         |}
         |
         |impl ${rustClassName(cls.className)} {
         |    pub fn to_short_string(&self) -> String {
         |        match self {
         |${arms.mkString("\n")}
         |        }
         |    }
         |}
         |""".stripMargin
    }
  }

  def enumDefs(): String = {
    val enums = analysis.enumValuesMap
    if (enums.isEmpty) "" else {
      enums.keySet.toList.sorted.map { enumName =>
        val values = enums(enumName).toList.sorted
        val members = values.map(v => s"    $v,")
        // to_short_string prints the variant name, matching Kotlin's `$enumVal`
        // (an enum's toString is its name).
        val arms = values.map(v => s"            $enumName::$v => \"$v\",")
        s"""#[derive(Debug, Clone, Copy, PartialEq, Eq)]
           |pub enum $enumName {
           |${members.mkString("\n")}
           |}
           |
           |impl $enumName {
           |    pub fn to_short_string(&self) -> String {
           |        match self {
           |${arms.mkString("\n")}
           |        }.to_string()
           |    }
           |}
           |""".stripMargin
      }.mkString("\n")
    }
  }

  /**
   * Rust expression producing the short-string fragment for one field, matching
   * `KotlinOptCodeGen.toShortStringExpr`. The result is a `String`/`&str`-ish
   * value spliced into a `format!("...{}...")`, so every branch yields something
   * `Display`-able.
   */
  private def shortStringFieldExpr(accessor: String, t: Type): String = reduceType(t) match {
    case Type.ClassType(_) => s"$accessor.to_short_string()"
    case Type.OptionalOf(inner) => reduceType(inner) match {
      case Type.ClassType(_) =>
        // Kotlin: `x?.toShortString()` → "null" when absent.
        s"""$accessor.as_ref().map(|x| x.to_short_string()).unwrap_or_else(|| "null".to_string())"""
      case _: Type.EnumType | _: Type.UnspecifiedEnumType =>
        s"""$accessor.as_ref().map(|x| x.to_short_string()).unwrap_or_else(|| "null".to_string())"""
      case Type.ArrayOf(elemType) =>
        // Opt(Array(...)): "null" when absent, else the array's short form.
        // NOTE: KotlinOptCodeGen mishandles this case (it falls through to the
        // bare expr and leaks node toString); Rust does the correct thing, so
        // the diff harness must canonicalise the Kotlin side for such fields.
        s"""$accessor.as_ref().map(|v| ${arrayShortExpr("v", elemType)}).unwrap_or_else(|| "null".to_string())"""
      case _ =>
        // Opt(scalar): Kotlin prints the value or "null".
        s"""$accessor.as_ref().map(|x| format!("{}", x)).unwrap_or_else(|| "null".to_string())"""
    }
    case Type.ArrayOf(elemType) => arrayShortExpr(accessor, elemType)
    case _: Type.EnumType | _: Type.UnspecifiedEnumType =>
      s"$accessor.to_short_string()"
    case Type.CharType =>
      // Kotlin prints the char directly.
      s"$accessor"
    case _ =>
      // bool / string / node bytes: Display.
      s"$accessor"
  }

  /** Short-string for a `Vec<elemType>` bound to `accessor`. Matches Kotlin's `[a, b]`. */
  private def arrayShortExpr(accessor: String, elemType: Type): String = reduceType(elemType) match {
    case Type.ClassType(_) | (_: Type.EnumType) | (_: Type.UnspecifiedEnumType) =>
      s"""format!("[{}]", $accessor.iter().map(|it| it.to_short_string()).collect::<Vec<_>>().join(", "))"""
    case _ =>
      // Array(scalar): Kotlin's List.toString() = "[a, b, c]".
      s"""format!("[{}]", $accessor.iter().map(|it| format!("{}", it)).collect::<Vec<_>>().join(", "))"""
  }

  /** `impl AstNode for Foo` for every concrete class, plus the trait itself. */
  def astNodeTraitAndImpls(): String = {
    val hierarchy = analysis.classRelations.toHierarchy
    val concretes = hierarchy.allTypes.values.toList.filter(_.subclasses.isEmpty).map(_.className).sorted
    val traitDef =
      s"""pub trait AstNode {
         |    fn node_id(&self) -> i32;
         |    fn start(&self) -> i32;
         |    fn end(&self) -> i32;
         |}
         |""".stripMargin
    val impls = concretes.map { c0 =>
      val c = rustClassName(c0)
      s"""impl AstNode for $c {
         |    fn node_id(&self) -> i32 { self.node_id }
         |    fn start(&self) -> i32 { self.start }
         |    fn end(&self) -> i32 { self.end }
         |}
         |""".stripMargin
    }.mkString("\n")
    traitDef + "\n" + impls
  }

  // ---- assembly --------------------------------------------------------------

  /**
   * Emit the whole `ast.rs` module: AST types, the walk context + walk
   * functions. The proto encoder lives in a separate file
   * (see [[RustProtoEncoderGen]]) so the two concerns stay decoupled.
   *
   * Layout:
   *   - enum definitions
   *   - struct / sealed-enum definitions
   *   - AstNode trait + impls
   *   - `struct Ctx { source_chars, history, ids }` + `match_start`
   *   - free `match_x_` functions
   */
  def generate(): String = {
    val writer = new StringWriter()

    writer.write("// Generated by RustOptCodeGen — do not edit by hand.\n")
    writer.write("// AST walk mirrors KotlinOptCodeGen; see\n")
    writer.write("// mgroup3/docs/phase_b_proto_design.md.\n")
    // Some leaf match fns don't read end_gen (e.g. a single-char terminal).
    writer.write("#![allow(unused_variables)]\n\n")
    writer.write("use crate::ktlib::{\n")
    writer.write("    get_sequence_elems, has_single_true, unroll_repeat0, unroll_repeat1,\n")
    writer.write("    IdIssuer, KernelSet, KernelSetExt,\n")
    writer.write("};\n\n")

    writer.write(enumDefs())
    writer.write("\n")
    writer.write(classDefs())
    writer.write("\n")
    writer.write(astNodeTraitAndImpls())
    writer.write("\n")

    // Walk context. Holds the char view, history, and id issuer.
    writer.write(
      """pub struct Ctx<'a> {
        |    pub source_chars: &'a [char],
        |    pub history: &'a [KernelSet],
        |    pub ids: IdIssuer,
        |}
        |
        |impl<'a> Ctx<'a> {
        |    pub fn new(source_chars: &'a [char], history: &'a [KernelSet]) -> Self {
        |        Self { source_chars, history, ids: IdIssuer::new(0) }
        |    }
        |
        |    fn next_id(&mut self) -> i32 {
        |        self.ids.next_id()
        |    }
        |
        |""".stripMargin)

    // matchStart is a method on Ctx (it reads source length + start symbol).
    writer.write(matchStartFunc())
    writer.write("\n}\n\n")

    // Free match functions for each reachable nonterminal.
    var visitedNonterms = Set[String]()
    while ((_requiredNonterms -- visitedNonterms).nonEmpty) {
      val next = (_requiredNonterms -- visitedNonterms).head
      writer.write(nonterminalMatchFunc(next))
      writer.write("\n\n")
      visitedNonterms += next
    }

    writer.toString
  }

  // ===========================================================================
  // AST DELTA WALK GENERATOR (Stage 2, design lsp_result_boundary.md §6).
  //
  // Emits the grammar-specific half of `delta.rs`: NodeEntry-oneof dispatch
  // helpers + per-nonterminal delta functions. The grammar-INDEPENDENT runtime
  // (LazyHistory, DeltaCtx, try_reuse, delta_list, dfs_free, PrevResult,
  // reconstruct, the delta session + FFI) is the static DELTA_RT_RS emitted by
  // Stage4RustEmit; the two are concatenated into one `delta.rs` module.
  //
  // Mirrors the validated hand prototype:
  //   - class-producing nonterminal -> `delta_x_(ctx,b,e,old: Option<i32>) -> i32`
  //       (try_reuse guard; else rebuild children + push a NodeEntry, fresh id).
  //   - scalar-producing nonterminal -> `delta_x_(ctx,b,e) -> T` (plain walk over
  //       the lazy history; no node).
  //   - Arr(class) nonterminal -> `delta_x_coords(ctx,b,e) -> Vec<(i32,i32)>`
  //       (element coords; the caller wraps with delta_list against the old list).
  //
  // Sealed up-casts vanish in delta mode: a class value is an i32 id, and a
  // sealed-parent field just stores whatever concrete child's id (no enum wrap).
  //
  // Must run AFTER generate() (reuses `_requiredNonterms`, the reachable set).
  // ===========================================================================

  private def deltaConcretesSorted: List[String] =
    analysis.classRelations.toHierarchy.allTypes.values.toList
      .filter(_.subclasses.isEmpty).map(_.className).sorted

  private def deltaTag(className: String): String = "TAG_" + camelToSnake(className).toUpperCase

  private def deltaNodePath(className: String): String =
    s"proto::node_entry::Node::${rustClassName(className)}"

  private def deltaFn(nt: String): String = s"delta_${camelToSnake(nt)}_"

  /** Return type for a scalar delta fn. Same shape as `rustReturnType`, but AST
   * enum types are qualified `crate::ast::` (delta.rs is a sibling module, so
   * unqualified enum names are out of scope). */
  private def deltaReturnType(t: Type): String = reduceType(t) match {
    case Type.EnumType(name) => s"crate::ast::$name"
    case Type.UnspecifiedEnumType(uid) => s"crate::ast::${analysis.shortenedEnumTypesMap(uid)}"
    case Type.OptionalOf(inner) => s"Option<${deltaReturnType(inner)}>"
    case Type.ArrayOf(inner) => s"Vec<${deltaReturnType(inner)}>"
    case _ => rustReturnType(t)
  }

  private def deltaCoordsFn(nt: String): String = s"delta_${camelToSnake(nt)}_coords"

  private sealed trait DKind
  private case object DScalarK extends DKind
  private case object DClassK extends DKind
  private case object DOptClassK extends DKind
  private case class DArrClassK(elem: String) extends DKind

  private def dKind(t: Type): DKind = reduceType(t) match {
    case Type.ClassType(_) => DClassK
    case Type.OptionalOf(inner) if isMsg(reduceType(inner)) => DOptClassK
    case Type.ArrayOf(inner) => reduceType(inner) match {
      case Type.ClassType(name) => DArrClassK(name)
      case _ => DScalarK
    }
    case _ => DScalarK
  }

  def generateDelta(): String = {
    val sb = new StringBuilder
    sb.append(deltaDispatch())
    sb.append("\n")
    sb.append(deltaCanon())
    sb.append("\n")
    sb.append(deltaWalkEntry())
    sb.append("\n")
    var visited = Set[String]()
    while ((_requiredNonterms -- visited).nonEmpty) {
      val next = (_requiredNonterms -- visited).head
      sb.append(deltaNonterminalFunc(next))
      sb.append("\n\n")
      visited += next
    }
    sb.toString
  }

  // ---- canonical tree form for the differential oracle (grammar-specific) ----
  // Renders a node table (nodes + id->index) from a root into a string that
  // captures type / spans / scalar fields / child STRUCTURE but is id-value
  // independent (Msg children recurse; ids differ between the delta's session-id
  // space and a fresh full encode). Two tables are tree-equal iff canon matches.

  private def deltaCanon(): String = {
    val concretes = deltaConcretesSorted
    val arms = concretes.map { c =>
      val fields = analysis.classParamTypes.getOrElse(c, List()).map { case (pname, ptype) =>
        val f = rustFieldName(pname)
        reduceType(ptype) match {
          case Type.ClassType(_) =>
            s"""            out.push_str("$f="); canon_node(nodes, by_id, m.$f, out); out.push(',');"""
          case Type.OptionalOf(inner) if isMsg(reduceType(inner)) =>
            s"""            out.push_str("$f="); if m.${f}_present { canon_node(nodes, by_id, m.$f, out); } else { out.push_str("null"); } out.push(',');"""
          case Type.ArrayOf(elem) if isMsg(reduceType(elem)) =>
            s"""            out.push_str("$f="); canon_list(nodes, by_id, &m.$f, out); out.push(',');"""
          case Type.OptionalOf(Type.ArrayOf(elem)) if isMsg(reduceType(elem)) =>
            s"""            out.push_str("$f="); if m.${f}_present { canon_list(nodes, by_id, &m.$f, out); } else { out.push_str("null"); } out.push(',');"""
          case _ =>
            s"""            out.push_str(&format!("$f={:?},", m.$f));"""
        }
      }.mkString("\n")
      s"""        Some(${deltaNodePath(c)}(m)) => {
         |            out.push_str("${rustClassName(c)}[");
         |            out.push_str(&m.start.to_string()); out.push(','); out.push_str(&m.end.to_string());
         |            out.push_str("](");
         |$fields
         |            out.push(')');
         |        }""".stripMargin
    }.mkString("\n")
    s"""// ---- canonical tree form for the delta oracle ----------------------------
       |pub fn canon(nodes: &[proto::NodeEntry], by_id: &FxHashMap<i32, usize>, root: i32) -> String {
       |    let mut out = String::new();
       |    canon_node(nodes, by_id, root, &mut out);
       |    out
       |}
       |
       |fn canon_list(nodes: &[proto::NodeEntry], by_id: &FxHashMap<i32, usize>, ids: &[i32], out: &mut String) {
       |    out.push('[');
       |    for (i, &c) in ids.iter().enumerate() {
       |        if i > 0 { out.push(','); }
       |        canon_node(nodes, by_id, c, out);
       |    }
       |    out.push(']');
       |}
       |
       |fn canon_node(nodes: &[proto::NodeEntry], by_id: &FxHashMap<i32, usize>, id: i32, out: &mut String) {
       |    let Some(&idx) = by_id.get(&id) else { out.push_str("<DANGLING>"); return; };
       |    match nodes[idx].node.as_ref() {
       |$arms
       |        None => out.push_str("<EMPTY>"),
       |    }
       |}
       |""".stripMargin
  }

  // ---- NodeEntry-oneof dispatch helpers (grammar-specific) -------------------

  private def deltaDispatch(): String = {
    val concretes = deltaConcretesSorted
    val tags = concretes.zipWithIndex.map { case (c, i) =>
      s"const ${deltaTag(c)}: i32 = $i;"
    }.mkString("\n")
    val tagArms = concretes.map { c =>
      s"        Some(${deltaNodePath(c)}(_)) => ${deltaTag(c)},"
    }.mkString("\n")
    val spanArms = concretes.map { c =>
      s"        Some(${deltaNodePath(c)}(m)) => (m.start, m.end),"
    }.mkString("\n")
    val shiftArms = concretes.map { c =>
      s"        Some(${deltaNodePath(c)}(m)) => set(&mut m.start, &mut m.end),"
    }.mkString("\n")
    val childArms = concretes.map { c =>
      val pushes = analysis.classParamTypes.getOrElse(c, List()).flatMap { case (pname, ptype) =>
        val f = rustFieldName(pname)
        reduceType(ptype) match {
          case Type.ClassType(_) => Some(s"out.push(m.$f);")
          case Type.OptionalOf(inner) if isMsg(reduceType(inner)) => Some(s"if m.${f}_present { out.push(m.$f); }")
          case Type.ArrayOf(elem) if isMsg(reduceType(elem)) => Some(s"out.extend_from_slice(&m.$f);")
          case Type.OptionalOf(Type.ArrayOf(elem)) if isMsg(reduceType(elem)) =>
            Some(s"if m.${f}_present { out.extend_from_slice(&m.$f); }")
          case _ => None
        }
      }
      if (pushes.isEmpty) s"        Some(${deltaNodePath(c)}(_)) => {}"
      else s"        Some(${deltaNodePath(c)}(m)) => { ${pushes.mkString(" ")} }"
    }.mkString("\n")
    s"""// ---- NodeEntry oneof dispatch (grammar-specific) --------------------------
       |$tags
       |
       |fn tag_of(entry: &proto::NodeEntry) -> i32 {
       |    match entry.node.as_ref() {
       |$tagArms
       |        None => -1,
       |    }
       |}
       |
       |fn span_of(entry: &proto::NodeEntry) -> (i32, i32) {
       |    match entry.node.as_ref() {
       |$spanArms
       |        None => (0, 0),
       |    }
       |}
       |
       |fn shift_span(entry: &mut proto::NodeEntry, pivot: i32, delta: i32) {
       |    let set = |s: &mut i32, e: &mut i32| {
       |        if *s > pivot { *s += delta; }
       |        if *e > pivot { *e += delta; }
       |    };
       |    match entry.node.as_mut() {
       |$shiftArms
       |        None => {}
       |    }
       |}
       |
       |fn child_ids_of(entry: &proto::NodeEntry, out: &mut Vec<i32>) {
       |    match entry.node.as_ref() {
       |$childArms
       |        None => {}
       |    }
       |}
       |""".stripMargin
  }

  private def deltaWalkEntry(): String = {
    val startSymbol = analysis.ngrammar
      .symbolOf(analysis.ngrammar.nsymbols(analysis.ngrammar.startSymbol).asInstanceOf[NStart].produce)
      .asInstanceOf[NNonterminal]
    val startFn = deltaFn(startSymbol.symbol.name)
    s"""// ---- delta walk entry ----------------------------------------------------
       |pub fn walk_delta(
       |    source_chars: &[char],
       |    query: KernelsQuery,
       |    reuse: ReuseInfo,
       |    old_nodes: &[proto::NodeEntry],
       |    by_id: &FxHashMap<i32, usize>,
       |    old_root: i32,
       |    next_id_start: i32,
       |) -> DeltaResult {
       |    let mut ctx = DeltaCtx {
       |        source_chars,
       |        hist: LazyHistory::new(query),
       |        dirty_lo: reuse.dirty_lo,
       |        dirty_hi: reuse.dirty_hi,
       |        delta: reuse.delta,
       |        old_nodes,
       |        by_id,
       |        patched: Vec::new(),
       |        kept: FxHashSet::default(),
       |        next_id: next_id_start,
       |    };
       |    let last_gen = ctx.source_chars.len() as i32;
       |    let kernel = ctx.hist.at(last_gen as usize).get_single(${startSymbol.id}, 1, 0, last_gen);
       |    let root_id = $startFn(&mut ctx, kernel.begin_gen, kernel.end_gen, Some(old_root));
       |    let mut freed = Vec::new();
       |    dfs_free(old_root, &ctx.kept, by_id, old_nodes, &mut freed);
       |    DeltaResult {
       |        patched: ctx.patched,
       |        freed,
       |        root_id,
       |        shift_pivot: reuse.pivot,
       |        shift_delta: reuse.delta,
       |    }
       |}
       |""".stripMargin
  }

  // ---- per-nonterminal delta functions ---------------------------------------

  private def deltaNonterminalFunc(nt: String): String = {
    varId = 0
    val ve = analysis.nonterminalValuefyExprs(nt)
    val t = analysis.nonterminalTypes(nt)
    dKind(t) match {
      case DClassK =>
        val body = deltaClassChoices(ve.choices, "begin_gen", "end_gen", "old")
        val lines = (body.prepares :+ body.result).map("    " + _).mkString("\n")
        s"""fn ${deltaFn(nt)}(ctx: &mut DeltaCtx, begin_gen: i32, end_gen: i32, old: Option<i32>) -> i32 {
           |$lines
           |}""".stripMargin
      case DScalarK =>
        val body = deltaScalarChoices(ve.choices, "begin_gen", "end_gen")
        val ret = deltaReturnType(t)
        val lines = (body.prepares :+ body.result).map("    " + _).mkString("\n")
        s"""fn ${deltaFn(nt)}(ctx: &mut DeltaCtx, begin_gen: i32, end_gen: i32) -> $ret {
           |$lines
           |}""".stripMargin
      case DOptClassK =>
        // Nonterminal whose type is Option<class>: delta fn returns Option<i32>
        // (the present node's id, or None).
        val body = deltaOptClassChoices(ve.choices, "begin_gen", "end_gen", "old")
        val lines = (body.prepares :+ body.result).map("    " + _).mkString("\n")
        s"""fn ${deltaFn(nt)}(ctx: &mut DeltaCtx, begin_gen: i32, end_gen: i32, old: Option<i32>) -> Option<i32> {
           |$lines
           |}""".stripMargin
      case DArrClassK(_) =>
        val body = deltaCoordsChoices(ve.choices, "begin_gen", "end_gen")
        val lines = (body.prepares :+ body.result).map("    " + _).mkString("\n")
        s"""fn ${deltaCoordsFn(nt)}(ctx: &mut DeltaCtx, begin_gen: i32, end_gen: i32) -> Vec<(i32, i32)> {
           |$lines
           |}""".stripMargin
    }
  }

  // ---- choice dispatch -------------------------------------------------------

  private def deltaChoiceSelect(
    choicesMap: Map[Symbols.Symbol, ValuefyExpr],
    beginGen: String,
    endGen: String,
  ): (List[String], List[(String, Symbols.Symbol)]) = {
    val choiceSymbols = choicesMap.keys.toList.sortBy(analysis.ngrammar.idOf)
    val choiceVars = choiceSymbols.map(_ => newVar())
    val tryCodes = choiceVars.zip(choiceSymbols).map { case (v, sym) =>
      val symbolId = analysis.ngrammar.idOf(sym)
      val lastPointer = analysis.ngrammar.lastPointerOf(symbolId)
      s"let $v = ctx.hist.at($endGen as usize).find_by_begin_gen_opt($symbolId, $lastPointer, $beginGen);"
    }
    val assertCode = s"assert!(has_single_true(&[${choiceVars.map(_ + ".is_some()").mkString(", ")}]));"
    (tryCodes :+ assertCode, choiceVars.zip(choiceSymbols))
  }

  private def deltaIfChain(arms: List[(String, String)]): String =
    arms.zipWithIndex.map { case ((cond, block), idx) =>
      if (idx == 0) s"if $cond.is_some() { $block }"
      else if (cond.nonEmpty) s"else if $cond.is_some() { $block }"
      else s"else { $block }"
    }.mkString(" ")

  private def deltaClassChoices(
    choicesMap: Map[Symbols.Symbol, ValuefyExpr],
    beginGen: String, endGen: String, oldVar: String,
  ): ExprBlob = {
    if (choicesMap.size == 1) {
      val (sym, expr) = choicesMap.head
      deltaClassExpr(expr, beginGen, endGen, sym, SequenceVarName(None), oldVar)
    } else {
      val (sel, choices) = deltaChoiceSelect(choicesMap, beginGen, endGen)
      val armExprs = choices.zipWithIndex.map { case ((v, sym), idx) =>
        val armBlob = deltaClassExpr(choicesMap(sym), beginGen, endGen, sym, SequenceVarName(None), oldVar)
        ((if (idx == choices.size - 1) "" else v), armBlob.asBlockExpr)
      }
      val rv = newVar()
      ExprBlob(sel :+ s"let $rv = ${deltaIfChain(armExprs)};", rv, Set())
    }
  }

  private def deltaScalarChoices(
    choicesMap: Map[Symbols.Symbol, ValuefyExpr], beginGen: String, endGen: String,
  ): ExprBlob = {
    if (choicesMap.size == 1) {
      val (sym, expr) = choicesMap.head
      deltaScalarExpr(expr, beginGen, endGen, sym, SequenceVarName(None))
    } else {
      // Metalang lowers `X?` to a two-arm choice (empty -> null, present -> X).
      // In scalar mode the arms must unify to `Option<X>`: wrap each non-null arm
      // in `Some(..)`. (In the plain generator `coerceBranch` does this.)
      val hasNull = choicesMap.values.exists(_ == ValuefyExpr.NullLiteral)
      val (sel, choices) = deltaChoiceSelect(choicesMap, beginGen, endGen)
      val armExprs = choices.zipWithIndex.map { case ((v, sym), idx) =>
        val armBlob = deltaScalarExpr(choicesMap(sym), beginGen, endGen, sym, SequenceVarName(None))
        val armExpr =
          if (hasNull && choicesMap(sym) != ValuefyExpr.NullLiteral) s"Some(${armBlob.asBlockExpr})"
          else armBlob.asBlockExpr
        ((if (idx == choices.size - 1) "" else v), armExpr)
      }
      val rv = newVar()
      ExprBlob(sel :+ s"let $rv = ${deltaIfChain(armExprs)};", rv, Set())
    }
  }

  private def deltaCoordsChoices(
    choicesMap: Map[Symbols.Symbol, ValuefyExpr], beginGen: String, endGen: String,
  ): ExprBlob = {
    if (choicesMap.size == 1) {
      val (sym, expr) = choicesMap.head
      deltaCoordsExpr(expr, beginGen, endGen, sym, SequenceVarName(None))
    } else {
      val (sel, choices) = deltaChoiceSelect(choicesMap, beginGen, endGen)
      val armExprs = choices.zipWithIndex.map { case ((v, sym), idx) =>
        val armBlob = deltaCoordsExpr(choicesMap(sym), beginGen, endGen, sym, SequenceVarName(None))
        ((if (idx == choices.size - 1) "" else v), armBlob.asBlockExpr)
      }
      val rv = newVar()
      ExprBlob(sel :+ s"let $rv = ${deltaIfChain(armExprs)};", rv, Set())
    }
  }

  /** Body of an Option<class>-typed nonterminal (result = Option<i32>): the
   * two-arm empty/present choice, empty -> None, present -> Some(id). */
  private def deltaOptClassChoices(
    choicesMap: Map[Symbols.Symbol, ValuefyExpr], beginGen: String, endGen: String, oldVar: String,
  ): ExprBlob = {
    if (choicesMap.size == 1) {
      val (sym, expr) = choicesMap.head
      if (expr == ValuefyExpr.NullLiteral) ExprBlob.code("None")
      else {
        val b = deltaClassExpr(expr, beginGen, endGen, sym, SequenceVarName(None), oldVar)
        ExprBlob(b.prepares, s"Some(${b.result})", b.required)
      }
    } else {
      val (sel, choices) = deltaChoiceSelect(choicesMap, beginGen, endGen)
      val nullChoice = choices.find { case (_, s) => choicesMap(s) == ValuefyExpr.NullLiteral }
        .getOrElse(throw new Exception("Option<class> nonterminal has no null arm"))
      val valChoice = choices.find { case (_, s) => choicesMap(s) != ValuefyExpr.NullLiteral }
        .getOrElse(throw new Exception("Option<class> nonterminal has no value arm"))
      val valBlob = deltaClassExpr(choicesMap(valChoice._2), beginGen, endGen, valChoice._2, SequenceVarName(None), oldVar)
      val rv = newVar()
      ExprBlob(sel :+ s"let $rv: Option<i32> = if ${nullChoice._1}.is_some() { None } else { Some(${valBlob.asBlockExpr}) };", rv, Set())
    }
  }

  // ---- class-producing expressions (result = i32 node id) --------------------

  private def deltaClassExpr(
    ve: ValuefyExpr, beginGen: String, endGen: String,
    symbol: Symbols.Symbol, seqVar: SequenceVarName, oldVar: String,
  ): ExprBlob = ve match {
    case ValuefyExpr.MatchNonterminal(name) =>
      _requiredNonterms += name
      val v = newVar()
      ExprBlob(List(s"let $v = ${deltaFn(name)}(ctx, $beginGen, $endGen, $oldVar);"), v, Set())
    case ValuefyExpr.Unbind(sym, e) => deltaClassExpr(e, beginGen, endGen, sym, seqVar, oldVar)
    case ValuefyExpr.JoinBody(bp) =>
      deltaClassExpr(bp, beginGen, endGen, symbol.asInstanceOf[Symbols.Join].sym, SequenceVarName(None), oldVar)
    case ValuefyExpr.SeqElemAt(index, e) =>
      val sequenceId = analysis.ngrammar.idOf(symbol)
      val sequence = analysis.ngrammar.nsequences(sequenceId)
      seqVar.name match {
        case Some(sv) =>
          deltaClassExpr(e, s"$sv[$index].0", s"$sv[$index].1", sequence.symbol.seq(index), seqVar, oldVar)
        case None =>
          val sv = newVar()
          val getSeq = s"let $sv = get_sequence_elems_lazy(&ctx.hist, $sequenceId, &[${sequence.sequence.mkString(", ")}], $beginGen, $endGen);"
          seqVar.name = Some(sv)
          val inner = deltaClassExpr(e, s"$sv[$index].0", s"$sv[$index].1", sequence.symbol.seq(index), seqVar, oldVar)
          ExprBlob(getSeq +: inner.prepares, inner.result, inner.required)
      }
    case ValuefyExpr.UnrollChoices(choices) => deltaClassChoices(choices, beginGen, endGen, oldVar)
    case ValuefyExpr.ConstructCall(className, params) =>
      deltaConstruct(className, params, beginGen, endGen, symbol, seqVar, oldVar)
    case ValuefyExpr.TernaryOp(cond, ifTrue, ifFalse) =>
      val c = deltaScalarExpr(cond, beginGen, endGen, symbol, seqVar)
      val tb = deltaClassExpr(ifTrue, beginGen, endGen, symbol, seqVar, oldVar)
      val fb = deltaClassExpr(ifFalse, beginGen, endGen, symbol, seqVar, oldVar)
      val rv = newVar()
      ExprBlob(c.prepares :+ s"let $rv = if ${c.result} { ${tb.asBlockExpr} } else { ${fb.asBlockExpr} };", rv, Set())
    case other =>
      throw new Exception(s"deltaClassExpr: unsupported ${other.getClass.getSimpleName}")
  }

  private def deltaConstruct(
    className: String, params: List[ValuefyExpr],
    beginGen: String, endGen: String, symbol: Symbols.Symbol, seqVar: SequenceVarName, oldVar: String,
  ): ExprBlob = {
    val declParams = analysis.classParamTypes.getOrElse(className, List())
    check(declParams.size == params.size,
      s"delta param count mismatch for $className: ${declParams.size} vs ${params.size}")
    val fieldResults = declParams.zip(params).map { case ((pname, ftype), pe) =>
      deltaField(className, pname, ftype, pe, beginGen, endGen, symbol, seqVar)
    }
    val prepares = fieldResults.flatMap(_._1)
    val assigns = fieldResults.flatMap(_._2) ++ List(s"start: $beginGen", s"end: $endGen")
    val rebuild =
      s"{ ${prepares.mkString(" ")} let id = ctx.alloc(); ctx.patched.push(proto::NodeEntry { id, node: Some(${deltaNodePath(className)}(proto::${rustClassName(className)} { ${assigns.mkString(", ")} })) }); id }"
    val result =
      s"{ let node_old = $oldVar; if let Some(reused) = ctx.try_reuse(${deltaTag(className)}, $beginGen, $endGen, node_old) { reused } else $rebuild }"
    ExprBlob(List(), result, Set())
  }

  private def oldFieldExtract(className: String, field: String, extractSome: String, none: String, guard: String): String = {
    val g = if (guard.isEmpty) "" else s" if $guard"
    s"match ctx.old_entry(node_old).and_then(|e| e.node.as_ref()) { Some(${deltaNodePath(className)}(m))$g => $extractSome, _ => $none }"
  }

  private def deltaField(
    className: String, pname: String, ftype: Type, pe: ValuefyExpr,
    beginGen: String, endGen: String, symbol: Symbols.Symbol, seqVar: SequenceVarName,
  ): (List[String], List[String]) = {
    val f = rustFieldName(pname)
    reduceType(ftype) match {
      case Type.ClassType(_) =>
        val oldF = s"let ${f}_old: Option<i32> = ${oldFieldExtract(className, f, s"Some(m.$f)", "None", "")};"
        val blob = deltaClassExpr(pe, beginGen, endGen, symbol, seqVar, s"${f}_old")
        (List(oldF) ++ blob.prepares :+ s"let ${f}_id = ${blob.result};", List(s"$f: ${f}_id"))
      case Type.OptionalOf(inner) if isMsg(reduceType(inner)) =>
        val oldF = s"let ${f}_old: Option<i32> = ${oldFieldExtract(className, f, s"Some(m.$f)", "None", s"m.${f}_present")};"
        val (p, presentV, idV) = deltaOptClassField(pe, beginGen, endGen, symbol, seqVar, s"${f}_old")
        (List(oldF) ++ p, List(s"${f}_present: $presentV", s"$f: $idV"))
      case Type.ArrayOf(elem) if isMsg(reduceType(elem)) =>
        val elemClass = classNameOf(elem).get
        val oldF = s"let ${f}_old: Vec<i32> = ${oldFieldExtract(className, f, s"m.$f.clone()", "Vec::new()", "")};"
        val coords = deltaCoordsExpr(pe, beginGen, endGen, symbol, seqVar)
        val rec = elemRecurseArg(pe, symbol, elemClass)
        (List(oldF) ++ coords.prepares :+ s"let $f = delta_list(ctx, &${coords.result}, &${f}_old, $rec);",
          List(s"$f: $f"))
      case Type.OptionalOf(Type.ArrayOf(elem)) if isMsg(reduceType(elem)) =>
        val elemClass = classNameOf(elem).get
        val oldF = s"let ${f}_old: Vec<i32> = ${oldFieldExtract(className, f, s"m.$f.clone()", "Vec::new()", s"m.${f}_present")};"
        val (p, presentV, coordsV) = deltaOptCoordsField(pe, beginGen, endGen, symbol, seqVar)
        val rec = elemRecurseArg(pe, symbol, elemClass)
        (List(oldF) ++ p :+ s"let $f: Vec<i32> = if $presentV { delta_list(ctx, &$coordsV, &${f}_old, $rec) } else { Vec::new() };",
          List(s"${f}_present: $presentV", s"$f: $f"))
      case _ =>
        val blob0 = deltaScalarExpr(pe, beginGen, endGen, symbol, seqVar)
        // Coerce a bare value flowing into an `Opt` field to `Some(..)` (the plain
        // generator's `coerce(.., FieldOf(Opt(..)))` does this); `scalarProtoField`
        // then destructures the `Option`.
        val blob = (reduceType(ftype), reduceType(typeOf(pe))) match {
          case (Type.OptionalOf(_), Type.OptionalOf(_) | Type.NullType) => blob0
          case (Type.OptionalOf(_), _) => blob0.copy(result = s"Some(${blob0.result})")
          case _ => blob0
        }
        scalarProtoField(f, ftype, blob)
    }
  }

  /** The delta_list recurse argument for a repeated-Msg field of element class
   * `elemClass`: a plain `delta_x_` fn item when the element is produced by a
   * same-named nonterminal, else an inline closure rebuilding the element (an
   * inline ConstructCall / sealed choice that has no standalone delta fn). */
  private def elemRecurseArg(pe: ValuefyExpr, symbol: Symbols.Symbol, elemClass: String): String =
    findElemCore(pe, symbol) match {
      case Some((core, coreSym)) =>
        // Always a closure over the element's class-producing core (a nonterminal
        // match, an inline construct, or a sealed choice) — this uses the correct
        // producing nonterminal, which may differ in name from `elemClass`.
        val blob = deltaClassExpr(core, "b", "e", coreSym, SequenceVarName(None), "old")
        s"|ctx: &mut DeltaCtx, b: i32, e: i32, old: Option<i32>| ${blob.asBlockExpr}"
      case None => deltaFn(elemClass)
    }

  /** Find the representative element's class-producing "core" expr (an inline
   * ConstructCall / sealed UnrollChoices) inside a repeated-Msg field expr, or
   * `None` when the element is produced by a nonterminal match (use `delta_x_`).
   * Skips `Opt` wrappers (a two-arm choice whose other arm is null) and digs
   * through sequence / concat / repeat / array structure. */
  private def findElemCore(e: ValuefyExpr, sym: Symbols.Symbol): Option[(ValuefyExpr, Symbols.Symbol)] = e match {
    case ValuefyExpr.Unbind(s, inner) => findElemCore(inner, s)
    case ValuefyExpr.SeqElemAt(idx, inner) =>
      val seq = analysis.ngrammar.nsequences(analysis.ngrammar.idOf(sym))
      findElemCore(inner, seq.symbol.seq(idx))
    case ValuefyExpr.BinOp(ValuefyExpr.BinOpType.ADD, l, r) =>
      findElemCore(l, sym).orElse(findElemCore(r, sym))
    case ValuefyExpr.ArrayExpr(elems) =>
      elems.iterator.flatMap(x => findElemCore(x, sym)).nextOption()
    case ValuefyExpr.UnrollRepeatFromZero(ep) => elemCoreOfRepeat(sym, ep)
    case ValuefyExpr.UnrollRepeatFromZeroNoUnbind(_, ep) => elemCoreOfRepeat(sym, ep)
    case ValuefyExpr.UnrollRepeatFromOne(ep) => elemCoreOfRepeat(sym, ep)
    case ValuefyExpr.UnrollRepeatFromOneNoUnbind(_, ep) => elemCoreOfRepeat(sym, ep)
    case ValuefyExpr.UnrollChoices(choices) =>
      if (choices.values.exists(_ == ValuefyExpr.NullLiteral)) {
        // Opt wrapper — dig into the non-null value arm.
        choices.collectFirst { case (s, ve) if ve != ValuefyExpr.NullLiteral => findElemCore(ve, s) }.flatten
      } else {
        Some((e, sym)) // genuine sealed inline element (no standalone delta fn)
      }
    case ValuefyExpr.ConstructCall(_, _) => Some((e, sym))
    case ValuefyExpr.MatchNonterminal(nt) =>
      // A single-class match is the element core (closure over its producing
      // nonterminal, whose name may differ from `elemClass`). An Arr/Opt/scalar
      // nonterminal is NOT the element (its elements come from inside it) — fall
      // back to `delta_<elemClass>_`.
      dKind(analysis.nonterminalTypes(nt)) match {
        case DClassK => Some((e, sym))
        case _ => None
      }
    case _ => None
  }

  private def elemCoreOfRepeat(sym: Symbols.Symbol, ep: ValuefyExpr): Option[(ValuefyExpr, Symbols.Symbol)] = {
    val repeat = analysis.ngrammar.symbolOf(analysis.ngrammar.idOf(sym)).asInstanceOf[NRepeat]
    findElemCore(ep, repeat.symbol.sym)
  }

  /** Strip `Unbind`/`SeqElemAt` (relocating begin/end via `seqVar`) down to the
   * opt "core" — a `MatchNonterminal` (nt is itself `Option<..>`-typed) or an
   * empty/present `UnrollChoices`. Returns (core, coreSym, resolvedBegin,
   * resolvedEnd, prepares). */
  private def resolveOptCore(
    pe: ValuefyExpr, beginGen: String, endGen: String, symbol: Symbols.Symbol, seqVar: SequenceVarName,
  ): (ValuefyExpr, Symbols.Symbol, String, String, List[String]) = pe match {
    case ValuefyExpr.Unbind(s, inner) => resolveOptCore(inner, beginGen, endGen, s, seqVar)
    case ValuefyExpr.SeqElemAt(idx, inner) =>
      val sequenceId = analysis.ngrammar.idOf(symbol)
      val sequence = analysis.ngrammar.nsequences(sequenceId)
      seqVar.name match {
        case Some(sv) =>
          resolveOptCore(inner, s"$sv[$idx].0", s"$sv[$idx].1", sequence.symbol.seq(idx), SequenceVarName(None))
        case None =>
          val sv = newVar()
          val getSeq = s"let $sv = get_sequence_elems_lazy(&ctx.hist, $sequenceId, &[${sequence.sequence.mkString(", ")}], $beginGen, $endGen);"
          seqVar.name = Some(sv)
          val (c, cs, rbg, reg, sel) = resolveOptCore(inner, s"$sv[$idx].0", s"$sv[$idx].1", sequence.symbol.seq(idx), SequenceVarName(None))
          (c, cs, rbg, reg, getSeq +: sel)
      }
    case _ => (pe, symbol, beginGen, endGen, List())
  }

  private def deltaOptClassField(
    pe: ValuefyExpr, beginGen: String, endGen: String, symbol: Symbols.Symbol,
    seqVar: SequenceVarName, oldVar: String,
  ): (List[String], String, String) = {
    val (core, coreSym, rbg, reg, sel) = resolveOptCore(pe, beginGen, endGen, symbol, seqVar)
    val tuple = optTuple(core, coreSym, rbg, reg, oldVar)
    val presentV = newVar()
    val idV = newVar()
    (sel ++ tuple.prepares :+ s"let (${presentV}, ${idV}): (bool, i32) = ${tuple.result};", presentV, idV)
  }

  /** A `(bool, i32)` (present, id) tuple expr for an Option<class> field value's
   * (SeqElemAt-resolved) core. Handles null, a direct class/opt-class nonterminal
   * match, an empty/present choice, and a ternary. */
  private def optTuple(
    core: ValuefyExpr, coreSym: Symbols.Symbol, rbg: String, reg: String, oldVar: String,
  ): ExprBlob = core match {
    case ValuefyExpr.NullLiteral => ExprBlob.code("(false, 0i32)")
    case ValuefyExpr.Unbind(s, inner) => optTuple(inner, s, rbg, reg, oldVar)
    case ValuefyExpr.MatchNonterminal(nt) =>
      _requiredNonterms += nt
      dKind(analysis.nonterminalTypes(nt)) match {
        case DOptClassK =>
          val v = newVar()
          ExprBlob(List(s"let $v = ${deltaFn(nt)}(ctx, $rbg, $reg, $oldVar);"),
            s"match $v { Some(x) => (true, x), None => (false, 0i32) }", Set())
        case DClassK =>
          val v = newVar()
          ExprBlob(List(s"let $v = ${deltaFn(nt)}(ctx, $rbg, $reg, $oldVar);"), s"(true, $v)", Set())
        case k => throw new Exception(s"optTuple MatchNonterminal $nt has kind $k")
      }
    case ValuefyExpr.UnrollChoices(choices) =>
      val (sel, choiceList) = deltaChoiceSelect(choices, rbg, reg)
      val nullChoice = choiceList.find { case (_, s) => choices(s) == ValuefyExpr.NullLiteral }
        .getOrElse(throw new Exception("optional element has no null arm"))
      val valChoice = choiceList.find { case (_, s) => choices(s) != ValuefyExpr.NullLiteral }
        .getOrElse(throw new Exception("optional element has no value arm"))
      val valBlob = deltaClassExpr(choices(valChoice._2), rbg, reg, valChoice._2, SequenceVarName(None), oldVar)
      ExprBlob(sel, s"if ${nullChoice._1}.is_some() { (false, 0i32) } else { (true, ${valBlob.asBlockExpr}) }", Set())
    case ValuefyExpr.TernaryOp(cond, ifTrue, ifFalse) =>
      val c = deltaScalarExpr(cond, rbg, reg, coreSym, SequenceVarName(None))
      def branch(e: ValuefyExpr): String = {
        val (bc, bs, bbg, beg, bsel) = resolveOptCore(e, rbg, reg, coreSym, SequenceVarName(None))
        val bt = optTuple(bc, bs, bbg, beg, oldVar)
        s"{ ${(bsel ++ bt.prepares).mkString(" ")} ${bt.result} }"
      }
      ExprBlob(c.prepares, s"if ${c.result} { ${branch(ifTrue)} } else { ${branch(ifFalse)} }", Set())
    case other => throw new Exception(s"optTuple: unsupported core ${other.getClass.getSimpleName}")
  }

  private def deltaOptCoordsField(
    pe: ValuefyExpr, beginGen: String, endGen: String, symbol: Symbols.Symbol, seqVar: SequenceVarName,
  ): (List[String], String, String) = {
    val a = optArms(pe, beginGen, endGen, symbol, seqVar)
    val presentV = newVar()
    val coordsV = newVar()
    val valBlob = deltaCoordsExpr(a.valExpr, a.rbg, a.reg, a.valSym, SequenceVarName(None))
    val body =
      s"let (${presentV}, ${coordsV}): (bool, Vec<(i32, i32)>) = if ${a.nullVar}.is_some() { (false, Vec::new()) } else { (true, ${valBlob.asBlockExpr}) };"
    (a.sel :+ body, presentV, coordsV)
  }

  /** Decomposed optional element: the null-arm choice var, the value-arm
   * symbol+expr, the RESOLVED begin/end (after any SeqElemAt relocation) the
   * value arm evaluates at, and the selection prepares. */
  private case class OptArms(
    nullVar: String,
    valSym: Symbols.Symbol,
    valExpr: ValuefyExpr,
    rbg: String,
    reg: String,
    sel: List[String],
  )

  private def optArms(
    pe: ValuefyExpr, beginGen: String, endGen: String, symbol: Symbols.Symbol, seqVar: SequenceVarName,
  ): OptArms = pe match {
    case ValuefyExpr.Unbind(sym, inner) => optArms(inner, beginGen, endGen, sym, seqVar)
    case ValuefyExpr.SeqElemAt(index, inner) =>
      val sequenceId = analysis.ngrammar.idOf(symbol)
      val sequence = analysis.ngrammar.nsequences(sequenceId)
      seqVar.name match {
        case Some(sv) =>
          optArms(inner, s"$sv[$index].0", s"$sv[$index].1", sequence.symbol.seq(index), SequenceVarName(None))
        case None =>
          val sv = newVar()
          val getSeq = s"let $sv = get_sequence_elems_lazy(&ctx.hist, $sequenceId, &[${sequence.sequence.mkString(", ")}], $beginGen, $endGen);"
          seqVar.name = Some(sv)
          val a = optArms(inner, s"$sv[$index].0", s"$sv[$index].1", sequence.symbol.seq(index), SequenceVarName(None))
          a.copy(sel = getSeq +: a.sel)
      }
    case ValuefyExpr.UnrollChoices(choices) =>
      val (sel, choiceList) = deltaChoiceSelect(choices, beginGen, endGen)
      val nullChoice = choiceList.find { case (_, sym) => choices(sym) == ValuefyExpr.NullLiteral }
        .getOrElse(throw new Exception("optional element has no null arm"))
      val valChoice = choiceList.find { case (_, sym) => choices(sym) != ValuefyExpr.NullLiteral }
        .getOrElse(throw new Exception("optional element has no value arm"))
      OptArms(nullChoice._1, valChoice._2, choices(valChoice._2), beginGen, endGen, sel)
    case other => throw new Exception(s"optArms: unsupported ${other.getClass.getSimpleName}")
  }

  // ---- coords-producing expressions (result = Vec<(i32,i32)>) ----------------

  private def deltaCoordsExpr(
    ve: ValuefyExpr, beginGen: String, endGen: String, symbol: Symbols.Symbol, seqVar: SequenceVarName,
  ): ExprBlob = ve match {
    case ValuefyExpr.Unbind(sym, e) => deltaCoordsExpr(e, beginGen, endGen, sym, seqVar)
    case ValuefyExpr.MatchNonterminal(name) =>
      _requiredNonterms += name
      val v = newVar()
      ExprBlob(List(s"let $v = ${deltaCoordsFn(name)}(ctx, $beginGen, $endGen);"), v, Set())
    case ValuefyExpr.SeqElemAt(index, e) =>
      val sequenceId = analysis.ngrammar.idOf(symbol)
      val sequence = analysis.ngrammar.nsequences(sequenceId)
      seqVar.name match {
        case Some(sv) => deltaCoordsExpr(e, s"$sv[$index].0", s"$sv[$index].1", sequence.symbol.seq(index), seqVar)
        case None =>
          val sv = newVar()
          val getSeq = s"let $sv = get_sequence_elems_lazy(&ctx.hist, $sequenceId, &[${sequence.sequence.mkString(", ")}], $beginGen, $endGen);"
          seqVar.name = Some(sv)
          val inner = deltaCoordsExpr(e, s"$sv[$index].0", s"$sv[$index].1", sequence.symbol.seq(index), seqVar)
          ExprBlob(getSeq +: inner.prepares, inner.result, inner.required)
      }
    case ValuefyExpr.BinOp(ValuefyExpr.BinOpType.ADD, lhs, rhs) =>
      val l = deltaCoordsExpr(lhs, beginGen, endGen, symbol, seqVar)
      val r = deltaCoordsExpr(rhs, beginGen, endGen, symbol, seqVar)
      val rv = newVar()
      ExprBlob(l.prepares ++ r.prepares :+ s"let mut $rv = ${l.result}; $rv.extend(${r.result});", rv, Set())
    case ValuefyExpr.ArrayExpr(elems) =>
      val rv = newVar()
      val elemExprs = elems.map(e => deltaElemCoord(e, beginGen, endGen, symbol, seqVar))
      ExprBlob(elemExprs.flatMap(_.prepares) :+ s"let $rv: Vec<(i32, i32)> = vec![${elemExprs.map(_.result).mkString(", ")}];", rv, Set())
    case ValuefyExpr.UnrollRepeatFromZero(ep) => deltaUnrollCoords("unroll_repeat0_lazy", ep, beginGen, endGen, symbol)
    case ValuefyExpr.UnrollRepeatFromZeroNoUnbind(_, ep) => deltaUnrollCoords("unroll_repeat0_lazy", ep, beginGen, endGen, symbol)
    case ValuefyExpr.UnrollRepeatFromOne(ep) => deltaUnrollCoords("unroll_repeat1_lazy", ep, beginGen, endGen, symbol)
    case ValuefyExpr.UnrollRepeatFromOneNoUnbind(_, ep) => deltaUnrollCoords("unroll_repeat1_lazy", ep, beginGen, endGen, symbol)
    case ValuefyExpr.UnrollChoices(choices) => deltaCoordsChoices(choices, beginGen, endGen)
    case ValuefyExpr.NullLiteral => ExprBlob.code("Vec::<(i32, i32)>::new()")
    case ValuefyExpr.ElvisOp(expr, ifNull) =>
      // `optArr ?: default` — an optional array with a default (usually `[]`).
      // Resolve the optional to its present/value arms; absent -> default coords.
      val a = optArms(expr, beginGen, endGen, symbol, seqVar)
      val valBlob = deltaCoordsExpr(a.valExpr, a.rbg, a.reg, a.valSym, SequenceVarName(None))
      val ifNullBlob = deltaCoordsExpr(ifNull, beginGen, endGen, symbol, seqVar)
      val rv = newVar()
      ExprBlob(a.sel ++ ifNullBlob.prepares :+
        s"let $rv: Vec<(i32, i32)> = if ${a.nullVar}.is_some() { ${ifNullBlob.result} } else { ${valBlob.asBlockExpr} };", rv, Set())
    case other => throw new Exception(s"deltaCoordsExpr: unsupported ${other.getClass.getSimpleName}")
  }

  private def deltaUnrollCoords(
    helperFn: String, elemProcessor: ValuefyExpr, beginGen: String, endGen: String, symbol: Symbols.Symbol,
  ): ExprBlob = {
    val v = newVar()
    val symbolId = analysis.ngrammar.idOf(symbol)
    val repeat = analysis.ngrammar.symbolOf(symbolId).asInstanceOf[NRepeat]
    val itemSymId = analysis.ngrammar.idOf(repeat.symbol.sym)
    val coord = deltaElemCoord(elemProcessor, "k.0", "k.1", repeat.symbol.sym, SequenceVarName(None))
    val closureBody = if (coord.prepares.isEmpty) coord.result else s"${coord.prepares.mkString(" ")} ${coord.result}"
    ExprBlob(
      List(
        s"let $v: Vec<(i32, i32)> = $helperFn(&ctx.hist, $symbolId, $itemSymId, ${repeat.baseSeq}, ${repeat.repeatSeq}, $beginGen, $endGen)",
        s"    .into_iter().map(|k| { $closureBody }).collect();"),
      v, Set())
  }

  private def deltaElemCoord(
    ve: ValuefyExpr, beginGen: String, endGen: String, symbol: Symbols.Symbol, seqVar: SequenceVarName,
  ): ExprBlob = ve match {
    // A class element spans exactly (beginGen, endGen) at this point — whether it
    // is produced by a nonterminal match, an inline construct, or a sealed choice.
    case ValuefyExpr.MatchNonterminal(_) => ExprBlob(List(), s"($beginGen, $endGen)", Set())
    case ValuefyExpr.ConstructCall(_, _) => ExprBlob(List(), s"($beginGen, $endGen)", Set())
    case ValuefyExpr.UnrollChoices(_) => ExprBlob(List(), s"($beginGen, $endGen)", Set())
    case ValuefyExpr.Unbind(sym, e) => deltaElemCoord(e, beginGen, endGen, sym, seqVar)
    case ValuefyExpr.SeqElemAt(index, e) =>
      val sequenceId = analysis.ngrammar.idOf(symbol)
      val sequence = analysis.ngrammar.nsequences(sequenceId)
      seqVar.name match {
        case Some(sv) => deltaElemCoord(e, s"$sv[$index].0", s"$sv[$index].1", sequence.symbol.seq(index), seqVar)
        case None =>
          val sv = newVar()
          val getSeq = s"let $sv = get_sequence_elems_lazy(&ctx.hist, $sequenceId, &[${sequence.sequence.mkString(", ")}], $beginGen, $endGen);"
          seqVar.name = Some(sv)
          val inner = deltaElemCoord(e, s"$sv[$index].0", s"$sv[$index].1", sequence.symbol.seq(index), seqVar)
          ExprBlob(getSeq +: inner.prepares, inner.result, inner.required)
      }
    case other => throw new Exception(s"deltaElemCoord: unsupported ${other.getClass.getSimpleName}")
  }

  // ---- scalar-producing expressions (mirror plain walk over the lazy hist) ---

  private def deltaScalarExpr(
    ve: ValuefyExpr, beginGen: String, endGen: String, symbol: Symbols.Symbol, seqVar: SequenceVarName,
  ): ExprBlob = ve match {
    case ValuefyExpr.MatchNonterminal(name) =>
      _requiredNonterms += name
      val v = newVar()
      ExprBlob(List(s"let $v = ${deltaFn(name)}(ctx, $beginGen, $endGen);"), v, Set())
    case ValuefyExpr.Unbind(sym, e) => deltaScalarExpr(e, beginGen, endGen, sym, seqVar)
    case ValuefyExpr.JoinBody(bp) =>
      deltaScalarExpr(bp, beginGen, endGen, symbol.asInstanceOf[Symbols.Join].sym, SequenceVarName(None))
    case ValuefyExpr.JoinCond(cp) =>
      deltaScalarExpr(cp, beginGen, endGen, symbol.asInstanceOf[Symbols.Join].join, SequenceVarName(None))
    case ValuefyExpr.SeqElemAt(index, e) =>
      val sequenceId = analysis.ngrammar.idOf(symbol)
      val sequence = analysis.ngrammar.nsequences(sequenceId)
      seqVar.name match {
        case Some(sv) => deltaScalarExpr(e, s"$sv[$index].0", s"$sv[$index].1", sequence.symbol.seq(index), seqVar)
        case None =>
          val sv = newVar()
          val getSeq = s"let $sv = get_sequence_elems_lazy(&ctx.hist, $sequenceId, &[${sequence.sequence.mkString(", ")}], $beginGen, $endGen);"
          seqVar.name = Some(sv)
          val inner = deltaScalarExpr(e, s"$sv[$index].0", s"$sv[$index].1", sequence.symbol.seq(index), seqVar)
          ExprBlob(getSeq +: inner.prepares, inner.result, inner.required)
      }
    case ValuefyExpr.UnrollRepeatFromZero(ep) => deltaUnrollScalar("unroll_repeat0_lazy", ep, beginGen, endGen, symbol)
    case ValuefyExpr.UnrollRepeatFromZeroNoUnbind(_, ep) => deltaUnrollScalar("unroll_repeat0_lazy", ep, beginGen, endGen, symbol)
    case ValuefyExpr.UnrollRepeatFromOne(ep) => deltaUnrollScalar("unroll_repeat1_lazy", ep, beginGen, endGen, symbol)
    case ValuefyExpr.UnrollRepeatFromOneNoUnbind(_, ep) => deltaUnrollScalar("unroll_repeat1_lazy", ep, beginGen, endGen, symbol)
    case ValuefyExpr.UnrollChoices(choices) => deltaScalarChoices(choices, beginGen, endGen)
    case ValuefyExpr.FuncCall(funcType, params) => deltaFuncCall(funcType, params, beginGen, endGen, symbol, seqVar)
    case ValuefyExpr.ArrayExpr(elems) =>
      val elemCodes = elems.map(deltaScalarExpr(_, beginGen, endGen, symbol, seqVar))
      ExprBlob(elemCodes.flatMap(_.prepares), s"vec![${elemCodes.map(_.result).mkString(", ")}]", Set())
    case ValuefyExpr.BinOp(op, lhs, rhs) =>
      val l = deltaScalarExpr(lhs, beginGen, endGen, symbol, seqVar)
      val r = deltaScalarExpr(rhs, beginGen, endGen, symbol, seqVar)
      val opExpr = op match {
        case ValuefyExpr.BinOpType.ADD =>
          (typeOf(lhs), typeOf(rhs)) match {
            case (Type.StringType, Type.StringType) => s"""format!("{}{}", ${l.result}, ${r.result})"""
            case (Type.ArrayOf(_), Type.ArrayOf(_)) => s"{ let mut v = ${l.result}; v.extend(${r.result}); v }"
          }
        case ValuefyExpr.BinOpType.EQ => s"(${l.result} == ${r.result})"
        case ValuefyExpr.BinOpType.NE => s"(${l.result} != ${r.result})"
        case ValuefyExpr.BinOpType.BOOL_AND => s"(${l.result} && ${r.result})"
        case ValuefyExpr.BinOpType.BOOL_OR => s"(${l.result} || ${r.result})"
      }
      ExprBlob(l.prepares ++ r.prepares, opExpr, Set())
    case ValuefyExpr.PreOp(ValuefyExpr.PreOpType.NOT, e) =>
      val ec = deltaScalarExpr(e, beginGen, endGen, symbol, seqVar)
      val rv = newVar()
      ExprBlob(ec.prepares :+ s"let $rv = !${ec.result};", rv, Set())
    case ValuefyExpr.ElvisOp(expr, ifNull) =>
      val exprVar = newVar()
      val ec = deltaScalarExpr(expr, beginGen, endGen, symbol, seqVar)
      val ifn = deltaScalarExpr(ifNull, beginGen, endGen, symbol, seqVar)
      ExprBlob(ec.prepares :+ s"let $exprVar = ${ec.result};",
        s"$exprVar.unwrap_or_else(|| ${ifn.asBlockExpr})", Set())
    case ValuefyExpr.TernaryOp(cond, ifTrue, ifFalse) =>
      val c = deltaScalarExpr(cond, beginGen, endGen, symbol, seqVar)
      val tb = deltaScalarExpr(ifTrue, beginGen, endGen, symbol, seqVar)
      val fb = deltaScalarExpr(ifFalse, beginGen, endGen, symbol, seqVar)
      val rv = newVar()
      ExprBlob(c.prepares :+ s"let $rv = if ${c.result} { ${tb.asBlockExpr} } else { ${fb.asBlockExpr} };", rv, Set())
    case ValuefyExpr.NullLiteral => ExprBlob.code("None")
    case ValuefyExpr.BoolLiteral(value) => ExprBlob.code(s"$value")
    case ValuefyExpr.CharLiteral(value) => ExprBlob.code(s"'${escapeChar(value)}'")
    case ValuefyExpr.CharFromTerminalLiteral => ExprBlob(List(), s"ctx.source_chars[$beginGen as usize]", Set())
    case ValuefyExpr.StringLiteral(value) => ExprBlob.code("\"" + escapeString(value) + "\".to_string()")
    case ValuefyExpr.CanonicalEnumValue(enumName, ev) => ExprBlob.code(s"crate::ast::$enumName::$ev")
    case ValuefyExpr.ShortenedEnumValue(uid, ev) =>
      ExprBlob.code(s"crate::ast::${analysis.shortenedEnumTypesMap(uid)}::$ev")
    case other => throw new Exception(s"deltaScalarExpr: unsupported ${other.getClass.getSimpleName}")
  }

  private def deltaUnrollScalar(
    helperFn: String, elemProcessor: ValuefyExpr, beginGen: String, endGen: String, symbol: Symbols.Symbol,
  ): ExprBlob = {
    val v = newVar()
    val symbolId = analysis.ngrammar.idOf(symbol)
    val repeat = analysis.ngrammar.symbolOf(symbolId).asInstanceOf[NRepeat]
    val itemSymId = analysis.ngrammar.idOf(repeat.symbol.sym)
    val elemCode = deltaScalarExpr(elemProcessor, "k.0", "k.1", repeat.symbol.sym, SequenceVarName(None))
    val closureBody = if (elemCode.prepares.isEmpty) elemCode.result else s"${elemCode.prepares.mkString(" ")} ${elemCode.result}"
    ExprBlob(
      List(
        s"let $v: Vec<_> = $helperFn(&ctx.hist, $symbolId, $itemSymId, ${repeat.baseSeq}, ${repeat.repeatSeq}, $beginGen, $endGen)",
        s"    .into_iter().map(|k| { $closureBody }).collect();"),
      v, Set())
  }

  private def deltaFuncCall(
    funcType: ValuefyExpr.FuncType.Value, params: List[ValuefyExpr],
    beginGen: String, endGen: String, symbol: Symbols.Symbol, seqVar: SequenceVarName,
  ): ExprBlob = funcType match {
    case ValuefyExpr.FuncType.IsPresent =>
      val param = deltaScalarExpr(params.head, beginGen, endGen, symbol, seqVar)
      @tailrec def code(t: Type): String = t match {
        case Type.ArrayOf(_) => s"!${param.result}.is_empty()"
        case Type.OptionalOf(_) => s"${param.result}.is_some()"
        case Type.StringType => s"!${param.result}.is_empty()"
        case u: Type.UnionOf => code(analysis.reduceUnionType(u))
        case _ => s"${param.result}.is_some()"
      }
      ExprBlob(param.prepares, code(typeOf(params.head)), Set())
    case ValuefyExpr.FuncType.IsEmpty =>
      val param = deltaScalarExpr(params.head, beginGen, endGen, symbol, seqVar)
      @tailrec def code(t: Type): String = t match {
        case Type.ArrayOf(_) => s"${param.result}.is_empty()"
        case Type.OptionalOf(_) => s"${param.result}.is_none()"
        case Type.StringType => s"${param.result}.is_empty()"
        case u: Type.UnionOf => code(analysis.reduceUnionType(u))
        case _ => s"${param.result}.is_none()"
      }
      ExprBlob(param.prepares, code(typeOf(params.head)), Set())
    case ValuefyExpr.FuncType.Chr =>
      deltaScalarExpr(params.head, beginGen, endGen, symbol, seqVar)
    case ValuefyExpr.FuncType.Str =>
      val paramCodes = params.map(deltaScalarExpr(_, beginGen, endGen, symbol, seqVar))
      def toStr(input: String, t: Type): String = t match {
        case Type.ArrayOf(elemType) => s"""$input.into_iter().map(|it| ${toStr("it", elemType)}).collect::<String>()"""
        case Type.OptionalOf(valueType) => s"""$input.map(|it| ${toStr("it", valueType)}).unwrap_or_default()"""
        case Type.BoolType => s"$input.to_string()"
        case Type.CharType => s"$input.to_string()"
        case Type.StringType => input
        case u: Type.UnionOf => toStr(input, analysis.reduceUnionType(u))
      }
      val pieces = paramCodes.zip(params).map { case (pc, p) => toStr(pc.result, typeOf(p)) }
      val result = if (pieces.size == 1) pieces.head else s"""[${pieces.map(p => s"($p)").mkString(", ")}].concat()"""
      ExprBlob(paramCodes.flatMap(_.prepares), result, Set())
  }

  private def scalarProtoField(f: String, ftype: Type, blob: ExprBlob): (List[String], List[String]) = {
    reduceType(ftype) match {
      case Type.OptionalOf(Type.ArrayOf(elemT2)) =>
        val conv = reduceType(elemT2) match {
          case Type.CharType => "xs.into_iter().map(|c| c as i32).collect()"
          case Type.EnumType(_) | Type.UnspecifiedEnumType(_) => "xs.into_iter().map(|e| (e as i32) + 1).collect()"
          case _ => "xs"
        }
        // Bind a typed temp so a `None` literal (absent field) infers its element.
        (blob.prepares ++ List(
          s"let ${f}_opt: ${deltaReturnType(ftype)} = ${blob.result};",
          s"let (${f}_present, $f): (bool, Vec<_>) = match ${f}_opt { Some(xs) => (true, $conv), None => (false, Vec::new()) };"),
          List(s"${f}_present: ${f}_present", s"$f: $f"))
      case Type.OptionalOf(inner) =>
        val (someVal, none) = reduceType(inner) match {
          case Type.CharType => ("x as i32", "0")
          case Type.EnumType(_) | Type.UnspecifiedEnumType(_) => ("(x as i32) + 1", "0")
          case Type.BoolType => ("x", "false")
          case Type.StringType => ("x", "String::new()")
          case _ => ("x", "Default::default()")
        }
        (blob.prepares ++ List(
          s"let ${f}_opt: ${deltaReturnType(ftype)} = ${blob.result};",
          s"let (${f}_present, $f) = match ${f}_opt { Some(x) => (true, $someVal), None => (false, $none) };"),
          List(s"${f}_present: ${f}_present", s"$f: $f"))
      case Type.ArrayOf(elemT) =>
        val conv = reduceType(elemT) match {
          case Type.CharType => s"${blob.result}.into_iter().map(|c| c as i32).collect()"
          case Type.EnumType(_) | Type.UnspecifiedEnumType(_) => s"${blob.result}.into_iter().map(|e| (e as i32) + 1).collect()"
          case _ => blob.result
        }
        (blob.prepares, List(s"$f: $conv"))
      case Type.EnumType(_) | Type.UnspecifiedEnumType(_) =>
        (blob.prepares, List(s"$f: (${blob.result} as i32) + 1"))
      case Type.CharType =>
        (blob.prepares, List(s"$f: ${blob.result} as i32"))
      case _ =>
        (blob.prepares, List(s"$f: ${blob.result}"))
    }
  }

  // ===========================================================================
  // Proto encoder: typed AST -> ID-based ParseResult (see phase_b_proto_design).
  //
  // Emitted into a separate `encode.rs`. Rules MUST match Stage2ProtoEmit:
  //   - one NodeEntry per concrete node; id monotonic from 1 (0 = absent).
  //   - Msg field           -> child encoded first, its id stored as int32.
  //   - Opt(Msg) field      -> `<f>_present: bool` + `<f>: i32` (0 when absent).
  //   - Opt(Arr(Msg))       -> `<f>_present: bool` + `<f>: Vec<i32>`.
  //   - Arr(Msg)            -> `<f>: Vec<i32>`.
  //   - scalars/enums       -> copied directly.
  //   - sealed parent       -> match, delegate to the concrete variant's enc fn.
  //   - nodes pushed post-order (children before parent) so the Kotlin decoder
  //     resolves in one forward pass.
  // ===========================================================================

  def generateEncoder(): String = {
    val hierarchy = analysis.classRelations.toHierarchy
    val all = hierarchy.allTypes.values.toList.sortBy(_.className)
    val concretes = all.filter(_.subclasses.isEmpty)
    val sealeds = all.filter(_.subclasses.nonEmpty)

    val header =
      """// Generated by RustOptCodeGen — do not edit by hand.
        |// typed AST -> ID-based proto ParseResult. See phase_b_proto_design.md.
        |
        |use crate::ast::*;
        |use crate::proto;
        |
        |/// Accumulates NodeEntry rows and hands out monotonic ids (from 1).
        |struct Encoder {
        |    nodes: Vec<proto::NodeEntry>,
        |    next_id: i32,
        |}
        |
        |impl Encoder {
        |    fn new() -> Self {
        |        Self { nodes: Vec::new(), next_id: 1 }
        |    }
        |    fn alloc(&mut self) -> i32 {
        |        let id = self.next_id;
        |        self.next_id += 1;
        |        id
        |    }
        |}
        |
        |""".stripMargin

    val concreteFns = concretes.map(concreteEncodeFn).mkString("\n")
    val sealedFns = sealeds.map(sealedEncodeFn).mkString("\n")

    // Public entry: encode the start symbol's type. `enc` is owned here, so the
    // call site borrows it mutably (inner fns already take `&mut Encoder`).
    val rootType = analysis.nonterminalTypes(analysis.startNonterminalName)
    val rootFn = rootType match {
      case Type.ClassType(name) => s"enc_${camelToSnake(name)}"
      case other => throw new Exception(s"start symbol type is not a class: ${Type.readableNameOf(other)}")
    }
    val entry =
      s"""/// Encode a typed AST root into a `ParseResult`.
         |pub fn encode(root: &${rustTypeUnboxed(rootType)}) -> proto::ParseResult {
         |    let mut enc = Encoder::new();
         |    let root_id = $rootFn(&mut enc, root);
         |    proto::ParseResult { root_id, nodes: enc.nodes }
         |}
         |""".stripMargin

    header + concreteFns + "\n" + sealedFns + "\n" + entry
  }

  /** `enc_<snake>` for a concrete class: encode children, build proto msg, push. */
  private def concreteEncodeFn(cls: ClassHierarchyItem): String = {
    val className = cls.className
    val fnName = s"enc_${camelToSnake(className)}"
    val params = analysis.classParamTypes.getOrElse(className, List())

    val prepares = scala.collection.mutable.ListBuffer[String]()
    val protoFields = scala.collection.mutable.ListBuffer[String]()

    params.foreach { case (pname, ptype) =>
      val field = rustFieldName(pname)
      ptype match {
        case Type.ClassType(_) =>
          // child id
          prepares += s"    let ${field}_id = ${encodeCallFor(ptype, s"&node.$field")};"
          protoFields += s"$field: ${field}_id"
        case Type.OptionalOf(Type.ClassType(_)) =>
          prepares += s"    let (${field}_present, ${field}_id) = match &node.$field {"
          prepares += s"        Some(x) => (true, ${encodeCallFor(ptype.asInstanceOf[Type.OptionalOf].typ, "x")}),"
          prepares += s"        None => (false, 0),"
          prepares += s"    };"
          protoFields += s"${field}_present: ${field}_present"
          protoFields += s"$field: ${field}_id"
        case Type.OptionalOf(Type.ArrayOf(elemT)) if isMsg(elemT) =>
          prepares += s"    let (${field}_present, $field): (bool, Vec<i32>) = match &node.$field {"
          prepares += s"        Some(xs) => (true, xs.iter().map(|x| ${encodeCallFor(elemT, "x")}).collect()),"
          prepares += s"        None => (false, Vec::new()),"
          prepares += s"    };"
          protoFields += s"${field}_present: ${field}_present"
          protoFields += s"$field: $field"
        case Type.OptionalOf(Type.ArrayOf(elemT2)) =>
          // Opt(Arr(scalar)): char/enum 은 i32 변환 (enum 은 +1 시프트).
          val conv = reduceType(elemT2) match {
            case Type.CharType => "xs.iter().map(|c| *c as i32).collect()"
            case Type.EnumType(_) | Type.UnspecifiedEnumType(_) =>
              "xs.iter().map(|e| (*e as i32) + 1).collect()"
            case _ => "xs.clone()"
          }
          prepares += s"    let (${field}_present, $field): (bool, Vec<_>) = match &node.$field {"
          prepares += s"        Some(xs) => (true, $conv),"
          prepares += s"        None => (false, Vec::new()),"
          prepares += s"    };"
          protoFields += s"${field}_present: ${field}_present"
          protoFields += s"$field: $field"
        case Type.OptionalOf(inner) =>
          // Opt(scalar/enum): present flag + copied value (default when absent).
          prepares += s"    let (${field}_present, $field) = match &node.$field {"
          prepares += s"        Some(x) => (true, ${copyScalar(inner, "x")}),"
          prepares += s"        None => (false, ${scalarDefault(inner)}),"
          prepares += s"    };"
          protoFields += s"${field}_present: ${field}_present"
          protoFields += s"$field: $field"
        case Type.ArrayOf(elemT) if isMsg(elemT) =>
          prepares += s"    let $field: Vec<i32> = node.$field.iter().map(|x| ${encodeCallFor(elemT, "x")}).collect();"
          protoFields += s"$field: $field"
        case Type.ArrayOf(elemT) =>
          // Vec of scalars. char/enum become i32 (with the enum +1 shift).
          reduceType(elemT) match {
            case Type.CharType =>
              protoFields += s"$field: node.$field.iter().map(|c| *c as i32).collect()"
            case Type.EnumType(_) | Type.UnspecifiedEnumType(_) =>
              protoFields += s"$field: node.$field.iter().map(|e| (*e as i32) + 1).collect()"
            case _ =>
              protoFields += s"$field: node.$field.clone()"
          }
        case _ =>
          // scalar / enum
          protoFields += s"$field: ${copyScalarField(ptype, s"node.$field")}"
      }
    }

    val protoFieldsStr =
      (protoFields.toList :+ "start: node.start" :+ "end: node.end").map("        " + _).mkString(",\n")

    s"""fn $fnName(enc: &mut Encoder, node: &${rustClassName(className)}) -> i32 {
       |${prepares.mkString("\n")}
       |    let id = enc.alloc();
       |    let msg = proto::${rustClassName(className)} {
       |$protoFieldsStr,
       |    };
       |    enc.nodes.push(proto::NodeEntry {
       |        id,
       |        node: Some(proto::node_entry::Node::${rustClassName(className)}(msg)),
       |    });
       |    id
       |}
       |""".stripMargin
  }

  /** `enc_<snake>` for a sealed parent: match and delegate to the variant. */
  private def sealedEncodeFn(cls: ClassHierarchyItem): String = {
    val fnName = s"enc_${camelToSnake(cls.className)}"
    val arms = cls.subclasses.toList.sorted.map { sub =>
      s"        ${rustClassName(cls.className)}::${rustClassName(sub)}(x) => enc_${camelToSnake(sub)}(enc, x),"
    }
    s"""fn $fnName(enc: &mut Encoder, node: &${rustClassName(cls.className)}) -> i32 {
       |    match node {
       |${arms.mkString("\n")}
       |    }
       |}
       |""".stripMargin
  }

  /** Build the `enc_x(enc, <expr>)` call for a Msg-or-sealed-typed value. */
  private def encodeCallFor(t: Type, expr: String): String = t match {
    case Type.ClassType(name) => s"enc_${camelToSnake(name)}(enc, $expr)"
    case Type.OptionalOf(inner) => encodeCallFor(inner, expr)
    case _ => throw new Exception(s"encodeCallFor on non-message type: ${Type.readableNameOf(t)}")
  }

  private def isMsg(t: Type): Boolean = t match {
    case Type.ClassType(_) => true
    case _ => false
  }

  // Scalar/enum copy at a proto field. Strings clone; char becomes i32
  // codepoint (proto stores char as int32 per Stage2ProtoEmit's CharType=>Int32).
  // Enums: the Rust AST enum discriminants are 0-based (sorted variant order)
  // while the proto enum reserves 0 for UNSPECIFIED and assigns the same
  // sorted order from 1 (Stage2ProtoEmit) — hence the +1 shift.
  private def copyScalarField(t: Type, expr: String): String = t match {
    case Type.StringType => s"$expr.clone()"
    case Type.CharType => s"$expr as i32"
    case Type.BoolType => expr
    case Type.EnumType(_) | Type.UnspecifiedEnumType(_) => s"($expr as i32) + 1"
    case _ => s"$expr.clone()"
  }

  private def copyScalar(t: Type, expr: String): String = t match {
    case Type.StringType => s"$expr.clone()"
    case Type.CharType => s"*$expr as i32"
    case Type.BoolType => s"*$expr"
    case Type.EnumType(_) | Type.UnspecifiedEnumType(_) => s"(*$expr as i32) + 1"
    case _ => s"$expr.clone()"
  }

  private def scalarDefault(t: Type): String = t match {
    case Type.StringType => "String::new()"
    case Type.CharType => "0"
    case Type.BoolType => "false"
    case Type.EnumType(_) | Type.UnspecifiedEnumType(_) => "0"
    case _ => "Default::default()"
  }
}

object RustOptCodeGen {
  /** SchemaBuilder.RUST_RESERVED_TYPE_NAMES 와 동일 목록 유지할 것. */
  val RustReservedTypeNames: Set[String] = Set(
    "Self", "Box", "Option", "Vec", "String", "Result", "Some", "None", "Ok", "Err",
    "Ctx", "Encoder", "Kernel", "KernelSet", "IdIssuer",
  )

  val RustKeywords: Set[String] = Set(
    "as", "break", "const", "continue", "crate", "else", "enum", "extern", "false", "fn",
    "for", "if", "impl", "in", "let", "loop", "match", "mod", "move", "mut", "pub", "ref",
    "return", "self", "Self", "static", "struct", "super", "trait", "true", "type", "unsafe",
    "use", "where", "while", "async", "await", "dyn", "abstract", "become", "box", "do",
    "final", "macro", "override", "priv", "typeof", "unsized", "virtual", "yield", "try",
  )
}
