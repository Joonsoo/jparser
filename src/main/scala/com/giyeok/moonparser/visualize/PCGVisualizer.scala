package com.giyeok.moonparser.visualize

import com.giyeok.moonparser.Grammar
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.layout.FillLayout
import com.giyeok.moonparser.visualize.utils.HorizontalResizableSplittedComposite
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.widgets.Button
import com.giyeok.moonparser.visualize.utils.VerticalResizableSplittedComposite
import com.giyeok.moonparser.Parser
import org.eclipse.swt.widgets.Control
import org.eclipse.zest.core.widgets.Graph
import org.eclipse.swt.widgets.Composite
import org.eclipse.zest.core.widgets.CGraphNode
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.LineBorder
import org.eclipse.draw2d.ColorConstants
import com.giyeok.moonparser.visualize.utils.VerticalResizableSplittedComposite
import org.eclipse.zest.layouts.LayoutStyles
import org.eclipse.zest.core.widgets.GraphNode

class PCGVisualizer(grammar: Grammar, parent: Composite, resources: ParseGraphVisualizer.Resources) {
    val parser = new Parser(grammar)

    val figureGenerator: FigureGenerator.Generator[Figure] = FigureGenerator.draw2d.Generator
    val figureAppearances = new FigureGenerator.Appearances[Figure] {
        val default = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 10, SWT.NONE), ColorConstants.black)
        val nonterminal = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.BOLD), ColorConstants.blue)
        val terminal = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.NONE), ColorConstants.red)

        override val small = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 8, SWT.NONE), ColorConstants.gray)
        override val kernelDot = FigureGenerator.draw2d.FontAppearance(new Font(null, "Monospace", 12, SWT.NONE), ColorConstants.green)
        override val symbolBorder = FigureGenerator.draw2d.BorderAppearance(new LineBorder(ColorConstants.lightGray))
    }

    val panel = new HorizontalResizableSplittedComposite(parent, SWT.NONE, 20)
    val upper = new VerticalResizableSplittedComposite(panel.upperPanel, SWT.NONE, 70)
    val lower = new VerticalResizableSplittedComposite(panel.lowerPanel, SWT.NONE, 70)

    class ParsingContextExpansionView(parent: Composite, style: Int) {
        parent.setLayout(new FillLayout())
        val graph = new Graph(parent, style)

        val (g, a) = (figureGenerator, figureAppearances)
        val nodeFig = g.horizontalFig(FigureGenerator.Spacing.None, Seq(g.textFig("PC", a.default), g.subFig(g.textFig("0", a.default))))
        nodeFig.setBorder(new LineBorder(ColorConstants.darkGray))
        nodeFig.setBackgroundColor(ColorConstants.buttonLightest)
        nodeFig.setOpaque(true)
        nodeFig.setSize(nodeFig.getPreferredSize())
        // nodeFig.setToolTip(newSymbolProgressContentFig(n, true, ParseNodeFigureGenerator.cleanestConfiguration))

        new CGraphNode(graph, SWT.NONE, nodeFig)

        import org.eclipse.zest.layouts.algorithms._
        val layoutAlgorithm = new HorizontalTreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS)
        graph.setLayoutAlgorithm(layoutAlgorithm, true)
    }

    val expansionGraph = new ParsingContextExpansionView(upper.leftPanel, SWT.NONE)
    val kernelsGraph = new Button(upper.rightPanel, SWT.NONE)
    val pcGraph = new Button(lower.leftPanel, SWT.NONE)
    val expansionList = new Button(lower.rightPanel, SWT.NONE)
    /*
    case 'y' | 'Y' =>
        graphAt(currentLocation) match {
            case v: ParsingContextGraphVisualizeWidget =>
                import com.giyeok.moonparser.Inputs._
                val termGroup = v.context.termGroupsForTerminals.toSeq(1)
                val abstractInput = AbstractInput(termGroup)
                println(s"Proceeding with ${termGroup.toShortString}")
                v.context.proceedTerminalVerbose(abstractInput) match {
                    case Left((newCtx, log)) =>
                        val newShell = new Shell(Display.getDefault())
                        newShell.setLayout(new FillLayout())
                        new ParsingContextGraphVisualizeWidget(newShell, resources, newCtx)
                        newShell.open()

                        val newShell2 = new Shell(Display.getDefault())
                        newShell2.setLayout(new FillLayout())
                        new ParsingContextProceedVisualizeWidget(newShell2, resources, Some(v.context), log)
                        newShell2.open()
                    case Right(error) => println(error)
                }
        }
    */
}

object PCGVisualizer {
    def start(grammar: Grammar, display: Display, shell: Shell): Unit = {
        val resources = new ParseGraphVisualizer.Resources {
            val defaultFontName = "Consolas"
            val default12Font = new Font(null, defaultFontName, 12, SWT.NONE)
            val fixedWidth12Font = new Font(null, defaultFontName, 12, SWT.NONE)
            val italic14Font = new Font(null, defaultFontName, 14, SWT.ITALIC)
            val bold14Font = new Font(null, defaultFontName, 14, SWT.BOLD)
        }
        shell.setLayout(new FillLayout)
        new PCGVisualizer(grammar, shell, resources)
        shell.open()
    }
}
