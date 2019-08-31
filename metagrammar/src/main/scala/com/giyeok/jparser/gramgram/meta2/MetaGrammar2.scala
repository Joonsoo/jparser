package com.giyeok.jparser.gramgram.meta2

import java.io.{BufferedWriter, FileWriter}

import com.giyeok.jparser.Inputs.CharsGrouping
import com.giyeok.jparser.NGrammar.fromGrammar
import com.giyeok.jparser.Symbols.Terminals
import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.nparser.{NaiveParser, ParseTreeConstructor}
import com.giyeok.jparser.{Grammar, NGrammar, ParseForestFunc, Symbols}

object MetaGrammar2 {

    object GrammarDef {
        lazy val oldGrammar: NGrammar = fromGrammar(MetaGrammar.translateForce("Meta Grammar 2",
            """Grammar = WS Def [WS Def]* WS
              |Def = Rule | TypeDef
              |
              |TypeDef = '@' ClassDef
              |  | '@' SuperDef
              |ClassDef = TypeName WS '(' WS [ClassParams WS]? ')'
              |SuperDef = TypeName WS '{' WS [SubTypes WS]? '}'
              |TypeName = Id
              |ClassParams = ClassParam [WS ',' WS ClassParam]*
              |ClassParam = ParamName [WS ':' WS TypeDesc]?
              |ParamName = Id
              |TypeDesc = ValueTypeDesc [WS '?']?
              |ValueTypeDesc = TypeName
              |  | OnTheFlyTypeDef
              |  | '[' WS TypeDesc WS ']'
              |SubTypes = SubType [WS ',' WS SubType]*
              |SubType = TypeName | ClassDef | SuperDef
              |
              |OnTheFlyTypeDef = '@' WS TypeName [WS OnTheFlySuperTypes]?
              |OnTheFlySuperTypes = '<' WS TypeName [WS ',' WS TypeName]* WS '>'
              |
              |Rule = LHS WS '=' WS RHSs
              |LHS = Nonterminal [WS ':' WS TypeDesc]?
              |RHSs = RHS [WS '|' WS RHS]*
              |RHS = Elem [WS Elem]*
              |Elem = Processor | Symbol
              |
              |Processor = Ref
              |  | '{' WS PExpr WS '}'
              |PExpr = PExpr WS BinOp WS PTerm
              |  | PTerm
              |BinOp = "+"
              |PTerm = Ref
              |  | BoundPExpr
              |  | ConstructExpr
              |  | '(' WS PExpr WS ')'
              |  | ArrayTerm
              |Ref = '$' RefIdx
              |ArrayTerm = '[' WS [PExpr [WS ',' WS PExpr]* WS]? ']'
              |BoundPExpr = Ref BoundedPExpr
              |BoundedPExpr = Ref
              |  | BoundPExpr
              |  | '{' WS PExpr WS '}'
              |ConstructExpr = TypeName WS ConstructParams
              |  | OnTheFlyTypeDefConstructExpr
              |ConstructParams = '(' WS [PExpr [WS ',' WS PExpr]* WS]? ')'
              |OnTheFlyTypeDefConstructExpr = OnTheFlyTypeDef WS NamedParams
              |NamedParams = '(' WS [NamedParam [WS ',' WS NamedParam]* WS]? ')'
              |NamedParam = ParamName [WS ':' WS TypeDesc]? WS '=' WS PExpr
              |
              |Symbol = BinSymbol
              |BinSymbol = BinSymbol WS '&' WS PreUnSymbol
              |  | BinSymbol WS '-' WS PreUnSymbol
              |  | PreUnSymbol
              |PreUnSymbol = '^' WS PreUnSymbol
              |  | '!' WS PreUnSymbol
              |  | PostUnSymbol
              |PostUnSymbol = PostUnSymbol WS '?'
              |  | PostUnSymbol WS '*'
              |  | PostUnSymbol WS '+'
              |  | AtomSymbol
              |AtomSymbol = Terminal
              |  | TerminalChoice
              |  | StringLiteral
              |  | Nonterminal
              |  | '(' InPlaceChoices ')'
              |  | Longest
              |  | EmptySequence
              |InPlaceChoices = InPlaceSequence [WS '|' WS InPlaceSequence]*
              |InPlaceSequence = Symbol [WS Symbol]*
              |Longest = '<' InPlaceChoices '>'
              |EmptySequence = '#'
              |Nonterminal = Id
              |Terminal = '\'' TerminalChar '\''
              |  | '.'
              |TerminalChoice = '\'' TerminalChoiceElem TerminalChoiceElem+ '\''
              |  | '\'' TerminalChoiceRange '\''
              |TerminalChoiceElem = TerminalChoiceChar | TerminalChoiceRange
              |TerminalChoiceRange = TerminalChoiceChar '-' TerminalChoiceChar
              |StringLiteral = '"' StringChar* '"'
              |
              |UnicodeChar = '\\' 'u' {0-9A-Fa-f} {0-9A-Fa-f} {0-9A-Fa-f} {0-9A-Fa-f}
              |TerminalChar = .-{\\}
              |  | '\\' {\'\\bnrt}
              |  | UnicodeChar
              |TerminalChoiceChar = .-{\'\-\\}
              |  | '\\' {\'\-\\bnrt}
              |  | UnicodeChar
              |StringChar = .-{"\\}
              |  | '\\' {"\\bnrt}
              |  | UnicodeChar
              |
              |StringLiteral = '"' StringChar* '"'
              |RefIdx = <('0' | [{1-9} {0-9}*])>
              |Id = <[{a-zA-Z_} {a-zA-Z0-9_}*]>
              |WS = ({ \n\r\t} | LineComment)*
              |LineComment = '/' '/' (.-'\n')* (EOF | '\n')
              |EOF = !.
        """.stripMargin))

