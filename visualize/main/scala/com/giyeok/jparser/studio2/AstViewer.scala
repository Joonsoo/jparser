package com.giyeok.jparser.studio2

import com.giyeok.jparser.metalang3.ValuefyExprSimulator
import com.giyeok.jparser.visualize.FigureGenerator.Spacing
import com.giyeok.jparser.visualize.{BasicVisualizeResources, FigureGenerator, ParseResultFigureGenerator, SymbolFigureGenerator}
import org.eclipse.draw2d.{ColorConstants, Figure, FigureCanvas}
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite

class AstViewer(parent: Composite, style: Int) extends Composite(parent, style) {
  setLayout(new FillLayout)

  val figureCanvas = new FigureCanvas(this, SWT.NONE)

  val figure = new org.eclipse.draw2d.Label("Abstract Syntax Tree")
  figureCanvas.setContents(figure)

  val astValueFigureGenerator = new AstValueFigureGenerator[Figure](BasicVisualizeResources.nodeFigureGenerators.fig, BasicVisualizeResources.nodeFigureGenerators.appear)

  def setAstValues(values: List[ValuefyExprSimulator.Value]): Unit = getDisplay.asyncExec { () =>
    // TODO figure 모양 개선(세로형으로)
    // TODO parse tree 안에 마우스 갖다대면 testText에 표시해주기
    figureCanvas.setContents(astValueFigureGenerator.g.verticalFig(Spacing.Big, values.map(astValueFigureGenerator.astValueFigure)))
    figureCanvas.setBackground(ColorConstants.white)
  }

  def invalidateAstValues(): Unit = getDisplay.asyncExec { () =>
    figureCanvas.setBackground(ColorConstants.lightGray)
  }
}

class AstValueFigureGenerator[Fig](figureGenerator: FigureGenerator.Generator[Fig], appearances: FigureGenerator.Appearances[Fig]) {
  val (g, ap) = (figureGenerator, appearances)
  val symbolFigureGenerator = new SymbolFigureGenerator(figureGenerator, appearances)

  def intersperse[T](xs: List[T], item: => T): List[T] = xs match {
    case List() | List(_) => xs
    case head +: tail => head +: item +: intersperse(tail, item)
  }

  def astValueFigure(astValue: ValuefyExprSimulator.Value): Fig = astValue match {
    case ValuefyExprSimulator.NodeValue(astNode) =>
      g.textFig(astNode.sourceText, ap.default)
    case ValuefyExprSimulator.ClassValue(className, args) =>
      val argsFig = intersperse(args.map(astValueFigure), g.textFig(",", ap.small))
      g.horizontalFig(
        Spacing.Small,
        List(g.textFig(className, ap.nonterminal),
          g.textFig("(", ap.small)) ++ argsFig ++ List(g.textFig(")", ap.small)))
    case ValuefyExprSimulator.ArrayValue(elems) =>
      val elemsFig = intersperse(elems.map(astValueFigure), g.textFig(",", ap.small))
      g.horizontalFig(Spacing.Small,
        List(g.textFig("[", ap.small)) ++ elemsFig ++ List(g.textFig("]", ap.small)))
    case ValuefyExprSimulator.EnumValue(enumType, enumValue) => g.textFig(s"%$enumType.$enumValue", ap.terminal)
    case ValuefyExprSimulator.NullValue => g.textFig("null", ap.terminal)
    case ValuefyExprSimulator.BoolValue(value) => g.textFig(s"$value", ap.terminal)
    case ValuefyExprSimulator.CharValue(value) => g.textFig(s"$value", ap.terminal)
    case ValuefyExprSimulator.StringValue(value) => g.textFig("\"" + value + "\"", ap.terminal)
  }
}
