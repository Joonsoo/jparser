package com.giyeok.moonparser

object Inputs {
    type Location = Int

    sealed trait Input {
        val location: Location
    }
    case class Character(char: Char, location: Location) extends Input
    case class Virtual(name: String, location: Location) extends Input
    case class EOF(location: Location) extends Input

    type Source = Iterable[Input]

    implicit class InputToShortString(input: Input) {
        def toShortString: String = input match {
            case Character(char, _) => s"'$char'"
            case Virtual(name, _) => s"{$name}"
            case EOF(_) => "(EOF)"
        }

        def toCleanString: String = input match {
            case Character(char, _) => char.toString
            case Virtual(name, _) => s"{$name}"
            case EOF(_) => "(EOF)"
        }
    }

    def fromString(source: String): Seq[Input] =
        source.toCharArray.zipWithIndex map { p => Character(p._1, p._2) }
}