        val newGrammar: String =
            """Grammar = WS Def (WS Def)* WS {@Grammar(defs=[$1] + $2$1)}
              |Def: @Def = Rule | TypeDef
              |
              |TypeDef: @TypeDef = '@' ClassDef
              |  | '@' SuperDef
              |ClassDef = TypeName WS '(' WS (ClassParams WS)? ')' {@ClassDef(typeName=$0, params=$4$0)}
              |SuperDef = TypeName WS '{' WS (SubTypes WS)? '}' {@SuperDef(typeName=$0, subs=$4$0)}
              |TypeName = Id {@TypeName(name=$0)}
              |ClassParams = ClassParam (WS ',' WS ClassParam)* {[$0] + $1$3}
              |ClassParam = ParamName (WS ':' WS TypeDesc)? {@ClassParam(name=$0, typeDesc=$1$3)}
              |ParamName = Id {@ParamName(name=$0)}
              |TypeDesc = ValueTypeDesc (WS '?')? {@TypeDesc(typ=$0, optional=$1)}
              |ValueTypeDesc: @ValueTypeDesc = TypeName
              |  | OnTheFlyTypeDef
              |  | '[' WS TypeDesc WS ']' {@ArrayTypeDesc(elemType=$2)}
              |SubTypes = SubType (WS ',' WS SubType)* {[$0] + $1$3}
              |SubType: @SubType = TypeName | ClassDef | SuperDef
              |
              |OnTheFlyTypeDef = '@' WS TypeName (WS OnTheFlySuperTypes)? {@OnTheFlyTypeDef(name=$2, supers=$3$1)}
              |OnTheFlySuperTypes = '<' WS TypeName (WS ',' WS TypeName)* WS '>' {[$2] + $3$3}
              |
              |Rule = LHS WS '=' WS RHSs {@Rule(lhs=$0, rhs=$4)}
              |LHS = Nonterminal (WS ':' WS TypeDesc)? {@LHS(name=$0, typeDesc=$1$3)}
              |RHSs = RHS (WS '|' WS RHS)* {[$0] + $1$3}
              |RHS = Elem (WS Elem)* {@RHS(elems=[$0] + $1$1)}
              |Elem: @Elem = Processor | Symbol
              |
              |Processor: @Processor = Ref
              |  | '{' WS PExpr WS '}' $2
              |PExpr: @PExpr = PExpr WS <BinOp> WS PTerm {@BinOpExpr(op=$2, lhs=$0, rhs=$4)}
              |  | PTerm
              |BinOp = "+"
              |PTerm: @PTerm = Ref
              |  | BoundPExpr
              |  | ConstructExpr
              |  | '(' WS PExpr WS ')' {@PTermParen(expr=$2)}
              |  | ArrayTerm
              |Ref = '$' RefIdx {@Ref(idx=$1)}
              |ArrayTerm = '[' WS (PExpr (WS ',' WS PExpr)* WS)? ']' {@PTermSeq(elems=$2{[$0] + $1$3})}
              |BoundPExpr = Ref BoundedPExpr {@BoundPExpr(ctx=$0, expr:@BoundedPExpr=$1)}
              |// type(BoundedPExpr)는 모두 BoundedPExpr의 subclass여야 함
              |BoundedPExpr = Ref
              |  | BoundPExpr
              |  | '{' WS PExpr WS '}' $2
              |// Ref, BoundPExpr, PExpr은 모두 BoundedPExpr의 subclass여야 함
              |ConstructExpr: @AbstractConstructExpr = TypeName WS ConstructParams {@ConstructExpr(typeName=$0, params=$2)}
              |  | OnTheFlyTypeDefConstructExpr
              |ConstructParams = '(' WS (PExpr (WS ',' WS PExpr)* WS)? ')' {$2{[$0] + $1$3}}
              |OnTheFlyTypeDefConstructExpr = OnTheFlyTypeDef WS NamedParams {@OnTheFlyTypeDefConstructExpr(typeDef=$0, params=$2)}
              |NamedParams = '(' WS (NamedParam (WS ',' WS NamedParam)* WS)? ')' {$2{[$0] + $1$3}}
              |NamedParam = ParamName (WS ':' WS TypeDesc)? WS '=' WS PExpr {@NamedParam(name=$0, typeDesc=$1$3, expr=$5)}
              |
              |Symbol: @Symbol = BinSymbol
              |BinSymbol: @BinSymbol = BinSymbol WS '&' WS PreUnSymbol {@JoinSymbol(symbol1=$0, symbol2=$4)}
              |  | BinSymbol WS '-' WS PreUnSymbol {@ExceptSymbol(symbol1=$0, symbol2=$4)}
              |  | PreUnSymbol
              |PreUnSymbol: @PreUnSymbol = '^' WS PreUnSymbol {@FollowedBy(expr=$2)}
              |  | '!' WS PreUnSymbol {@NotFollowedBy(expr=$2)}
              |  | PostUnSymbol
              |PostUnSymbol: @PostUnSymbol = PostUnSymbol WS ('?' | '*' | '+') {@Repeat(expr=$0, repeat=$2)}
              |  | AtomSymbol
              |AtomSymbol: @AtomSymbol = Terminal
              |  | TerminalChoice
              |  | StringLiteral
              |  | Nonterminal
              |  | '(' InPlaceChoices ')' {@Paren(choices=$1)}
              |  | Longest
              |  | EmptySequence {@EmptySeq()}
              |InPlaceChoices = InPlaceSequence (WS '|' WS InPlaceSequence)* {@InPlaceChoices(choices=[$0] + $1$3)}
              |// TODO InPlaceSequence에서 Symbol -> Elem 받도록 수정
              |InPlaceSequence = Symbol (WS Symbol)* {@InPlaceSequence(seq=[$0] + $1$1)}
              |Longest = '<' InPlaceChoices '>' {@Longest(choices=$1)}
              |EmptySequence = '#'
              |Nonterminal = Id {@Nonterminal(name=$0)}
              |Terminal: @Terminal = '\'' TerminalChar '\'' $1
              |  | '.' {@AnyTerminal(c=$0)}
              |TerminalChoice = '\'' TerminalChoiceElem TerminalChoiceElem+ '\'' {@TerminalChoice(choices:[TerminalChoiceElem]=[$1] + $2$0)}
              |  | '\'' TerminalChoiceRange '\'' {TerminalChoice([$1])}
              |TerminalChoiceElem: @TerminalChoiceElem = TerminalChoiceChar
              |  | TerminalChoiceRange
              |TerminalChoiceRange = TerminalChoiceChar '-' TerminalChoiceChar {@TerminalChoiceRange(start=$0, end=$2)}
              |StringLiteral = '"' StringChar* '"' {@StringLiteral(value=$1$0)}
              |
              |UnicodeChar = '\\' 'u' '0-9A-Fa-f' '0-9A-Fa-f' '0-9A-Fa-f' '0-9A-Fa-f' {@CharUnicode(code=[$2, $3, $4, $5])}
              |TerminalChar: @TerminalChar = .-'\\' {@CharAsIs(c=$0)}
              |  | '\\' '\'\\bnrt' {@CharEscaped(escapeCode=$1)}
              |  | UnicodeChar
              |TerminalChoiceChar: @TerminalChoiceChar = .-'\'\-\\' {CharAsIs($0)}
              |  | '\\' '\'\-\\bnrt' {CharEscaped($1)}
              |  | UnicodeChar
              |StringChar: @StringChar = .-'"\\' {CharAsIs($0)}
              |  | '\\' '"\\bnrt' {CharEscaped($1)}
              |  | UnicodeChar
              |
              |RefIdx = <'0' | '1-9' '0-9'*>
              |Id = <'a-zA-Z_' 'a-zA-Z0-9_'*>
              |WS = (' \n\r\t' | LineComment)*
              |LineComment = '/' '/' (.-'\n')* (EOF | '\n')
              |EOF = !.
        """.stripMargin
    }

