package com.giyeok.bokparser.visualize

import scala.collection.immutable.HashMap

import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.draw2d.Label
import org.eclipse.draw2d.MouseListener
import org.eclipse.draw2d.OrderedLayout
import org.eclipse.draw2d.ToolbarLayout
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Canvas
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

import com.giyeok.bokparser.AnyCharacterInput
import com.giyeok.bokparser.CharacterRangeInput
import com.giyeok.bokparser.DefItem
import com.giyeok.bokparser.Except
import com.giyeok.bokparser.Grammar
import com.giyeok.bokparser.LookaheadExcept
import com.giyeok.bokparser.Nonterminal
import com.giyeok.bokparser.OneOf
import com.giyeok.bokparser.PoolCharacterInput
import com.giyeok.bokparser.Repeat
import com.giyeok.bokparser.RepeatRangeFrom
import com.giyeok.bokparser.RepeatRangeTo
import com.giyeok.bokparser.Sequence
import com.giyeok.bokparser.StringInput
import com.giyeok.bokparser.UnicodeCategoryCharacterInput
import com.giyeok.bokparser.UnicodeUtil
import com.giyeok.bokparser.VirtualInput
import com.giyeok.bokparser.grammars.SampleGrammar1

object VisualizedGrammar {
	def main(args: Array[String]) {
		val vp = new VisualizedGrammar(SampleGrammar1)

		val display = new Display
		val shell = new Shell(display)

		vp.init(shell)
		shell.setLayout(new FillLayout)

		shell.open()
		while (!shell.isDisposed()) {
			while (!display.readAndDispatch()) {
				display.sleep()
			}
		}
	}
}

class VisualizedGrammar(val grammar: Grammar) {
	def init(parent: Canvas) = {
		val canvas = new FigureCanvas(parent)
		val figure = new GrammarFigure(grammar)

		canvas.setContents(figure)
	}
}

abstract class DefItemFigureListener extends MouseListener

class GrammarFigure(val grammar: Grammar)(implicit itemListener: DefItemFigureListener = null) extends Figure {
	{
		val layout = new ToolbarLayout(false)
		layout.setSpacing(2)
		setLayoutManager(layout)

		for (rule <- grammar.rules)
			add(new RuleFigure(rule))
	}
}

class RuleFigure(val rule: (String, List[DefItem]))(implicit itemListener: DefItemFigureListener = null) extends Figure {
	{
		val layout = new ToolbarLayout(true)
		layout.setSpacing(4)
		setLayoutManager(layout)

		val left = new Figure; val right = new Figure
		val leftLayout = new ToolbarLayout(false)
		val rightLayout = new ToolbarLayout(false)
		leftLayout.setMinorAlignment(OrderedLayout.ALIGN_BOTTOMRIGHT)
		left.setLayoutManager(leftLayout)
		rightLayout.setMinorAlignment(OrderedLayout.ALIGN_TOPLEFT)
		right.setLayoutManager(rightLayout)

		import DefItemFigure._
		def addAll(rhs: List[DefItem]): Unit =
			rhs match {
				case item :: rest => right.add(item); addAll(rest)
				case List() =>
			}

		val boil = new Figure(); boil.setLayoutManager(new ToolbarLayout(true))
		val boilname = new Label(rule._1); boilname.setFont(DefItemFigure.nonterminalFont);
		boil.add(boilname); boil.add(arrowLabel)
		left.add(boil)
		addAll(rule._2)

		add(left); add(right)
	}

	def arrowLabel: Label = {
		val arrow = new Label(" \u2192 ")
		arrow.setFont(RuleFigure.defaultFont)
		arrow
	}
	def barLabel: Label = {
		val bar = new Label("|")
		bar.setFont(RuleFigure.defaultFont)
		bar
	}
}
object RuleFigure {
	val defaultFont = new Font(null, "Arial", 12, SWT.NONE)
}

object DefItemFigure {
	val nonterminalFont = new Font(null, "Arial", 12, SWT.ITALIC)
	val stringFont = new Font(null, "Arial", 12, SWT.BOLD)
	val instFont = new Font(null, "Arial", 12, SWT.NONE)
	val virtFont = new Font(null, "Arial", 12, SWT.BOLD)
	val defaultFont = new Font(null, "Arial", 10, SWT.NONE)

	implicit def defitem2label(defitem: DefItem)(implicit itemListener: DefItemFigureListener = null): Figure = {
		def addWithBars(fig: Figure, items: Seq[DefItem]) = {
			if (!(items isEmpty)) {
				for (i <- items.init) { fig add i; fig add "|" }
				fig add (items.last)
			}
		}
		val fig = defitem match {
			case Nonterminal(name) => label(name, nonterminalFont)
			case StringInput(string) => stringInputLabel(string)
			case AnyCharacterInput() => label("any char", instFont)
			case PoolCharacterInput(chars) => label((escapeChars(chars) toSeq) mkString "|", instFont)
			case UnicodeCategoryCharacterInput(cates) => label(UnicodeUtil.translateToString(cates) mkString "|", instFont)
			case CharacterRangeInput(from, to) => label(escape(from) + ".." + escape(to), instFont)
			case VirtualInput(name) => label(name, virtFont)
			case Sequence(seq, whitespace) => 
				val fig = horzFigure()
				if (seq isEmpty) fig add new Label("\u03B5")
				else seq foreach (fig add _)
				fig
			case OneOf(items) => val fig = horzFigure(); addWithBars(fig, items); fig
			case Except(item, except) =>
				val fig = horzFigure()
				fig add "("; fig add item; fig add ")"
				fig add label("but not", instFont)
				fig add "("; addWithBars(fig, except); fig add ")"
				fig
			case LookaheadExcept(except) =>
				val fig = horzFigure()
				fig add "Lookahead not in ("; addWithBars(fig, except); fig add ")"
				fig
			case Repeat(item, range) => {
				val fig = horzFigure()
				fig add "("; fig add item; fig add ")"
				range match {
					case RepeatRangeTo(0, 1) => fig add "?"
					case RepeatRangeTo(from, to) => fig add ("[" + from + ".." + to + "]")
					case RepeatRangeFrom(0) => fig add "*"
					case RepeatRangeFrom(1) => fig add "+"
					case RepeatRangeFrom(from) => fig add ("[" + from + "..]")
				}
				fig
			}
		}
		if (itemListener != null) fig.addMouseListener(itemListener)
		fig
	}
	def horzFigure(spacing: Int = 4) = {
		val layout = new ToolbarLayout(true); layout.setSpacing(spacing)
		val fig = new Figure(); fig.setLayoutManager(layout)
		fig
	}
	implicit def string2label(s: String): Label = label(s, defaultFont)
	
	def stringInputLabel(string: String) = label(escape(string), stringFont)

	def escmap = HashMap(
		'\n' -> "\\n",
		'\t' -> "\\t",
		'\r' -> "\\r",
		'\u000B' -> "000B",
		'\u000C' -> "000C",
		'\u0020' -> "0020",
		'\u2028' -> "2028",
		'\u2029' -> "2029",
		'\u00A0' -> "00A0",
		'\uFEFF' -> "FEFF")
	def escape(char: Char): String = (escmap get char) match {
		case Some(v) => v
		case None => String valueOf char
	}
	def escape(str: String): String = (str map (escape _)) mkString

	def escapeChars(chars: Seq[Char]) =
		for (c <- chars) yield escape(c)

	def label(text: String, font: Font) = {
		val k = new Label(text)
		k.setFont(font)
		k
	}
}
