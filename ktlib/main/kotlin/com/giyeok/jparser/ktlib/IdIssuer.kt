package com.giyeok.jparser.ktlib

interface IdIssuer {
  fun nextId(): Int
}

class IdIssuerImpl(startId: Int = 0) : IdIssuer {
  private var idCounter = startId

  override fun nextId(): Int {
    idCounter += 1
    return idCounter
  }
}