    val parser = new NaiveParser(GrammarDef.oldGrammar)

    def grammarSpecToAST(grammar: String): Option[AST.Grammar] = {
        val result = parser.parse(grammar)

        result match {
            case Left(ctx) =>
                val reconstructor = new ParseTreeConstructor(ParseForestFunc)(GrammarDef.oldGrammar)(ctx.inputs, ctx.history, ctx.conditionFinal)
                reconstructor.reconstruct() match {
                    case Some(parseForest) =>
                        assert(parseForest.trees.size == 1)
                        val tree = parseForest.trees.head

                        Some(ASTifier.matchGrammar(tree))
                    case None =>
                        println("Incomplete input")
                        None
                }
            case Right(error) =>
                println(error)
                None
        }
    }

    class Analysis(val astAnalysis: AstAnalysis, val typeAnalysis: TypeAnalysis)

    def analyze(ast: AST.Grammar): Analysis = {
        val astAnalyzer = new AstAnalyzer(ast)
        val astAnalysis = astAnalyzer.analyzeAstifiers()
        val typeAnalysis = new TypeAnalyzer(astAnalyzer).analyzeTypeHierarchy()

        new Analysis(astAnalysis, typeAnalysis)
    }

    def stringify(grammar: Grammar): String = {
        def symbolStringOf(symbol: Symbols.Symbol, outerPrec: Int): String = {
            val (string: String, prec: Int) = symbol match {
                case terminal: Symbols.Terminal =>
                    val s = terminal match {
                        case Terminals.Any | Terminals.AnyChar => "."
                        case Terminals.ExactChar(char) =>
                            val s = char match {
                                case '\n' => "\\n"
                                case '\\' => "\\\\"
                                case '\'' => "\\'"
                                // TODO complete escape
                                case _ => s"$char"
                            }
                            s"'$s'"
                        case chars: Terminals.Chars =>
                            def escapeChar(char: Char): String =
                                char match {
                                    case '-' => "\\-"
                                    case '}' => "\\}"
                                    case '\n' => "\\n"
                                    case '\t' => "\\t"
                                    case '\r' => "\\r"
                                    case '\\' => "\\\\"
                                    // TODO complete escape
                                    case _ => "" + char
                                }

                            "{" + (chars.chars.groups.sorted map { pair =>
                                if (pair._1 == pair._2) s"${escapeChar(pair._1)}"
                                else if (pair._1 + 1 == pair._2) s"${escapeChar(pair._1)}${escapeChar(pair._2)}"
                                else s"${escapeChar(pair._1)}-${escapeChar(pair._2)}"
                            } mkString "") + "}"
                        // case Terminals.Unicode(categories) =>
                        // case Terminals.ExactVirtual(name) =>
                        // case Terminals.Virtuals(names) =>
                    }
                    (s, 0)
                case Symbols.Nonterminal(name) =>
                    (name, 0)
                case Symbols.Sequence(seq, contentIdx) =>
                    val isString = seq forall (_.isInstanceOf[Symbols.Terminals.ExactChar])
                    if (isString) {
                        // string인 경우
                        // TODO escape
                        ("\"" + (seq map (_.asInstanceOf[Symbols.Terminals.ExactChar].char)).mkString + "\"", 0)
                    } else {
                        (seq map {
                            symbolStringOf(_, 5)
                        } mkString " ", 5)
                    }
                case Symbols.OneOf(syms) =>
                    val isOptional = syms.size == 2 && (syms contains Symbols.Proxy(Symbols.Sequence(Seq())))
                    if (isOptional) {
                        val optContent = (syms - Symbols.Proxy(Symbols.Sequence(Seq()))).head
                        (symbolStringOf(optContent, 2) + "?", 2)
                    } else {
                        ("(" + (syms.toSeq map {
                            symbolStringOf(_, 5)
                        } mkString " | ") + ")", 0)
                    }
                case Symbols.Repeat(sym, 0) =>
                    (symbolStringOf(sym, 2) + "*", 2)
                case Symbols.Repeat(sym, 1) =>
                    (symbolStringOf(sym, 2) + "+", 2)
                case Symbols.Repeat(sym, more) =>
                    ("(" + ((symbolStringOf(sym, 5) + " ") * more) + (symbolStringOf(sym, 5) + "*") + ")", 0)
                case Symbols.Except(sym, except) =>
                    (symbolStringOf(sym, 4) + "-" + symbolStringOf(except, 4), 4)
                case Symbols.LookaheadIs(lookahead) =>
                    ("^" + symbolStringOf(lookahead, 0), 1)
                case Symbols.LookaheadExcept(except) =>
                    ("!" + symbolStringOf(except, 0), 1)
                case Symbols.Proxy(sym) =>
                    ("(" + symbolStringOf(sym, 5) + ")", 0)
                case Symbols.Join(sym, join) =>
                    (symbolStringOf(sym, 3) + "&" + symbolStringOf(join, 3), 3)
                case Symbols.Longest(sym) =>
                    ("<" + symbolStringOf(sym, 5) + ">", 0)
            }
            if (outerPrec < prec) "(" + string + ")" else string
        }

        def nonterminalNameOf(name: String): String = {
            val charArray = name.toCharArray
            if (charArray forall { c => ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || (c == '_') }) {
                name
            } else {
                // TODO \하고 ` escape
                s"`$name`"
            }
        }

        def ruleStringOf(lhs: String, rhs: List[Symbols.Symbol]): String = {
            nonterminalNameOf(lhs) + " = " + ((rhs map { symbol => symbolStringOf(symbol, 6) }) mkString "\n    | ")
        }

        val startRule = grammar.rules(grammar.startSymbol.name).toList
        val restRules = grammar.rules - grammar.startSymbol.name

        ruleStringOf(grammar.startSymbol.name, startRule) + "\n" +
            (restRules.toList.map { kv => ruleStringOf(kv._1, kv._2.toList) } mkString "\n")
    }

