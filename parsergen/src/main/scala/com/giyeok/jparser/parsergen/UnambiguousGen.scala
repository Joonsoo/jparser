package com.giyeok.jparser.parsergen

import com.giyeok.jparser.Inputs.CharacterTermGroupDesc
import com.giyeok.jparser.examples.SimpleGrammars
import com.giyeok.jparser.nparser.NGrammar

class UnambiguousGen(val grammar: NGrammar) {
    val simpleGen: SimpleGen = new SimpleGenGen(grammar).generateGenerator()
    val nodeActions: Map[Int, Map[CharacterTermGroupDesc, SimpleGen.Action]] =
        simpleGen.termActions.groupBy(_._1._1) map { kv =>
            kv._1 -> (kv._2.toSeq map { k => k._1._2 -> k._2 }).toMap
        }

    def analyze(): Unit = {
        val directReplaceable: Set[(Int, Int)] = ((nodeActions.toSeq flatMap { kv =>
            kv._2.values collect {
                case SimpleGen.Replace(replaceNodeType) =>
                    kv._1 -> replaceNodeType
                case SimpleGen.ReplaceAndAppend(replaceNodeType, _, _) =>
                    kv._1 -> replaceNodeType
                case SimpleGen.ReplaceAndFinish(replaceNodeType) =>
                    kv._1 -> replaceNodeType
            }
        }) ++ (simpleGen.impliedNodes.toSeq flatMap { kv =>
            kv._2 match {
                case Some(repl) =>
                    if (repl._1 != kv._1._1) Set[(Int, Int)](kv._1._1 -> repl._1, kv._1._2 -> repl._2)
                    else Set[(Int, Int)](kv._1._2 -> repl._2)
                case None => Set[(Int, Int)]()
            }
        })).toSet
        val directReplaceableMap = directReplaceable.groupBy(_._1).mapValues(_.map(_._2).toSet)
        val directReachable = (nodeActions.toSeq flatMap { kv =>
            val originNode = kv._1
            kv._2.values collect {
                case SimpleGen.Append(appendNode, pendingFinish) =>
                    (originNode, appendNode, pendingFinish)
                case SimpleGen.ReplaceAndAppend(replaceNodeType, appendNodeType, pendingFinish) =>
                    (replaceNodeType, appendNodeType, pendingFinish)
            }
        }).toSet
        println("Replaceable: ", directReplaceable)
        println("ReplaceableMap: ")
        directReplaceableMap.toSeq.sortBy(_._1) foreach {
            println(_)
        }
        println(directReachable)
    }
}

object UnambiguousGen {
    def main(args: Array[String]): Unit = {
        val grammar = NGrammar.fromGrammar(SimpleGrammars.arrayGrammar)
        val gengen = new UnambiguousGen(grammar)
        gengen.nodeActions.toSeq.sortBy(_._1) foreach { kv =>
            println(s"Node ${kv._1}:")
            kv._2.toSeq.sortBy(_._1.toShortString) foreach { acts =>
                println(s"  ${acts._1.toShortString} -> ${acts._2}")
            }
        }
        println("Implied:")
        gengen.simpleGen.impliedNodes.toSeq.sortBy(_._1) foreach { kv =>
            println(s"  ${kv._1} -> ${kv._2}")
        }
        gengen.analyze()
    }
}
