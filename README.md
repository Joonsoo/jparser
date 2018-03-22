# J Parser

This is an implementation of Conditional Derivation Grammar parsing algorithm.

## Conditional Derivation Grammar

Conditional Derivation Grammar(CDG) is an extension of Context-Free Grammar(CFG) with additional features: intersection and exclusion, followed-by and not-followed-by, and longest match.

The overall structure of CDG looks similar to CFG. The following shows the typical expression grammar written in CDG.
```
Expr = Term | Expr {+\-} Term
Term = Factor | Term {*/} Factor
Factor = Number | '(' Expr ')'
Number = {1-9} {0-9}*
```

The name of nonterminal can be an arbitrary string of digits and English alphabets. No matter what the first letter is, all names are treated equally.

A pair of curly brackets represents a character class. `{*/}` is matched to `*` or `/`. You can specify a range in the curly brackets as `{1-9}`, that means any character from `1` to `9`. Since the hyphen represents a range, you should write `\` in front of `-` if you mean the hyphen character itself, as in `{+\-}`.


CDG has 5 special _conditional nonterminals_ as follows:
	
* `A&B` is intersection symbol. `A&B` matches to a string iff the string is matched to both `A` and `B`.
* `A-B` is exclusion symbol. `A-B` matches to a string iff the string is matched to `A`, but not to `B`.
* `$A` is followed-by symbol. It is matched to an empty string iff there exists a following substring that is matched to `A`.
* `!A` is not-followed-by symbol. It is matched to an empty string iff there exists NOT any following substring that is matched to `A`.
* `<A>` is the longest match symbol of `A`. It is matched to a string iff the string is matched to `A`, and no longer match is possible.

These symbols are useful especially when you want to describe the semantics of lexical analyzers such as lex or flex. Such programs use longest match policy and priorities among tokens, and these features cannot be represented in CFGs.

The following shows an example CDG that imitates the lexical analyzers.

```
S = token*
token = keyword | operator | identifier
keyword = ("if" | "else") & name
operator = <op>
op = '+' | "++"
identifier = name - keyword
name = <[{A-Za-z} {0-9A-Za-z}*]>
```

Here are some example strings of this grammar:

* `abc` is an `identifier`; it is a `name` but not `keyword`.
* `if` is a `keyword`; it is `name` and it is `"if"`.
* `ifx` is an `identifier`; `ifx` is a `name` and it is not a `keyword` even though `ifx` starts with `if`.
* `++` is an `operator`; it could have been two `+` unless `operator` is `op` without longest match.
* `+++` is two `operator`s of `++` and `+`.


## About the parsing algorithm

The parsing algorithm for CDG implemented in J Parser is a modified version of [Earley parsing](https://en.wikipedia.org/wiki/Earley_parser). CDG parsing uses graphs to represent the states of parsing and introduces _accept condition_ for conditional nonterminals.

The upper bound of the time complexity of the algorithm seems to be exponential to the length of the input string. However, I expect it to have reasonable and practical time complexity (e.g. linear) for the grammars of the most programming languages.


## How to run

J Parser provides a UI program to try CDGs and visualize the parsing process.
In order to run the UI, you need [SBT 1.1.1](https://www.scala-sbt.org/).
If you have SBT, run `startStudio.sh`(Linux/Mac) or `startStudio.bat`(Windows).
The script simply runs SBT, and it will download and install all requirements, including Scala.

## Implementation

The algorithm is entirely implemented in [Scala](http://scala-lang.org/). Other requirements are as follows:

* [SWT](https://www.eclipse.org/swt/) for GUI programming
	* [oneswt](https://bintray.com/joonsoo/sbt-plugins/sbt-oneswt) is used for simpler integration of SWT with SBT.
* [JFace](https://wiki.eclipse.org/JFace) for GUI programming
* [Eclipse draw2d](https://www.eclipse.org/gef/draw2d/) for visualization
* [Eclipse zest](https://www.eclipse.org/gef/zest/) for graph layout algorithms of visualization
* [Scala XML](https://github.com/scala/scala-xml) for web-based visualization (TBD)
