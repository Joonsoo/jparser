package com.giyeok.jparser.swingvis

import com.giyeok.gviz.figure.Figure
import com.giyeok.jparser.Inputs._
import com.giyeok.jparser.ParseResultTree._
import com.giyeok.jparser.Symbols.{Nonterminal, Proxy, Repeat, Sequence, Symbol, Terminals}
import com.giyeok.jparser.gviz.FigureUtil._
import com.giyeok.jparser.metalang3.ValuefyExprSimulator
import com.giyeok.jparser.{ParseResultTree, Symbols}

class FigureGen(val renderJoin: Boolean = false, val renderLookaheadExcept: Boolean = true) {
  def parseNodeFigure(node: ParseResultTree.Node): Figure = node match {
    case ParseResultTree.TerminalNode(start, input) =>
      text(input.toShortString, "terminal")
    case ParseResultTree.BindNode(symbol: Repeat, body) =>
      // TODO 이거 고쳐야하나? 없애야 하나?
      def childrenOf(node: Node, sym: Symbol): List[Figure] = node match {
        case BindNode(`sym`, _) => List(parseNodeFigure(node))
        case BindNode(s, body) => childrenOf(body, sym)
        case b: CyclicBindNode => List(parseNodeFigure(b))
        case s: SequenceNode => s.children flatMap (childrenOf(_, sym))
        case s: CyclicSequenceNode => List(parseNodeFigure(s))
        case _ => ???
      }

      val children = childrenOf(body, symbol.symbol)
      container(
        vert(List(
          if (children.isEmpty) text("ε", "") else horiz(children, "sequence"),
          symbolFigure(symbol.symbol)
        ), ""),
        "bind")
    case ParseResultTree.BindNode(symbol, body) =>
      container(
        vert(List(
          parseNodeFigure(body),
          symbolFigure(symbol.symbol)
        ), ""),
        "bind")
    case ParseResultTree.CyclicBindNode(start, end, symbol) =>
      vert(List(text("cyclic", "small"), symbolFigure(symbol.symbol)), "bind")
    case ParseResultTree.JoinNode(symbol, body, join) =>
      // TODO symbol?
      if (renderJoin) {
        vert(List(parseNodeFigure(body), parseNodeFigure(join)), "join")
      } else {
        parseNodeFigure(body)
      }
    case s: ParseResultTree.SequenceNode =>
      if (s.children.isEmpty) {
        text("ε", "")
      } else {
        def isLookaheadNode(node: Node): Boolean = node match {
          case BindNode(_: Symbols.LookaheadExcept, _) => true
          case BindNode(_: Symbols.LookaheadIs, _) => true
          case _ => false
        }

        val seq = s.children.zipWithIndex flatMap { c =>
          val (child, idx) = c
          // if it is content child
          if (isLookaheadNode(child)) {
            if (renderLookaheadExcept) {
              Some(parseNodeFigure(child))
            } else {
              None
            }
          } else {
            Some(parseNodeFigure(child))
          }
        }
        horiz(seq, "sequence")
      }
    case ParseResultTree.CyclicSequenceNode(start, end, symbol, pointer, _children) => ???
      text("TODO(cyclicSequence)", "")
  }

  def exactCharacterRepr(char: Char): String = char match {
    case c if 33 <= c && c <= 126 => c.toString
    case '\n' => "\\n"
    case '\r' => "\\r"
    case '\t' => "\\t"
    case c => f"\\u$c%04x"
  }

  def rangeCharactersRepr(start: Char, end: Char): (String, String) =
    (exactCharacterRepr(start), exactCharacterRepr(end))

  def join(list: List[Figure], joining: => Figure): List[Figure] = list match {
    case head +: List() => List(head)
    case head +: next +: List() => List(head, joining, next)
    case head +: next +: rest => head +: joining +: join(next +: rest, joining)
  }

  def needParentheses(symbol: Symbol): Boolean =
    symbol match {
      case _@(Nonterminal(_) | Terminals.ExactChar(_) | Sequence(Seq(Terminals.ExactChar(_)))) => false
      case _ => true
    }

