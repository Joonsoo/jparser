package com.giyeok.moonparser

object Inputs {
    type Location = Int

    sealed trait Input
    case class Character(char: Char, location: Location) extends Input
    case class Virtual(name: String, location: Location) extends Input
    case class AbstractInput(chars: CharGroupDescription) extends Input

    trait CharGroupDescription {
        // TODO
        ???
        def toShortString: String = ???
    }

    type Source = Iterable[Input]

    implicit class InputToShortString(input: Input) {
        def toShortString: String = input match {
            case Character(char, _) =>
                char match {
                    case '\n' => "\\n"
                    case '\t' => "\\t"
                    case '\\' => "\\\\"
                    case _ => s"$char"
                }
            case Virtual(name, _) => s"<$name>"
            case AbstractInput(chars) => s"{${chars.toShortString}}"
        }

        def toCleanString: String = input match {
            case Character(char, _) =>
                char match {
                    case '\n' => "\\n"
                    case '\r' => "\\r"
                    case '\t' => "\\t"
                    case c => c.toString
                }
            case Virtual(name, _) => s"{$name}"
            case AbstractInput(chars) => s"{${chars.toShortString}}"
        }
    }
    implicit class SourceToCleanString(source: Source) {
        def toCleanString: String = (source map { _.toCleanString }).mkString
    }

    def fromString(source: String): Seq[Input] =
        source.toCharArray.zipWithIndex map { ci => Character(ci._1, ci._2) }
}
