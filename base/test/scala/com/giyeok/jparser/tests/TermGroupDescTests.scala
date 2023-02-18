package com.giyeok.jparser.tests

import com.giyeok.jparser.Inputs._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TermGroupDescTests extends AnyFlatSpec with Matchers {
    it should "flat exact char and flat exact char" in {
        val g1 = CharsGroup(Set(), Set(), Set('a', 'b', 'c'))
        val g2 = CharsGroup(Set(), Set(), Set('a', 'c', 'd'))

        (g1 - g2) should be(CharsGroup(Set(), Set(), Set('b')))
        (g1 + g2) should be(CharsGroup(Set(), Set(), Set('a', 'b', 'c', 'd')))
        (g1 intersect g2) should be(CharsGroup(Set(), Set(), Set('a', 'c')))
    }
    it should "flat unicode and flat exact char" in {
        val unicodeCharGroup = CharsGroup(Set('a'.getType), Set(), Set())
        val exactCharGroup = CharsGroup(Set(), Set(), Set('a'))

        (unicodeCharGroup - exactCharGroup) should be(CharsGroup(Set('a'.getType), Set('a'), Set()))
        (unicodeCharGroup + exactCharGroup) should be(CharsGroup(Set('a'.getType), Set(), Set()))
        (unicodeCharGroup intersect exactCharGroup) should be(CharsGroup(Set(), Set(), Set('a')))
    }

    it should "unicode with excluding chars and flat exact char" in {
        val unicodeCharGroup = CharsGroup(Set('a'.getType), Set('a', 'b'), Set())
        val exactCharGroup = CharsGroup(Set(), Set(), Set('a', 'c'))

        (unicodeCharGroup - exactCharGroup) should be(CharsGroup(Set('a'.getType), Set('a', 'b', 'c'), Set()))
        (unicodeCharGroup + exactCharGroup) should be(CharsGroup(Set('a'.getType), Set('b'), Set()))
        (unicodeCharGroup intersect exactCharGroup) should be(CharsGroup(Set(), Set(), Set('c')))
    }

    it should "" in {
        val nums = CharsGroup(Set(), Set(), Set(8, 4, 9, 5, 6, 1, 2, 7, 3))
    }
}
