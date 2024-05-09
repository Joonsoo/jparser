# J Parser

Parser generator for Conditional Derivation Grammar


## Conditional Derivation Grammar

Conditional Derivation Grammar(CDG) is an extension of Context-Free Grammar(CFG) with additional features: intersection and exclusion, followed-by and not-followed-by, and longest match.

The overall structure of CDG looks similar to CFG. The following shows the typical expression grammar written in CDG.
```
Expr = Term | Expr '+\-' Term
Term = Factor | Term '*/' Factor
Factor = Number | '(' Expr ')'
Number = '1-9' '0-9'*
```

A nonterminal name can be a string of English alphabet and digits. But the first letter should be an alphabet. No matter whether the first letter is upper case or lower case, all names are treated equally.

The equal symbol(`=`) is the delimiter between left-hand-side and right-hand-side and the vertical bar(`|`) is the delimiter between the derivation rules.

A single quote represents a set of characters. `'*/'` is matched to `*` or `/`. You can specify a range like `'1-9'`, that means any character from `1` to `9`. Since the hyphen represents a range, `\` must come before `-` if you mean the hyphen character, as in `'+\-'`.


CDG has 5 special _conditional nonterminals_ as follows:

* `A&B` is intersection symbol. `A&B` matches to a string if the string is matched to both `A` AND `B`.
* `A-B` is exclusion symbol. `A-B` matches to a string if the string is matched to `A`, but not to `B`.
* `^A` is followed-by symbol. It is matched to an empty string if there exists a following substring that is matched to `A`.
* `!A` is not-followed-by symbol. It is matched to an empty string if there exists NOT any following substring that is matched to `A`.
* `<A>` is the longest match symbol of `A`. It is matched to a string if the string is matched to `A`, and no longer match is possible.

These symbols are useful especially when you want to describe the semantics of lexical analyzers such as lex or flex. Such programs use longest match policy and priorities among tokens, which cannot be represented in CFGs.

The following shows an example CDG that imitates the lexical analyzers.

```
S = token*
token = keyword | operator | identifier
keyword = ("if" | "else") & name
operator = <op>
op = '+' | "++"
identifier = name - keyword
name = <['A-Za-z' '0-9A-Za-z'*]>
```

Here are some example strings of this grammar:

* `abc` is an `identifier`; it is a `name` but not `keyword`.
* `if` is a `keyword`; it is `name` and it is `"if"`.
* `ifx` is an `identifier`; `ifx` is a `name` and it is not a `keyword` even though `ifx` starts with `if`.
* `++` is an `operator`; it could have been two `+` unless `operator` is `op` without longest match.
* `+++` is two `operator`s of `++` and `+`.


## Abstract Syntax Tree

You can describe the structure of Abstract Syntax Tree, or AST in the CDG definition.

Look at the following example:
```
Expr: Expr
  = Term WS '+' WS Expr {Add(lhs=$0, rhs=$4)}
  | Term
Term: Term
  = Factor WS '*' WS Term {Mul(lhs=$0, rhs=$4)}
  | Factor
Factor: Factor
  = '0-9'+ {Number(value=str($0))}
  | '(' WS Expr WS ')' {Paren(body=$2)}
