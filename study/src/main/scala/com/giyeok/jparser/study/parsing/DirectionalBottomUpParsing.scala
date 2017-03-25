package com.giyeok.jparser.study.parsing

import com.giyeok.jparser.Inputs.Input
import com.giyeok.jparser.study.CfgSymbols.CfgSymbol
import com.giyeok.jparser.study.CfgSymbols.Nonterminal
import com.giyeok.jparser.study.CfgSymbols.Terminal
import com.giyeok.jparser.study.ContextFreeGrammar

object DirectionalBottomUpParsing {
    case class Item(lhs: Nonterminal, rhs: Seq[CfgSymbol], dot: Int, p: Int) {
        def completed: Boolean = rhs.length == dot
        def following: CfgSymbol = rhs(dot)
        def proceed: Item = Item(lhs, rhs, dot + 1, p)

        override def toString: String = {
            val (h, t) = rhs.splitAt(dot)
            val rhsString = (h map { _.toShortString } mkString " ") + "\u2022" + (t map { _.toShortString } mkString " ")
            s"${lhs.name} -> $rhsString @ $p"
        }
    }

    case class Itemset(p: Int, items: Set[Item]) {
        val completed: Set[Item] = items filter { _.completed }
        val active: Set[Item] = items filterNot { _.completed }
        def isEmpty: Boolean = items.isEmpty

        def print(): Unit = {
            println(s"Itemset $p")
            completed foreach { c => println(c.toString) }
            println("----------")
            val (act, pred) = active partition { _.dot > 0 }
            act foreach { c => println(c.toString) }
            println("----------")
            pred foreach { c => println(c.toString) }
        }

        def lines(): Seq[String] = {
            sealed trait Line { def length: Int }
            case class Title(text: String) extends Line { def length = text.length }
            case class LeftAligned(text: String) extends Line { def length = text.length }
            case class Separator(l: String, c: String, r: String) extends Line { def length = 1 }

            val (act, pred) = active partition { _.dot > 0 }
            val lines = Seq(
                Seq(Title(s"Itemset $p")),
                Seq(Separator("+", "=", "+")),
                completed map { i => LeftAligned(i.toString) },
                Seq(Separator("+", "-", "+")),
                act map { i => LeftAligned(i.toString) },
                Seq(Separator("|", " ", "|")),
                pred map { i => LeftAligned(i.toString) },
                Seq(Separator("+", "=", "+"))
            ).flatten

            val width = (lines map { _.length }).max
            val r = lines map {
                case Title(text) =>
                    val left = (width - text.length) / 2
                    "  " + (" " * left) + text + (" " * (width - text.length - left)) + "  "
                case LeftAligned(text) =>
                    "| " + text + (" " * (width - text.length)) + " |"
                case Separator(l, c, r) =>
                    l + (c * (width + 2)) + r
            }
            assert(r forall { _.length == r.head.length })
            r
        }
    }

    case class History(history: Seq[Itemset]) {
        def apply(i: Int): Itemset = history(i)
        def :+(next: Itemset) = History(history :+ next)

        def horizontalLines(): Seq[String] = {
            val itemLines = history map { _.lines() }
            val height = (itemLines map { _.length }).max
            itemLines.foldLeft((0 until height) map { _ => "" }) { (cc, i) =>
                assert(cc.length == height)
                val box = i ++ ((0 until (height - i.length)) map { _ => " " * i.head.length })
                cc zip box map { x => x._1 + "  " + x._2 }
            }
        }
    }

    case class Context(p: Int, history: History, itemset: Itemset)

    class EarleyParser(grammar: ContextFreeGrammar) {
        def scanner(p: Int, itemset: Itemset, input: Input): Set[Item] = {
            val accepting = itemset.active filter { item =>
                item.following match {
                    case Terminal(terminal) => terminal accept input
                    case _ => false
                }
            }
            accepting map { _.proceed }
        }

        def completer(completed: Set[Item], history: History): Set[Item] = {
            completed filter { _.completed } flatMap { completed =>
                val proceedings = history(completed.p).active filter { item =>
                    item.following match {
                        case nonterminal: Nonterminal => nonterminal == completed.lhs
                        case _ => false
                    }
                }
                proceedings map { _.proceed }
            }
        }

        def predictor(p: Int, predicting: Set[Item]): Set[Item] = {
            predicting filterNot { _.completed } flatMap { predicting =>
                predicting.following match {
                    case lhs @ Nonterminal(name) =>
                        grammar.rules(name) map { rhs => Item(lhs, rhs, 0, p) }
                    case _ => Set()
                }
            }
        }

        def repeatUntilStablized(func: Set[Item] => Set[Item], items: Set[Item], cc: Set[Item]): Set[Item] =
            if (items subsetOf cc) cc else repeatUntilStablized(func, func(items), cc ++ items)

        def initial: Context = {
            val starter = (grammar.rules(grammar.startNonterminal) map { rhs =>
                Item(Nonterminal(grammar.startNonterminal), rhs, 0, 0)
            }).toSet
            val itemset = Itemset(0, repeatUntilStablized(predictor(0, _), starter, Set()))
            Context(0, History(Seq(itemset)), itemset)
        }

        def proceed(context: Context, input: Input): Option[Context] = {
            val nextp = context.p + 1
            val scanned = scanner(nextp, context.itemset, input)
            if (scanned.isEmpty) None else {
                val completed = repeatUntilStablized(completer(_, context.history), scanned, Set())
                val predicted = repeatUntilStablized(predictor(nextp, _), scanned ++ completed, Set())

                val itemset = Itemset(nextp, completed ++ predicted)
                Some(Context(nextp, context.history :+ itemset, itemset))
            }
        }
    }
}