    // 문법이 주어지면
    // 1a. processor가 없는 문법 텍스트
    // 1b. NGrammar 정의하는 스칼라 코드(new NGrammar(...))
    // 1c. (나중엔) 제너레이트된 파서
    // 2. 정의된 타입들을 정의하는 자바 코드
    // 3. ParseForest를 주면 프로세서로 처리해서 가공한 값으로 만들어주는 자바 코드

    case class TestGrammar(packageName: Option[String], name: String, grammar: String, parseUtils: Boolean)

    def main(args: Array[String]): Unit = {
        val pkgName = Some("com.giyeok.jparser.gramgram.meta2.generated")
        val expressionGrammar = TestGrammar(pkgName, "ExpressionGrammar",
            """expression: @Expression = term
              |    | expression '+' term {@BinOp(op=$1, lhs:Expression=$0, rhs=$2)}
              |term: @Term = factor
              |    | term '*' factor {BinOp($1, $0, $2)}
              |factor: @Factor = number
              |    | variable
              |    | '(' expression ')' {@Paren(expr=$1)}
              |number: @Number = '0' {@Integer(value=[$0])}
              |    | '1-9' '0-9'* {Integer([$0] + $1)}
              |variable = <'A-Za-z'+> {@Variable(name=$0)}
            """.stripMargin, parseUtils = true)
        val metaGrammar2 = TestGrammar(pkgName, "MetaGrammar2Ast", GrammarDef.newGrammar, parseUtils = true)

        List(expressionGrammar, metaGrammar2) foreach { grammar =>
            val analysis = analyze(grammarSpecToAST(grammar.grammar).get)

            analysis.astAnalysis.astifiers.foreach { case (lhs, rhs) =>
                println(s"$lhs =")
                rhs.foreach { r =>
                    println(s"  ${r._1.toShortString}")
                    println(s"  ${r._2}")
                }
            }
            println(analysis.typeAnalysis.typeDependenceGraph.toDotGraphModel.printDotGraph())

            val scalaCode = s"package ${grammar.packageName.get}\n\n" +
                new ScalaDefGenerator(analysis).toGrammarObject(grammar.name, parseUtils = grammar.parseUtils)
            val filepath = s"./metagrammar/src/main/scala/com/giyeok/jparser/gramgram/meta2/generated/${grammar.name}.scala"
            val writer = new BufferedWriter(new FileWriter(filepath))
            writer.write(scalaCode)
            writer.close()
        }
    }
}
