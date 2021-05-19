package com.giyeok.jparser.milestone.codegen

import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.fast.{GraphNoIndex, KernelTemplate, TasksSummary}
import com.giyeok.jparser.metalang3a.generated.MetaLang3Ast
import com.giyeok.jparser.milestone._
import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.nparser.ParsingContext.Kernel
import com.giyeok.jparser.nparser.{AcceptCondition, ParsingContext}
import com.giyeok.jparser.utils.ScalaCodeGenUtil._
import com.giyeok.jparser.{Inputs, NGrammar}

class ScalaParserDataCodeGen(parserData: MilestoneParserData) {
  def requiredImports(): Set[String] = Set(
    "com.giyeok.jparser.Inputs",
    "com.giyeok.jparser.nparser.AcceptCondition.Always",
    "com.giyeok.jparser.nparser.AcceptCondition.Never",
    "com.giyeok.jparser.nparser.AcceptCondition.And",
    "com.giyeok.jparser.nparser.AcceptCondition.Or",
    "com.giyeok.jparser.nparser.AcceptCondition.NotExists",
    "com.giyeok.jparser.nparser.AcceptCondition.Exists",
    "com.giyeok.jparser.nparser.AcceptCondition.Unless",
    "com.giyeok.jparser.nparser.AcceptCondition.OnlyIf",
    "com.giyeok.jparser.nparser.ParsingContext.Graph",
    "com.giyeok.jparser.nparser.ParsingContext.Node",
    "com.giyeok.jparser.nparser.ParsingContext.Kernel",
    "com.giyeok.jparser.fast.KernelTemplate",
    "com.giyeok.jparser.fast.TasksSummary",
    "com.giyeok.jparser.milestone.MilestoneParserData")

  def parserData(grammarDef: ScalaCodeBlobNode): ScalaCodeBlobNode =
    ArgsCall("MilestoneParserData", List(
      grammarDef,
      tasksSummary(parserData.byStart),
      termActions(),
      edgeProgressActions(),
      derivedGraph()))

  def parserDataFile(packageName: String, objectName: String): ScalaFile = {
    val ngrammarValName = "ngrammar"
    val milestoneParserDataValName = "milestoneParserData"
    ScalaFile(packageName, requiredImports(), List(
      ObjectDef(objectName, List(
        TextBlob("def k(symbolId: Int, pointer: Int, beginGen: Int, endGen: Int): Kernel = Kernel(symbolId, pointer, beginGen, endGen)"),
        TextBlob("def t(symbolId: Int, pointer: Int): KernelTemplate = KernelTemplate(symbolId, pointer)"),
        ValBlob(milestoneParserDataValName, parserData(TextBlob(ngrammarValName)))
      ))))
  }

  def termActions(): ScalaCodeBlobNode = {
    val mapEntries = parserData.termActions.toList.sortBy(_._1).map { entry =>
      val actionEntries = entry._2.map { termAction => // TODO sort
        ArrowBlob(termGroupDesc(termAction._1), parsingAction(termAction._2))
      }
      ArrowBlob(kernelTemplate(entry._1), ArgsCall("List", actionEntries))
    }
    ArgsCall("Map", mapEntries)
  }

  def termGroupDesc(input: TermGroupDesc): ScalaCodeBlobNode = input match {
    case desc: Inputs.CharacterTermGroupDesc =>
      desc match {
        case Inputs.AllCharsExcluding(excluding) =>
          ArgsCall("Inputs.AllCharsExcluding", List(termGroupDesc(excluding)))
        case Inputs.CharsGroup(unicodeCategories, excludingChars, chars) =>
          ArgsCall("Inputs.CharsGroup", List(
            ArgsCall("Set", unicodeCategories.toList.sorted.map(x => TextBlob(x.toString))),
            ArgsCall("Set", excludingChars.toList.sorted.map(CharBlob)),
            ArgsCall("Set", chars.toList.sorted.map(CharBlob))))
      }
    case desc: Inputs.VirtualTermGroupDesc => ???
  }

