package com.giyeok.bokparser.visualize

import scala.collection.mutable.HashMap

import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.draw2d.IFigure
import org.eclipse.draw2d.Label
import org.eclipse.draw2d.LineBorder
import org.eclipse.draw2d.MouseEvent
import org.eclipse.draw2d.MouseListener
import org.eclipse.draw2d.ToolbarLayout
import org.eclipse.draw2d.XYLayout
import org.eclipse.draw2d.geometry.Rectangle
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Canvas
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

import com.giyeok.bokparser.CharInputSymbol
import com.giyeok.bokparser.DefItem
import com.giyeok.bokparser.EOFSymbol
import com.giyeok.bokparser.EmptySymbol
import com.giyeok.bokparser.Grammar
import com.giyeok.bokparser.NontermSymbol
import com.giyeok.bokparser.Nonterminal
import com.giyeok.bokparser.ParserInput
import com.giyeok.bokparser.StackSymbol
import com.giyeok.bokparser.StartSymbol
import com.giyeok.bokparser.StringInput
import com.giyeok.bokparser.TermSymbol
import com.giyeok.bokparser.TokenInputSymbol
import com.giyeok.bokparser.VirtInputSymbol
import com.giyeok.bokparser.dynamic.Parser
import com.giyeok.bokparser.grammars.JavaScriptGrammar
import com.giyeok.bokparser.grammars.JavaScriptParser
import com.giyeok.bokparser.tests.JavaScriptTestCases

object VisualizedDynamicParser {
	def main(args: Array[String]) {
		val display = new Display
		val shell = new Shell(display)

		val program = JavaScriptTestCases.test
		val vp = new VisualizedDynamicParser(JavaScriptGrammar, JavaScriptParser.getTokenizer(ParserInput.fromString(program)), shell)

		shell.setText(program)
		shell.setLayout(new FillLayout)

		shell.open()

		vp.canvas.addKeyListener(new KeyListener {
			def keyPressed(e: KeyEvent) = {
				e.keyCode match {
					case '\r' =>
						if (!vp.proceed()) {
							println("Parsing finished")
						}
					case 'a' =>
						var counter = 0
						while (vp.proceed() && counter < 1000) (counter += 1)
						println("Parsing finished")
						println(vp.result.messages.length)
					case _ =>
				}
			}
			def keyReleased(e: KeyEvent) = {}
		})
		while (!shell.isDisposed()) {
			while (!display.readAndDispatch()) {
				display.sleep()
			}
		}
	}
}

class VisualizedDynamicParser(grammar: Grammar, input: ParserInput, parent: Canvas) extends Parser(grammar, input) {
	// === Preserving stack ===
	class PreservingOctopusStack(starter: StackEntry) extends OctopusStack(starter) {
		protected var all = List[StackEntry](bottom)
		protected var done = List[StackEntry]()

		override def add(entry: StackEntry) = {
			all ::= entry
			super.add(entry)
		}
		override def pop() = {
			done ::= top
			super.pop()
		}
		def getAll = all
		def getDone = done

		val parser = VisualizedDynamicParser.this
	}

	trait ChildrenMap extends PreservingOctopusStack {
		def getChildrenOf(parent: StackEntry) =
			for (entry <- all if entry.parent == parent) yield entry
	}

	trait HashedChildrenMap extends PreservingOctopusStack with ChildrenMap {
		import scala.collection.mutable.HashMap

		private val childrenMap = new HashMap[StackEntry, List[StackEntry]]()

		override def add(entry: StackEntry) = {
			if (entry.parent != null) {
				(childrenMap get entry.parent) match {
					case Some(l) => childrenMap(entry.parent) = l ::: List(entry)
					case None => childrenMap += entry.parent -> List(entry)
				}
			}
			super.add(entry)
		}
		override def getChildrenOf(parent: StackEntry) =
			(childrenMap get parent) match { case Some(v) => v case _ => List() }
	}
	override val stack = new PreservingOctopusStack(starter) with HashedChildrenMap

	// === Visualization ===
	implicit val self = this

	val canvas = new FigureCanvas(parent)
	val figure = new StackFigure(this)

	canvas.setContents(figure)

	def proceed() = if (parseStep()) { figure.updateTree(); true } else false

	def openGrammar(nonterm: Nonterminal) = {
		// TODO
	}
}

class StackFigure(val parser: VisualizedDynamicParser)(implicit val vp: VisualizedDynamicParser) extends Figure {
	import scala.collection.mutable.HashMap

