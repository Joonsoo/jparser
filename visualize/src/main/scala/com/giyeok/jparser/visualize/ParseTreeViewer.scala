package com.giyeok.jparser.visualize

import com.giyeok.jparser.ParseForest
import org.eclipse.draw2d.{ColorConstants, Figure, FigureCanvas}
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite

class ParseTreeViewer(parent: Composite, style: Int) extends Composite(parent, style) {
  setLayout(new FillLayout)

  val figureCanvas = new FigureCanvas(this, SWT.NONE)

  val figure = new org.eclipse.draw2d.Label("Parse Tree")
  figureCanvas.setContents(figure)

  val parseResultFigureGenerator = new ParseResultFigureGenerator[Figure](BasicVisualizeResources.nodeFigureGenerators.fig, BasicVisualizeResources.nodeFigureGenerators.appear)

  def setParseForest(parseForest: ParseForest): Unit = getDisplay.asyncExec { () =>
    // TODO figure 모양 개선(세로형으로)
    // TODO parse tree 안에 마우스 갖다대면 testText에 표시해주기
    figureCanvas.setContents(parseResultFigureGenerator.parseResultFigure(parseForest))
    figureCanvas.setBackground(ColorConstants.white)
  }

  def invalidateParseForest(): Unit = getDisplay.asyncExec { () =>
    figureCanvas.setBackground(ColorConstants.lightGray)
  }
}
