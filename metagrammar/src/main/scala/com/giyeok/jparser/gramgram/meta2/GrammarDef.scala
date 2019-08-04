package com.giyeok.jparser.gramgram.meta2

import com.giyeok.jparser.gramgram.MetaGrammar
import com.giyeok.jparser.nparser.NGrammar

object GrammarDef {
    lazy val oldGrammar: NGrammar = NGrammar.fromGrammar(MetaGrammar.translateForce("Meta Grammar 2",
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
          |Id = <[{a-zA-Z} {a-zA-Z0-9}*]>
          |WS = ({ \n\r\t} | LineComment)*
          |LineComment = '/' '/' (.-'\n')* (EOF | '\n')
          |EOF = !.
        """.stripMargin))

    val newGrammar: String =
        """Grammar = WS Def (WS Def)* WS {@Grammar(defs=[$1 + $2$1])}
          |Def: @Def = Rule | TypeDef
          |
          |TypeDef: @TypeDef = '@' ClassDef
          |  | '@' SuperDef
          |ClassDef = TypeName WS '(' WS (ClassParams WS)? ')' {@ClassDef(typeName=$0, params=$4$0)}
          |SuperDef = TypeName WS '{' WS (SubTypes WS)? '}' {@SuperDef(typeName=$0, subs=$4$0)}
          |TypeName = Id
          |ClassParams = ClassParam (WS ',' WS ClassParam)* {[$0] + $1$3}
          |ClassParam = ParamName (WS ':' WS TypeDesc)? {@ClassParam(name=$0, typeDesc=$1$3)}
          |ParamName = Id
          |TypeDesc = ValueTypeDesc (WS '?')? {@TypeDesc(type=$0, optional:bool=$1)}
          |ValueTypeDesc: @ValueTypeDesc = TypeName
          |  | OnTheFlyTypeDef
          |  | '[' WS TypeDesc WS ']' {@ArrayTypeDesc(elemType=$2)}
          |SubTypes = SubType (WS ',' WS SubType)*
          |SubType: @SubType = TypeName | ClassDef | SuperDef
          |
          |OnTheFlyTypeDef = '@' WS TypeName (WS OnTheFlySuperTypes)? {@OnTheFlyTypeDef(name=$2, supers=$4$1)}
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
          |PExpr: @PExpr = PExpr WS <BinOp> WS PTerm {@BinOpExpr(op=$2, lhs=$0, rhs=$1)}
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
          |ConstructExpr: @AbstractConstructExpr = TypeName WS ConstructParams {@ConstructExpr(type=$0, params=$2)}
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
          |Terminal: @Terminal = '\'' TerminalChar '\'' {@TerminalChar(char=$2)}
          |  | '.' {@AnyTerminal()}
          |TerminalChoice = '\'' TerminalChoiceElem TerminalChoiceElem+ '\'' {@TerminalChoice(choices=[$1] + $2)}
          |  | '\'' TerminalChoiceRange '\'' {TerminalChoice([$1])}
          |TerminalChoiceElem: @TerminalChoiceElem = TerminalChoiceChar {@TerminalChoiceChar(char=$0)}
          |  | TerminalChoiceRange
          |TerminalChoiceRange = TerminalChoiceChar '-' TerminalChoiceChar {@TerminalChoiceRange(start=$0, end=$2)}
          |StringLiteral = '"' StringChar* '"' {@StringLiteral(value=$1)}
          |
          |UnicodeChar = '\\' 'u' '0-9A-Fa-f' '0-9A-Fa-f' '0-9A-Fa-f' '0-9A-Fa-f'
          |TerminalChar = .-'\\'
          |  | '\\' '\'\\bnrt'
          |  | UnicodeChar
          |TerminalChoiceChar = .-'\'\-\\'
          |  | '\\' '\'\-\\bnrt'
          |  | UnicodeChar
          |StringChar = .-'"\\'
          |  | '\\' '"\\bnrt'
          |  | UnicodeChar
          |
          |StringLiteral = '"' StringChar* '"'
          |RefIdx = <'0' | '1-9' '0-9'*>
          |Id = <'a-zA-Z' 'a-zA-Z0-9'*>
          |WS = (' \n\r\t' | LineComment)*
          |LineComment = '/' '/' (.-'\n')* (EOF | '\n')
          |EOF = !.
        """.stripMargin
}
