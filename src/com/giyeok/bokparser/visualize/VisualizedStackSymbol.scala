package com.giyeok.bokparser.visualize

import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.Label
import org.eclipse.draw2d.ToolbarLayout

import com.giyeok.bokparser.CharInputSymbol
import com.giyeok.bokparser.EOFSymbol
import com.giyeok.bokparser.NontermSymbol
import com.giyeok.bokparser.StackSymbol
import com.giyeok.bokparser.StartSymbol
import com.giyeok.bokparser.TermSymbol
import com.giyeok.bokparser.VirtInputSymbol

object VisualizedStackSymbol {
	def main(args: Array[String]) {
		// parse some string and show the result as tree
	}
}

// Show tree structure of given StackSymbol visually
class VisualizedStackSymbol(val stackSymbol: StackSymbol) extends Figure {
	def stringify(symbol: StackSymbol): String = symbol match {
		case StartSymbol => "$"
		case NontermSymbol(_, containing) => "(" + ((containing map (stringify _)) mkString ", ") + ")"
		case TermSymbol(term, pointer) => term match {
			case CharInputSymbol(char) => s"$char"
			case VirtInputSymbol(name) => name
			case EOFSymbol => "$"
		}
	}
	{
		setLayoutManager(new ToolbarLayout)

		val repr = stringify(stackSymbol)
		add(new Label(repr))
	}
}
