package com.giyeok.bokparser.visualize

import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.draw2d.Label
import org.eclipse.draw2d.LineBorder
import org.eclipse.draw2d.MouseEvent
import org.eclipse.draw2d.MouseMotionListener
import org.eclipse.draw2d.ToolbarLayout
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

import com.giyeok.bokparser.BlackboxParser
import com.giyeok.bokparser.CharInputSymbol
import com.giyeok.bokparser.EOFSymbol
import com.giyeok.bokparser.EmptySymbol
import com.giyeok.bokparser.Grammar
import com.giyeok.bokparser.NontermSymbol
import com.giyeok.bokparser.Nonterminal
import com.giyeok.bokparser.ParseSuccess
import com.giyeok.bokparser.Parser
import com.giyeok.bokparser.ParserInput
import com.giyeok.bokparser.StackSymbol
import com.giyeok.bokparser.StartSymbol
import com.giyeok.bokparser.TermSymbol
import com.giyeok.bokparser.VirtInputSymbol
import com.giyeok.bokparser.grammars.JavaScriptGrammar

object VisualizedStackSymbol {
	def main(args: Array[String]) {
		val display = new Display
		val shell = new Shell(display)

		val program = 
		"""
function click_expandingMenuHeader(obj,sectionName)
{
var x=document.getElementById("cssprop_" + sectionName).parentNode.className;
if (x.indexOf("expandingMenuNotSelected")>-1)
	{
	x=x.replace("expandingMenuNotSelected","expandingMenuSelected");
	document.getElementById("cssprop_" + sectionName).parentNode.className=x;
	document.getElementById("cssprop_" + sectionName).style.display="block";
	}
else
	{
	x=x.replace("expandingMenuSelected","expandingMenuNotSelected");
	document.getElementById("cssprop_" + sectionName).parentNode.className=x;
	document.getElementById("cssprop_" + sectionName).style.display="none";
	}
}
		"""
		val result = new BlackboxParser(JavaScriptGrammar).parse(ParserInput.fromString(program))

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

		shell.setText(program)
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
class VisualizedStackSymbol(val stackSymbol: StackSymbol, val whitespace: Boolean = true, val grammar: Grammar = null, borderColor: Color = ColorConstants.darkGray) extends Figure {
	{
		val _layout = new ToolbarLayout(false)

		val whitespaceBoxColor = ColorConstants.blue
		val normalBoxColor = ColorConstants.darkGray

		stackSymbol match {
			case StartSymbol => add(new Label("$"))
			case NontermSymbol(item) =>
				item.item match {
					case Nonterminal(name) =>
						val nonterm = new Label(name)
						if (grammar != null) {
							nonterm.setToolTip(new RuleFigure((name, grammar.rules(name))))
						}
						add(nonterm)
					case _ =>
				}

				val fig = new Figure
				fig.setLayoutManager({
					val layout = new ToolbarLayout(true)
					layout.setSpacing(2)
					layout
				})
				fig.setBorder(null)

				if (whitespace && (item.isInstanceOf[Parser#StackEntry#ParsingSequence])) {
					val s = item.asInstanceOf[Parser#StackEntry#ParsingSequence]
					def x(children: List[StackSymbol], indices: List[Int], pointer: Int): Unit =
						indices match {
							case i :: is =>
								if (pointer == i) {
									fig.add(new VisualizedStackSymbol(children.head, whitespace, grammar, normalBoxColor))
									x(children.tail, is, pointer + 1)
								} else {
									fig.add(new VisualizedStackSymbol(children.head, whitespace, grammar, whitespaceBoxColor))
									x(children.tail, indices, pointer + 1)
								}
							case _ =>
								if (!(children isEmpty)) {
									fig.add(new VisualizedStackSymbol(children.head, whitespace, grammar, whitespaceBoxColor))
									x(children.tail, indices, pointer + 1)
								}
						}
					x(s.childrenWithWS, s.indexNonWS, 0)
				} else {
					item.children.foreach((x) => fig.add(new VisualizedStackSymbol(x, whitespace, grammar, normalBoxColor)))
				}

				_layout.setSpacing(2)
				add(fig)
			case TermSymbol(input, pointer) =>
				input match {
					case CharInputSymbol(char) => add(new Label(s"'$char'"))
					case VirtInputSymbol(name) => add(new Label(name))
					case EOFSymbol => add(new Label("$"))
				}
			case EmptySymbol => add(new Label("()"))
		}

		setLayoutManager(_layout)

		// set border for nonterminals
		stackSymbol match {
			case NontermSymbol(item) =>
				item.item match {
					case _: Nonterminal =>
						val highlightedBorder = new LineBorder(ColorConstants.red, 1)
						val normalBorder = new LineBorder(borderColor, 1)
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
					case _ =>
				}
			case _ =>
		}
	}
}
