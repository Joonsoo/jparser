package com.giyeok.moonparser

object Inputs {
    type Location = Int

    trait Input {
        val location: Location
        def toShortString: String
    }
    case class Character(char: Char, location: Location) extends Input {
        def toShortString = s"'$char'"
    }
    case class Virtual(name: String, location: Location) extends Input {
        def toShortString = s"{$name}"
    }
    case class EOF(location: Location) extends Input {
        def toShortString = "()"
    }

    type Source = Iterable[Input]

    def fromString(source: String): Seq[Input] =
        source.toCharArray.zipWithIndex map { p => Character(p._1, p._2) }
}