WS = ' '*
```

If you parse the input string `123 * (456 + 789)`, you will get `Mul(Number(123), Paren(Add(Number(456), Number(789))))`.

* The parts between a pair of curly brackets are the AST expressions.
* `Add(...)`, `Mul(...)`, `Number(...)`, `Paren(...)` are the class initializer.
* `$0` and `$4` represents other symbols in the same sequence. As you can see, the index is 0-based.
* The final AST will not contain the information about the symbols that are not referred. For example, Add class does not have the information about the WS symbols between the operator and operands.
* `Expr: Expr`, `Term: Term`, `Factor: Factor` defines the type of the symbols, that means, Expr symbol is type of the class named Expr, and so on.
* AST generator infers the type relations from the definition as much as possible.
  * In the example above, `Expr` class should be the super class of `Add` class and `Term` class. `Term` class should be the super class of `Mul` and `Factor`, and so on.
* You can generate the Kotlin codes defining the classes for the AST and the code that converts a parse tree to a generated AST class instance.


## How to run

* JParser can be built and run using [bibix](https://github.com/Joonsoo/bibix). You need bibix to run jparser.
* JParser provides an UI to try grammars. In order to run the UI, run `bibix visualize.parserStudio` on the jparser repository directory.
  * You can see the following screen, if the build was successful:
  * ![Parser Studio](./parserstudio.png)
  * You can enter your grammar definition to the panel on the left. The example string should go to the panel on the right top, then the parse tree will be shown on the panel below. The panel at the right bottom shows the AST from the example string.

* You can also generate the parser code using bibix rule `genKtAstMgroup2`. The following shows a typical example of generating the parser using jparser and defining the parser module from the generated code and data.

```
from bibix.plugins import ktjvm
import git("https://github.com/Joonsoo/jparser.git") as jparser

parserGen = jparser.genKtAstMgroup2(
  cdgFile = "grammar/hexman.cdg",
  astifierClassName = "com.giyeok.hexman.HexManAst",
  parserDataFileName = "hexman-mg2-parserdata.pb",
)

action generate {
  file.clearDirectory("parser/generated/resources")
  file.clearDirectory("parser/generated/kotlin")
  file.copyDirectory(parserGen.srcsRoot, "parser/generated/kotlin")
  file.copyFile(parserGen.parserData, "parser/generated/resources")
}

generated = ktjvm.library(
  srcs = glob("parser/generated/kotlin/**.kt"),
  deps = [jparser.ktparser.main],
  resources = ["parser/generated/resources/hexman-mg2-parserdata.pb"]
)

main = ktjvm.library(
  srcs = glob("parser/main/kotlin/**.kt"),
  deps = [generated]
)
```


## See also

* I have been writing a series of [blog postings](https://giyeok.com/categories.html#jparser) about jparser (in Korean).
  * This series covers from the introduction to parsing, CDG, naive CDG parsing algorithm, accept condition, parse tree reconstruction algorithm, faster milestone algorithm, even faster milestone group algorithm, AST processors, and so on.
  * I am still writing the series, and some of the uploaded postings are uncompleted.

* JParser is used by some of my personal projects including:
  * [bibix](https://github.com/Joonsoo/bibix)
  * [sugarproto](https://github.com/Joonsoo/sugarproto)
  * [jsontype](https://github.com/Joonsoo/jsontype)
<!--
  * [hexman](https://github.com/Joonsoo/hexman)
  * [Compiler tutorial book](https://github.com/Joonsoo/compilerproject)
  * [autodb](https://github.com/Joonsoo/autodb3)
-->

* You can find the examples of CDG under the [examples/metalang3/resources](examples/metalang3/resources) directory.
  * The grammar of CDG itself is defined in CDG. It is called `metalang3` internally.
  * There are the CDG definitions of the simpler languages such as JSON, Protobuf 2 and 3, and [ASDL](https://asdl.sourceforge.net/).
  * I have tried to define the grammars of practical programming languages such as Java, Kotlin, and Dart. I believe it should be possible to define the grammars of those languages in CDG, and to some extent, it was successful. However, I eventually gave up because these languages are too big and complex, requiring too much time and effort.


## Notes

* JParser is implemented in Scala and Kotlin. I first started writing it in Java, but switched to Scala soon. Later, I started using Kotlin for some of the new modules. But most of the core code of JParser is still in Scala.
* JParser UI was first implemented using [SWT](https://www.eclipse.org/swt/) and some sub projects of Eclipse including [draw2d](https://www.eclipse.org/gef/draw2d/) and [zest](https://www.eclipse.org/gef/zest/). But due to the platform dependent nature of SWT, it was too painful to maintain the SWT-related codes. So I rewrote some of the UIs using Swing. The SWT version is still in the repository, but they won't be run because bibix doesn't support SWT at the moment.
