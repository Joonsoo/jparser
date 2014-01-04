package com.giyeok.moonparser

object Inputs {
    abstract class Location

    abstract class Input(location: Location)
    case class Character(char: Char, location: Location) extends Input(location)
    case class EOF(location: Location) extends Input(location)
}