  def kernelTemplate(template: KernelTemplate): ScalaCodeBlobNode =
    TextBlob(s"t(${template.symbolId}, ${template.pointer})")

  def tasksSummary(summary: TasksSummary): ScalaCodeBlobNode = {
    ArgsCall("TasksSummary", List(
      ArgsCall("List", summary.progressedKernels.map(prog => PairBlob(node(prog._1), acceptCondition(prog._2)))),
      ArgsCall("List", summary.finishedKernels.map(node))))
  }

  def node(node: ParsingContext.Node): ScalaCodeBlobNode = {
    ArgsCall("Node", List(kernel(node.kernel), acceptCondition(node.condition)))
  }

  def kernel(kernel: Kernel): ScalaCodeBlobNode =
    TextBlob(s"k(${kernel.symbolId},${kernel.pointer},${kernel.beginGen},${kernel.endGen})")

  def parsingAction(action: ParsingAction): ScalaCodeBlobNode = ArgsCall("ParsingAction", List(
    ArgsCall("List", action.appendingMilestones.map(appending =>
      PairBlob(kernelTemplate(appending.milestone), acceptCondition(appending.acceptCondition)))),
    tasksSummary(action.tasksSummary),
    ArgsCall("List", action.startNodeProgressConditions.map(acceptCondition)),
    graph(action.graphBetween)
  ))

  def edgeProgressActions(): ScalaCodeBlobNode = {
    val entries = parserData.edgeProgressActions.toList.sortBy(_._1).map { entry =>
      val (start, end) = entry._1
      ArrowBlob(ArrowBlob(kernelTemplate(start), kernelTemplate(end)), parsingAction(entry._2))
    }
    ArgsCall("Map", entries)
  }

  def derivedGraph(): ScalaCodeBlobNode = ArgsCall("Map", parserData.derivedGraph.toList.map(pair =>
    ArrowBlob(kernelTemplate(pair._1), graph(pair._2))))

  def acceptCondition(input: AcceptCondition): ScalaCodeBlobNode = input match {
    case AcceptCondition.Always => TextBlob("Always")
    case AcceptCondition.Never => TextBlob("Never")
    case AcceptCondition.And(conditions) =>
      ArgsCall("And", conditions.toList.map(acceptCondition))
    case AcceptCondition.Or(conditions) =>
      ArgsCall("Or", conditions.toList.map(acceptCondition))
    case AcceptCondition.NotExists(beginGen, endGen, symbolId) =>
      TextBlob(s"NotExists($beginGen, $endGen, $symbolId)")
    case AcceptCondition.Exists(beginGen, endGen, symbolId) =>
      TextBlob(s"Exists($beginGen, $endGen, $symbolId)")
    case AcceptCondition.Unless(beginGen, endGen, symbolId) =>
      TextBlob(s"Unless($beginGen, $endGen, $symbolId)")
    case AcceptCondition.OnlyIf(beginGen, endGen, symbolId) =>
      TextBlob(s"OnlyIf($beginGen, $endGen, $symbolId)")
  }

  def graph(graph: ParsingContext.Graph): ScalaCodeBlobNode =
    ArgsCall("GraphNoIndex", List(ArgsCall("Set", List()), ArgsCall("Set", List())))

  def graph(graph: GraphNoIndex): ScalaCodeBlobNode =
    ArgsCall("GraphNoIndex", List(ArgsCall("Set", List()), ArgsCall("Set", List())))
}

object ScalaParserDataCodeGen {
  val ngrammar: NGrammar = MetaLang3Ast.ngrammar

  def main(args: Array[String]): Unit = {
    val parserData = new MilestoneParserGen(MetaLang3Ast.naiveParser).parserData()
    val codegen = new ScalaParserDataCodeGen(parserData)
    codegen.requiredImports().foreach(i => println(s"import $i"))
    println(codegen.parserData(TextBlob("ngrammar")).generate(0))
    println("***")

    //    val parser = new MilestoneParser(x)
    //    val parsed = parser.parseAndReconstructToForest("[a,a,a,a]").get.trees.head
    //    println(ArrayExprAst.matchStart(parsed))
  }
}
