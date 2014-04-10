package com.giyeok.moonparser.utils

trait SeqOrderedTester {
    // TODO verify this

    implicit class SeqOrderedTestable[T <% Ordered[T]](s: Seq[T]) {
        private def test(compare: (T, T) => Boolean): Boolean = {
            def rec(i: Int): Boolean =
                if (i >= s.size) true else if (!compare(s(i - 1), s(i))) false else rec(i + 1)
            rec(1)
        }
        def isStrictlyDecreasing: Boolean = test(_ > _)
        def isDecreasing: Boolean = test(_ >= _)
        def isStrictlyIncreasing: Boolean = test(_ < _)
        def isIncreasing: Boolean = test(_ <= _)
    }

    implicit class ListOrderedTestable[T <% Ordered[T]](l: List[T]) {
        private def test(compare: (T, T) => Boolean): Boolean = {
            def rec(head: T, tail: List[T]): Boolean =
                if (tail isEmpty) true else if (!compare(head, tail.head)) false else rec(tail.head, tail.tail)
            if (l isEmpty) true else rec(l.head, l.tail)
        }
        def isStrictlyDecreasing: Boolean = test(_ > _)
        def isDecreasing: Boolean = test(_ >= _)
        def isStrictlyIncreasing: Boolean = test(_ < _)
        def isIncreasing: Boolean = test(_ <= _)
    }
}
