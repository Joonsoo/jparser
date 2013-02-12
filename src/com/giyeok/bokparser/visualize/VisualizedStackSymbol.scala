package com.giyeok.bokparser.visualize

import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.Label
import org.eclipse.draw2d.LineBorder
import org.eclipse.draw2d.ToolbarLayout
import com.giyeok.bokparser.CharInputSymbol
import com.giyeok.bokparser.EOFSymbol
import com.giyeok.bokparser.EmptySymbol
import com.giyeok.bokparser.NontermSymbol
import com.giyeok.bokparser.Nonterminal
import com.giyeok.bokparser.StackSymbol
import com.giyeok.bokparser.StartSymbol
import com.giyeok.bokparser.TermSymbol
import com.giyeok.bokparser.VirtInputSymbol
import com.giyeok.bokparser.grammars.JavaScriptGrammar
import com.giyeok.bokparser.ParserInput
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Display
import com.giyeok.bokparser.Parser
import com.giyeok.bokparser.BlackboxParser
import org.eclipse.draw2d.FigureCanvas
import com.giyeok.bokparser.ParseSuccess
import org.eclipse.swt.layout.FillLayout
import org.eclipse.draw2d.MouseMotionListener
import org.eclipse.draw2d.MouseEvent
import com.giyeok.bokparser.Grammar

object VisualizedStackSymbol {
	def main(args: Array[String]) {
		val display = new Display
		val shell = new Shell(display)

		val result = new BlackboxParser(JavaScriptGrammar).parse(ParserInput.fromString("  var q    =  co  + 1,  x  = 321.5e-71;"))

		val figure = new Figure
		val layout = new ToolbarLayout(false)
		layout.setSpacing(5)
		figure.setLayoutManager(layout)

		result.messages.foreach((x) => x match {
			case ParseSuccess(parsed) =>
				figure add new VisualizedStackSymbol(parsed, true, JavaScriptGrammar)
			case _ =>
		})

		val canvas = new FigureCanvas(shell)
		canvas.setContents(figure)

		shell.setLayout(new FillLayout)
		shell.open()

		while (!shell.isDisposed()) {
			while (!display.readAndDispatch()) {
				display.sleep()
			}
		}
	}
}

// Show tree structure of given StackSymbol visually
class VisualizedStackSymbol(val stackSymbol: StackSymbol, val whitespace: Boolean = false, val grammar: Grammar = null) extends Figure {
	private val _layout = new ToolbarLayout(false)

	stackSymbol match {
		case StartSymbol => add(new Label("$"))
		case NontermSymbol(item) =>
			def layoutManager(spacing: Int = 2) = {
				val layout = new ToolbarLayout(true)
				layout.setSpacing(spacing)
				layout
			}

			item.item match {
				case Nonterminal(name, _, _) =>
					val nonterm = new Label(name)
					if (grammar != null) {
						nonterm.setToolTip(new RuleFigure((name, grammar.rules(name))))
					}
					add(nonterm)
				case _ =>
			}

			val children = new Figure
			children.setLayoutManager(layoutManager())

			if (whitespace) {
				val precedingWS = new Figure
				precedingWS.setLayoutManager(layoutManager())
				precedingWS.setBorder(new LineBorder(ColorConstants.gray, 1))
				item.precedingWS.foreach((x) => precedingWS.add(new VisualizedStackSymbol(x, whitespace, grammar)))
				children add precedingWS
			}

			val content = new Figure
			content.setLayoutManager(layoutManager())
			content.setBorder(new LineBorder(ColorConstants.darkGray, 1))
			item.children.foreach((x) => content.add(new VisualizedStackSymbol(x, whitespace, grammar)))
			children add content

			if (whitespace) {
				val followingWS = new Figure
				followingWS.setLayoutManager(layoutManager())
				followingWS.setBorder(new LineBorder(ColorConstants.gray, 1))
				item.followingWS.foreach((x) => followingWS.add(new VisualizedStackSymbol(x, whitespace, grammar)))
				children add followingWS
			}

			_layout.setSpacing(2)
			add(children)
		case TermSymbol(input, pointer) =>
			input match {
				case CharInputSymbol(char) => add(new Label(s"'$char'"))
				case VirtInputSymbol(name) => add(new Label(name))
				case EOFSymbol => add(new Label("$"))
			}
		case EmptySymbol => add(new Label("()"))
	}

	setLayoutManager(_layout)

	private val highlightedBorder = new LineBorder(ColorConstants.red, 1)
	private val normalBorder = new LineBorder(ColorConstants.black, 1)
	setBorder(normalBorder)

	addMouseMotionListener(new MouseMotionListener {
		def mouseDragged(e: MouseEvent) {}

		def mouseEntered(e: MouseEvent) {
			setBorder(highlightedBorder)
		}

		def mouseExited(e: MouseEvent) {
			setBorder(normalBorder)
		}

		def mouseHover(e: MouseEvent) {}

		def mouseMoved(e: MouseEvent) {}
	})
}
