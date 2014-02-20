package com.giyeok.moonparser

object Inputs {
    type Location = Int

    trait Input { val location: Location }
    case class Character(char: Char, location: Location) extends Input
    case class Virtual(name: String, location: Location) extends Input
    case class EOF(location: Location) extends Input

    trait Source {
        def get(location: Location): Input
    }
}