	private val figureMap = new HashMap[Parser#StackEntry, StackEntryFigure]

	setLayoutManager(new TreeLayouter)

	private def addAllFigures(entries: List[Parser#StackEntry]) = {
		for (entry <- entries if (!(figureMap contains entry))) {
			val figure = new StackEntryFigure(this, entry)
			add(figure)
			setConstraint(figure, new Rectangle(10, 10, -1, -1))
			figureMap += entry -> figure
		}
	}

	def updateTree() {
		layout()
	}

	private val stack = parser.stack

	class TreeLayouter extends XYLayout {
		override def layout(x: IFigure) = {
			super.layout(x)
			updateTree()
			super.layout(x)
		}
		def updateTree() = {
			def layoutNode(node: parser.StackEntry, left: Int, top: Int): Int = {
				val children = stack.getChildrenOf(node)
				val figure = figureMap(node)
				val size = figure.getPreferredSize()

				if (stack.hasNext && node == stack.top) {
					figure.setBackgroundColor(StackEntryFigure.nextColor)
				} else if (stack.getDone contains node) {
					figure.setBackgroundColor(StackEntryFigure.doneColor)
				} else {
					figure.setBackgroundColor(StackEntryFigure.backgroundColor)
				}

				if (children.isEmpty) return size.height
				var (margin_x, margin_y) = (20, 15)
				var (x, y) = (left + size.width + margin_x, top)

				for (child <- children) {
					val childFigure = figureMap(child)

					setConstraint(childFigure, new Rectangle(x, y, -1, -1))
					y += layoutNode(child, x, y) + margin_y
				}
				size.height max (y - top)
			}
			addAllFigures(stack.getAll)
			layoutNode(stack.bottom, 10, 10)
		}
	}
}

object StackEntryFigure {
	val doneColor = ColorConstants.lightGray
	val nextColor = ColorConstants.lightBlue
	val backgroundColor = new Color(null, 255, 255, 206)
}
class StackEntryFigure(val stackFigure: StackFigure, val stackEntry: Parser#StackEntry)(implicit val vp: VisualizedDynamicParser) extends Figure {
	private val stackSymbolFigure = new StackSymbolFigure(stackEntry.symbol)
	private val stackEntryItemsFigure = new StackEntryItemsFigure(stackEntry.items)
	private var expanded = false

	{
		val layout = new ToolbarLayout
		layout.setStretchMinorAxis(false)
		layout.setSpacing(2)
		setLayoutManager(layout)

		setBorder(new LineBorder(ColorConstants.black, 1))
		setBackgroundColor(StackEntryFigure.backgroundColor)
		setOpaque(true)

		var string = stackEntry.id + "@" + stackEntry.pointer
		if (stackEntry.parent != null) {
			string += " from " + stackEntry.parent.id
		}
		if (stackEntry.generatedFrom != null) {
			string += " genfrom " + stackEntry.generatedFrom.id
		}
		if (stackEntry.generatedFromItem != null) {
			string += "#" + stackEntry.generatedFromItem.id
		}
		add(new Label(string))

		add(stackSymbolFigure)
		addMouseListener(new MouseListener {
			def mouseReleased(e: MouseEvent) {}
			def mousePressed(e: MouseEvent) {
				toggle
				getParent().invalidate()
			}
			def mouseDoubleClicked(e: MouseEvent) {}
		})
	}

	def toggle = if (expanded) collapse else expand
	def expand = if (!expanded) {
		add(stackEntryItemsFigure)
		expanded = true
		layout()
		stackFigure.updateTree()
	}
	def collapse = if (expanded) {
		remove(stackEntryItemsFigure)
		expanded = false
		layout()
		stackFigure.updateTree
	}
}

class StackEntryItemsFigure(val items: List[Parser#StackEntry#StackEntryItem])(implicit val vp: VisualizedDynamicParser) extends Figure {
	{
		val layout = new ToolbarLayout(false)
		setLayoutManager(layout)

		for (item <- items) add(new StackEntryItemFigure(item))
	}
}

class StackEntryItemFigure(val entryItem: Parser#StackEntry#StackEntryItem)(implicit val vp: VisualizedDynamicParser) extends Figure {
	private val derivedFrom = entryItem.derivedFrom map (_.id) mkString (", ")

	{
		val layout = new ToolbarLayout(true)
		setLayoutManager(layout)

		val item = entryItem.item
		if (entryItem.derivedFrom.isEmpty) {
			add(new Label(s"${entryItem.id}:"))
		} else {
			add(new Label(s"${entryItem.id}[$derivedFrom]:"))
		}
		addStateDefItemLabel(item)

		setToolTip(new Label(s"${item.item.id}"))
	}

	def addStateDefItemLabel(item: Parser#StackEntry#ParsingItem): Unit = {
		val labelize = DefItemFigure.defitem2label _
		item match {
			// COMMENT Expect someday Scala supports for type matching on dependent types
			case sn: Parser#StackEntry#ParsingNonterminal =>
				val fig = labelize(sn.item)
				val tooltip = new Figure()
				tooltip.setLayoutManager(new ToolbarLayout(false))
				tooltip.add(new Label(s"${sn.item.id}"))
				tooltip.add(new RuleFigure((sn.item.name, vp.grammar.rules(sn.item.name))))
				fig.setToolTip(tooltip)
				if (!sn.done) add(dotLabel)
				add(fig)
				if (sn.done) add(dotLabel)
			case si: Parser#StackEntry#ParsingStringInput =>
				add(DefItemFigure.stringInputLabel(si.item.string.substring(0, si.pointer)))
				add(dotLabel)
				add(DefItemFigure.stringInputLabel(si.item.string.substring(si.pointer)))
			case seq: Parser#StackEntry#ParsingSequence =>
				def addLabels(l: List[DefItem]): Unit = l match {
					case x :: xs =>
						add(labelize(x)); addLabels(xs)
					case Nil =>
				}
				val (f, b) = seq.item.seq splitAt seq.pointer
				addLabels(f)
				add(dotLabel)
				addLabels(b)
				if ((!b.isEmpty) && seq.finishable)
					add(dotLabel)
			case rep: Parser#StackEntry#ParsingRepeat =>
				if (rep.item.range canProceed rep.count) add(dotLabel)
				add(labelize(rep.item))
				if (rep.finishable) add(dotLabel)
			case _: Parser#StackEntry#ParsingInput | _: Parser#StackEntry#ParsingOneOf =>
				if (!item.finishable) add(dotLabel)
				add(labelize(item.item))
				if (item.finishable) add(dotLabel)
			case exc: Parser#StackEntry#ParsingExcept =>
				if (!item.finishable) add(dotLabel)
				add(labelize(exc.item.item))
				add(new Label(" except "))
				for (i <- exc.item.except)
					add(labelize(i))
				if (item.finishable) add(dotLabel)
			case _ =>
		}
	}

	def dotLabel = {
		val lab = new Label("\u2022")
		lab.setFont(DefItemFigure.defaultFont)
		lab
	}
}

class StackSymbolFigure(val symbol: StackSymbol)(implicit val vp: VisualizedDynamicParser) extends Figure {
	{
		val layout = new ToolbarLayout(false)
		setLayoutManager(layout)

		val (repr, font) = symbol match {
			case StartSymbol => ("$", DefItemFigure.stringFont)
			case NontermSymbol(item) => item.item match {
				case Nonterminal(name) => (name, DefItemFigure.nonterminalFont)
				case StringInput(string) => (string, DefItemFigure.stringFont)
				case _ => ("<<" + item.item.id + ">>", DefItemFigure.defaultFont)
			}
			case TermSymbol(input, pointer) => input match {
				case CharInputSymbol(char) => (s"$char at $pointer", DefItemFigure.stringFont)
				case VirtInputSymbol(virt) => (s"$virt at $pointer", DefItemFigure.virtFont)
				case TokenInputSymbol(token) => (s"${token.text} at $pointer", DefItemFigure.tokenFont)
				case EOFSymbol => ("$", DefItemFigure.defaultFont) // should not be here
			}
			case EmptySymbol(_) => (".", DefItemFigure.defaultFont) // empty
		}
		val symbolLabel = new Label(repr)
		symbolLabel.setFont(DefItemFigure.defaultFont)
		symbolLabel.setToolTip(new VisualizedStackSymbol(symbol))
		add(symbolLabel)

		addMouseListener(new MouseListener {
			def mousePressed(e: MouseEvent) = {}
			def mouseReleased(e: MouseEvent) = {}
			def mouseDoubleClicked(e: MouseEvent) = {
				val shell = new Shell(Display.getCurrent())

				val canvas = new FigureCanvas(shell)
				canvas.setContents(new VisualizedStackSymbol(symbol, true, vp.grammar))

				shell.setLayout(new FillLayout)
				shell.open()
			}
		})
	}
}