  def symbolFigure(symbol: Symbol): Figure = symbol match {
    case Terminals.ExactChar(char) => text(exactCharacterRepr(char), "terminal")
    case Terminals.Chars(chars) =>
      horiz(text("{", "") +: join(chars.groups map {
        case (f, t) if f == t => text(exactCharacterRepr(f), "terminal")
        case (f, t) =>
          val (rangeStart: String, rangeEnd: String) = rangeCharactersRepr(f, t)
          horiz(List(
            text(rangeStart, "terminal"), text("-", "default"), text(rangeEnd, "terminal")
          ), "")
      }, text("|", "")) :+ text("}", ""), "")
    case t: Symbols.Terminal => text(t.toShortString, "terminal")
    case Symbols.Start => text("Start", "")
    case Symbols.Nonterminal(name) => text(name, "nonterminal")
    case Symbols.OneOf(syms) =>
      if (syms.size == 2 && (syms contains Proxy(Sequence(Seq())))) {
        // A? 의 경우
        val opt = (syms - Proxy(Sequence(Seq()))).head
        horiz(Seq(symbolFigure(opt), text("?", "small")), "")
      } else {
        horiz(join((syms.toSeq sortBy (_.id) map { sym =>
          if (needParentheses(sym)) horiz(List(text("(", ""), symbolFigure(sym), text(")", "")), "")
          else symbolFigure(sym)
        }).toList, text("|", "")), "")
      }
    case Symbols.Repeat(sym, lower) =>
      val rep: String = lower match {
        case 0 => "*"
        case 1 => "+"
        case n => s"$n+"
      }
      if (needParentheses(sym)) horiz(List(text("(", ""), symbolFigure(sym), text(")" + rep, "")), "")
      else horiz(List(symbolFigure(sym), text(rep, "")), "")

    case Symbols.Except(sym, except) =>
      val symFig =
        if (!needParentheses(sym)) symbolFigure(sym)
        else horiz(Seq(text("(", ""), symbolFigure(sym), text(")", "")), "")
      val exceptFig =
        if (!needParentheses(except)) symbolFigure(except)
        else horiz(Seq(text("(", ""), symbolFigure(except), text(")", "")), "")
      horiz(List(symFig, text("-", ""), exceptFig), "")
    case Symbols.LookaheadIs(lookahead) =>
      horiz(List(text("(", ""), text("$", ""), symbolFigure(lookahead), text(")", "")), "")
    case Symbols.LookaheadExcept(except) =>
      horiz(List(text("(", ""), text("!", ""), symbolFigure(except), text(")", "")), "")
    case Symbols.Proxy(sym) =>
      horiz(List(text("[", ""), symbolFigure(sym), text("]", "")), "")
    case Symbols.Join(sym, join) =>
      horiz(List(symbolFigure(sym), text("&", ""), symbolFigure(join)), "")
    case Symbols.Longest(sym) =>
      horiz(List(text("<", ""), symbolFigure(sym), text(">", "")), "")
    case Symbols.Sequence(seq) =>
      if (seq.isEmpty) {
        text("ε", "")
      } else {
        def adjExChars(list: List[Terminals.ExactChar]): Figure =
          horiz(list map (symbolFigure(_)), "")

        val grouped = seq.foldRight((List[Figure](), List[Terminals.ExactChar]())) {
          (i, m) =>
            i match {
              case terminal: Terminals.ExactChar => (m._1, terminal +: m._2)
              case symbol if m._2.isEmpty => (symbolFigure(symbol) +: m._1, List())
              case symbol => (symbolFigure(symbol) +: adjExChars(m._2) +: m._1, List())
            }
        }
        horiz(if (grouped._2.isEmpty) grouped._1 else adjExChars(grouped._2) +: grouped._1, "")
      }
  }

  def intersperse[T](xs: List[T], item: => T): List[T] = xs match {
    case List() | List(_) => xs
    case head +: tail => head +: item +: intersperse(tail, item)
  }

  def astValueFigure(value: ValuefyExprSimulator.Value): Figure = value match {
    case ValuefyExprSimulator.NodeValue(astNode) =>
      text(astNode.sourceText, "")
    case ValuefyExprSimulator.ClassValue(className, args) =>
      val argsFig = intersperse(args.map(astValueFigure), text(",", "small"))
      horiz(
        text(className, "nonterminal") +: text("(", "small") +:
          argsFig :+ text(")", "small"), "")
    case ValuefyExprSimulator.ArrayValue(elems) =>
      val elemsFig = intersperse(elems.map(astValueFigure), text(",", "small"))
      horiz((text("[", "small") +: elemsFig) :+ text("]", "small"), "")
    case ValuefyExprSimulator.EnumValue(enumType, enumValue) =>
      text(s"%$enumType.$enumValue", "literal")
    case ValuefyExprSimulator.NullValue =>
      text("null", "keyword")
    case ValuefyExprSimulator.BoolValue(value) =>
      text(s"$value", "keyword")
    case ValuefyExprSimulator.CharValue(value) =>
      text(s"$value", "literal")
    case ValuefyExprSimulator.StringValue(value) =>
      text("\"" + value + "\"", "literal")
  }
}
