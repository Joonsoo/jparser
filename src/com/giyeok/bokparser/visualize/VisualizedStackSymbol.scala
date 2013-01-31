package com.giyeok.bokparser.visualize
import com.giyeok.bokparser.StackSymbol
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.Label
import com.giyeok.bokparser.StartSymbol
import com.giyeok.bokparser.NontermSymbol
import com.giyeok.bokparser.TermSymbol
import com.giyeok.bokparser.CharInputSymbol
import com.giyeok.bokparser.VirtInputSymbol
import com.giyeok.bokparser.EOFSymbol
import org.eclipse.draw2d.ToolbarLayout

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
