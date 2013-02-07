package com.giyeok.bokparser.visualize

import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.Label
import org.eclipse.draw2d.ToolbarLayout
import com.giyeok.bokparser.CharInputSymbol
import com.giyeok.bokparser.EmptySymbol
import com.giyeok.bokparser.EOFSymbol
import com.giyeok.bokparser.NontermSymbol
import com.giyeok.bokparser.StackSymbol
import com.giyeok.bokparser.StartSymbol
import com.giyeok.bokparser.TermSymbol
import com.giyeok.bokparser.VirtInputSymbol
import com.giyeok.bokparser.Nonterminal

object VisualizedStackSymbol {
	def main(args: Array[String]) {
		// parse some string and show the result as tree
	}
}

// Show tree structure of given StackSymbol visually
class VisualizedStackSymbol(val stackSymbol: StackSymbol) extends Figure {
	private def stringify(symbol: StackSymbol): String = symbol match {
		case StartSymbol => "$"
		case NontermSymbol(item) => item.item match {
			case Nonterminal(name, _, _) => name + "(" + ((item.children map (stringify _)) mkString ", ") + ")"
			case _ => "(" + ((item.children map (stringify _)) mkString ", ") + ")"
		}
		case TermSymbol(term, pointer) => term match {
			case CharInputSymbol(char) => s"$char"
			case VirtInputSymbol(name) => name
			case EOFSymbol => "$"
		}
		case EmptySymbol => "."
	}

	val repr = stringify(stackSymbol)
	setLayoutManager(new ToolbarLayout)

	add(new Label(repr))
	add(new Label(stackSymbol.text))
}
