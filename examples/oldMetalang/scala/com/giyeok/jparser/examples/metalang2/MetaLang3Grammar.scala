package com.giyeok.jparser.examples.metalang2

object MetaLang3Grammar {
  val inMetaLang2: String =
    """Grammar = WS Def (WSNL Def)* WS {@Grammar(defs=[$1] + $2$1)}
      |Def: @Def = Rule | TypeDef
      |
      |Rule = LHS WS '=' WS RHS (WS '|' WS RHS)* {@Rule(lhs=$0, rhs=[$4] + $5$3)}
      |LHS = Nonterminal (WS ':' WS TypeDesc)? {@LHS(name=$0, typeDesc=$1$3)}
      |RHS = Sequence
      |Elem: @Elem = Symbol | Processor
      |
      |
      |// Symbol
      |Symbol: @Symbol = BinSymbol
      |BinSymbol: @BinSymbol = BinSymbol WS '&' WS PreUnSymbol {@JoinSymbol(body=$0, join=$4)}
      |  | BinSymbol WS '-' WS PreUnSymbol {@ExceptSymbol(body=$0, except=$4)}
      |  | PreUnSymbol
      |PreUnSymbol: @PreUnSymbol = '^' WS PreUnSymbol {@FollowedBy(followedBy=$2)}
      |  | '!' WS PreUnSymbol {@NotFollowedBy(notFollowedBy=$2)}
      |  | PostUnSymbol
      |PostUnSymbol: @PostUnSymbol = PostUnSymbol WS '?' {@Optional(body=$0)}
      |  | PostUnSymbol WS '*' {@RepeatFromZero(body=$0)}
      |  | PostUnSymbol WS '+' {@RepeatFromOne(body=$0)}
      |  | AtomSymbol
      |AtomSymbol: @AtomSymbol = Terminal
      |  | TerminalChoice
      |  | StringSymbol
      |  | Nonterminal
      |  | '(' WS InPlaceChoices WS ')' $2
      |  | Longest
      |  | EmptySequence
      |Terminal: @Terminal = '\'' TerminalChar '\'' $1
      |  | '.' {@AnyTerminal()}
      |TerminalChoice = '\'' TerminalChoiceElem TerminalChoiceElem+ '\'' {@TerminalChoice(choices:[TerminalChoiceElem]=[$1] + $2$0)}
      |  | '\'' TerminalChoiceRange '\'' {TerminalChoice([$1])}
      |TerminalChoiceElem: @TerminalChoiceElem = TerminalChoiceChar
      |  | TerminalChoiceRange
      |TerminalChoiceRange = TerminalChoiceChar '-' TerminalChoiceChar {@TerminalChoiceRange(start=$0, end=$2)}
      |StringSymbol = '"' StringChar* '"' {@StringSymbol(value=$1$0)}
      |Nonterminal = NonterminalName {@Nonterminal(name=$0)}
      |InPlaceChoices = Sequence (WS '|' WS Sequence)* {@InPlaceChoices(choices=[$0] + $1$3)}
      |Sequence = Elem (WS Elem)* {@Sequence<Symbol>(seq=[$0] + $1$1)}
      |Longest = '<' WS InPlaceChoices WS '>' {@Longest(choices=$2)}
      |EmptySequence = '#' {@EmptySeq()}
      |
      |TerminalChar: @TerminalChar = .-'\\' {@CharAsIs(value=$0)}
      |  | '\\' '\'\\bnrt' {@CharEscaped(escapeCode=$1)}
      |  | UnicodeChar
      |TerminalChoiceChar: @TerminalChoiceChar = .-'\'\-\\' {CharAsIs($0)}
      |  | '\\' '\'\-\\bnrt' {CharEscaped($1)}
      |  | UnicodeChar
      |StringChar: @StringChar = .-'"\\' {CharAsIs($0)}
      |  | '\\' '"\\bnrt' {CharEscaped($1)}
      |  | UnicodeChar
      |UnicodeChar = '\\' 'u' '0-9A-Fa-f' '0-9A-Fa-f' '0-9A-Fa-f' '0-9A-Fa-f' {@CharUnicode(code=[$2, $3, $4, $5])}
      |
      |
      |// Processor
      |Processor: @Processor = Ref
      |  | '{' WS PExpr WS '}' $2
      |
      |Ref: @Ref = ValRef | RawRef | AllRawRef
      |ValRef = '$' CondSymPath? RefIdx {@ValRef(idx=$2, condSymPath=$1)}
      |CondSymPath = ('<' | '>')+
      |RawRef = "\\$" CondSymPath? RefIdx {@RawRef(idx=$2, condSymPath=$1)}
      |AllRawRef = "\\$$" {@AllRawRef()}
      |
      |// 우선순위 낮은것부터
      |PExpr: @PExpr = TernaryExpr // TODO (WS ':' WS TypeDesc)? 를 뒤에 붙일 수 있을까?
      |TernaryExpr: @TerExpr = // BoolOrExpr WS '?' WS <TernaryExpr> WS ':' WS <TernaryExpr> {@TernaryOp(cond=$0, ifTrue=$4$0, ifFalse=$8$0)}
      |  BoolOrExpr
      |BoolOrExpr: @BoolOrExpr = BoolAndExpr WS "&&" WS BoolOrExpr {@BinOp(op=$2, lhs=$0, rhs=$4)}
      |  | BoolAndExpr
      |BoolAndExpr: @BoolAndExpr = BoolEqExpr WS "||" WS BoolAndExpr {BinOp($2, $0, $4)}
      |  | BoolEqExpr
      |BoolEqExpr: @BoolEqExpr = ElvisExpr WS ("==" | "!=") WS BoolEqExpr {BinOp($2, $0, $4)}
      |  | ElvisExpr
      |ElvisExpr: @ElvisExpr = AdditiveExpr WS "?:" WS ElvisExpr {@ElvisOp(value=$0, ifNull=$4)}
      |  | AdditiveExpr
      |AdditiveExpr: @AdditiveExpr = PrefixNotExpr WS '+' WS AdditiveExpr {BinOp($2, $0, $4)}
      |  | PrefixNotExpr
      |PrefixNotExpr: @PrefixNotExpr = '!' WS PrefixNotExpr {@PrefixOp(expr=$2, op=$0)}
      |  | Atom
      |Atom: @Atom = Ref
      |  | BindExpr
      |  | NamedConstructExpr
      |  | FuncCallOrConstructExpr
      |  | ArrayExpr
      |  | Literal
      |  | EnumValue
      |  | '(' WS PExpr WS ')' {@ExprParen(body=$2)}
      |
      |BindExpr = ValRef BinderExpr {@BindExpr(ctx=$0, binder=$1)}
      |BinderExpr: @BinderExpr = Ref
      |  | BindExpr
      |  | '{' WS PExpr WS '}' $2
      |NamedConstructExpr = TypeName (WS SuperTypes)? WS NamedConstructParams {@NamedConstructExpr(typeName=$0, params=$3, supers=$1$1)}
      |NamedConstructParams = '(' WS NamedParam (WS ',' WS NamedParam)* WS ')' {[$2] + $3$3}
      |NamedParam = ParamName (WS ':' WS TypeDesc)? WS '=' WS PExpr {@NamedParam(name=$0, typeDesc=$1$3, expr=$5)}
      |FuncCallOrConstructExpr = TypeOrFuncName WS CallParams {@FuncCallOrConstructExpr(funcName=$0, params=$2)}
      |CallParams = '(' WS (PExpr (WS ',' WS PExpr)* WS)? ')' {$2{[$0] + $1$3}}
      |ArrayExpr = '[' WS (PExpr (WS ',' WS PExpr)* WS)? ']' {@ArrayExpr(elems=$2{[$0] + $1$3})}
      |Literal: @Literal = "null" {@NullLiteral()}
      |  | ("true" | "false") {@BoolLiteral(value=$0)}
      |  | '\'' CharChar '\'' {@CharLiteral(value=$1)}
      |  | '"' StrChar* '"' {@StringLiteral(value=$1)}
      |EnumValue: @AbstractEnumValue = CanonicalEnumValue | ShortenedEnumValue
      |CanonicalEnumValue = EnumTypeName '.' EnumValueName {@CanonicalEnumValue(enumName=$0, valueName=$2)}
      |// ShortenedEnumValue는 어떤 Enum 값인지 외부의 정보로부터 확실히 알 수 있을 때만 사용 가능
      |ShortenedEnumValue = '%' EnumValueName {@ShortenedEnumValue(valueName=$1)}
      |
      |
      |// TypeDef
      |TypeDef: @TypeDef = ClassDef
      |  | SuperDef // SuperDef는 super class가 자신의 sub class를 리스팅하는 식으로 정의하는 것.
      |  | EnumTypeDef
      |ClassDef: @ClassDef = TypeName WS SuperTypes {@AbstractClassDef(name=$0, supers=$2)}
      |  // | TypeName WS ClassParamsDef {@ConcreteClassDef(name=$0, supers=[], params=$2)}
      |  | TypeName WS SuperTypes WS ClassParamsDef {@ConcreteClassDef(name=$0, supers=$2, params=$4)}
      |SuperTypes = '<' WS (TypeName (WS ',' WS TypeName)* WS)? '>' {$2{[$0] + $1$3}}
      |ClassParamsDef = '(' WS (ClassParamDef (WS ',' WS ClassParamDef)* WS)? WS ')' {$2{[$0] + $1$3}}
      |ClassParamDef = ParamName (WS ':' WS TypeDesc)? {@ClassParamDef(name=$0, typeDesc=$1$3)}
      |
      |SuperDef = TypeName (WS SuperTypes)? WS '{' (WS SubTypes)? WS '}' {@SuperDef(typeName=$0, subs=$4$1, supers=$1$1)}
      |SubTypes = SubType (WS ',' WS SubType)* {[$0] + $1$3}
      |SubType: @SubType = TypeName | ClassDef | SuperDef
      |
      |EnumTypeDef = EnumTypeName WS '{' WS Id (WS ',' WS Id)* WS '}' {@EnumTypeDef(name=$0, values=[$4] + $5$3)}
      |
      |
      |// TypeDesc
      |TypeDesc = NonNullTypeDesc (WS '?')? {@TypeDesc(typ=$0, optional=$1)}
      |NonNullTypeDesc: @NonNullTypeDesc = TypeName
      |  | '[' WS TypeDesc WS ']' {@ArrayTypeDesc(elemType=$2)}
      |  | ValueType
      |  | AnyType
      |  | EnumTypeName
      |  | TypeDef
      |
      |ValueType: @ValueType = "boolean" {@BooleanType()}
      |  | "char" {@CharType()}
      |  | "string" {@StringType()}
      |AnyType = "any" {@AnyType()}
      |EnumTypeName = '%' Id {@EnumTypeName(name=$1)}
      |// EnumTypeDef로 enum의 모든 값이 한군데서 정의되었으면 이값들만 사용되어야 한다.
      |
      |
      |// Common
      |TypeName = Id-Keyword {@TypeName(name=$0)}
      |  | '`' Id '`' {TypeName($1)}
      |NonterminalName = Id-Keyword {@NonterminalName(name=$0)}
      |  | '`' Id '`' {NonterminalName($1)}
      |TypeOrFuncName = Id-Keyword {@TypeOrFuncName(name=$0)}
      |  | '`' Id '`' {TypeOrFuncName($1)}
      |ParamName = Id-Keyword {@ParamName(name=$0)}
      |  | '`' Id '`' {ParamName($1)}
      |EnumValueName = Id {@EnumValueName(name=$0)}
      |Keyword = "boolean" | "char" | "string" | "true" | "false" | "null"
      |StrChar = StringChar
      |CharChar = TerminalChar
      |
      |RefIdx = <'0' | '1-9' '0-9'*>
      |Id = <'a-zA-Z_' 'a-zA-Z0-9_'*>
      |WS = (' \n\r\t' | LineComment)*
      |WSNL = WS // TODO newline이 포함된 WS
      |LineComment = '/' '/' (.-'\n')* (EOF | '\n')
      |EOF = !.
      |""".stripMargin

  val inMetaLang3: String =
    """Grammar = WS Def (WSNL Def)* WS {Grammar(defs=[$1] + $2)}
      |Def: Def = Rule | TypeDef
      |
      |Rule = LHS WS '=' WS (RHS (WS '|' WS RHS)* {[$0] + $1}) {Rule(lhs=$0, rhs=$4)}
      |LHS = Nonterminal (WS ':' WS TypeDesc)? {LHS(name=$0, typeDesc=$1)}
      |RHS = Sequence
      |Sequence = Elem (WS Elem)* {Sequence<Symbol>(seq=[$0] + $1)}
      |Elem: Elem = Symbol | Processor
      |
      |
      |// Symbol
      |Symbol: Symbol = BinSymbol
      |BinSymbol: BinSymbol = BinSymbol WS '&' WS PreUnSymbol {JoinSymbol(body=$0, join=$4)}
      |  | BinSymbol WS '-' WS PreUnSymbol {ExceptSymbol(body=$0, except=$4)}
      |  | PreUnSymbol
      |PreUnSymbol: PreUnSymbol = '^' WS PreUnSymbol {FollowedBy(followedBy=$2)}
      |  | '!' WS PreUnSymbol {NotFollowedBy(notFollowedBy=$2)}
      |  | PostUnSymbol
      |PostUnSymbol: PostUnSymbol = PostUnSymbol WS '?' {Optional(body=$0)}
      |  | PostUnSymbol WS '*' {RepeatFromZero(body=$0)}
      |  | PostUnSymbol WS '+' {RepeatFromOne(body=$0)}
      |  | AtomSymbol
      |AtomSymbol: AtomSymbol = Terminal
      |  | TerminalChoice
      |  | StringSymbol
      |  | Nonterminal
      |  | '(' WS InPlaceChoices WS ')' $2
      |  | Longest
      |  | EmptySequence
      |Terminal: Terminal = '\'' TerminalChar '\'' $1
      |  | '.' {AnyTerminal()}
      |TerminalChoice: TerminalChoice = '\'' TerminalChoiceElem TerminalChoiceElem+ '\'' {TerminalChoice(choices=[$1] + $2)}
      |  | '\'' TerminalChoiceRange '\'' {TerminalChoice(choices=[$1])}
      |TerminalChoiceElem: TerminalChoiceElem = TerminalChoiceChar
      |  | TerminalChoiceRange
      |TerminalChoiceRange = TerminalChoiceChar '-' TerminalChoiceChar {TerminalChoiceRange(start=$0, end=$2)}
      |StringSymbol = '"' StringChar* '"' {StringSymbol(value=$1)}
      |Nonterminal = NonterminalName {Nonterminal(name=$0)}
      |InPlaceChoices = Sequence (WS '|' WS Sequence)* {InPlaceChoices(choices=[$0] + $1)}
      |Longest = '<' WS InPlaceChoices WS '>' {Longest(choices=$2)}
      |EmptySequence = '#' {EmptySeq()}
      |
      |TerminalChar: TerminalChar = .-'\\' {CharAsIs(value=$0)}
      |  | '\\' '\'\\bnrt' {CharEscaped(escapeCode=$1)}
      |  | UnicodeChar
      |TerminalChoiceChar: TerminalChoiceChar = .-'\'\-\\' {CharAsIs(value=$0)}
      |  | '\\' '\'\-\\bnrt' {CharEscaped(escapeCode=$1)}
      |  | UnicodeChar
      |StringChar: StringChar = .-'"\\' {CharAsIs(value=$0)}
      |  | '\\' '"\\bnrt' {CharEscaped(escapeCode=$1)}
      |  | UnicodeChar
      |UnicodeChar = '\\' 'u' '0-9A-Fa-f' '0-9A-Fa-f' '0-9A-Fa-f' '0-9A-Fa-f' {CharUnicode(code=[$2, $3, $4, $5])}
      |
      |
      |// Processor
      |Processor: Processor = Ref
      |  | PExprBlock
      |
      |Ref: Ref = ValRef | RawRef
      |ValRef = '$' CondSymPath? RefIdx {ValRef(idx=$2, condSymPath=$1)}
      |CondSymPath: [%CondSymDir{BODY, COND}] = ('<' {%BODY} | '>' {%COND})+
      |RawRef = "\\$" CondSymPath? RefIdx {RawRef(idx=$2, condSymPath=$1)}
      |
      |PExprBlock = '{' WS PExpr WS '}' {ProcessorBlock(body=$2)}
      |PExpr: PExpr = TernaryExpr WS ':' WS TypeDesc {TypedPExpr(body=$0, typ=$4)}
      |  | TernaryExpr
      |TernaryExpr: TernaryExpr = BoolOrExpr WS '?' WS <TernaryExpr> WS ':' WS <TernaryExpr> {TernaryOp(cond=$0, ifTrue=$4, ifFalse=$8)}
      |  | BoolOrExpr
      |BoolOrExpr: BoolOrExpr = BoolAndExpr WS "&&" WS BoolOrExpr {BinOp(op=%Op.AND, lhs=$0, rhs=$4)}
      |  | BoolAndExpr
      |BoolAndExpr: BoolAndExpr = BoolEqExpr WS "||" WS BoolAndExpr {BinOp(op=%Op.OR, lhs=$0, rhs=$4)}
      |  | BoolEqExpr
      |BoolEqExpr: BoolEqExpr = ElvisExpr WS ("==" {%Op.EQ} | "!=" {%Op.NE}) WS BoolEqExpr {BinOp(op=$2, lhs=$0, rhs=$4)}
      |  | ElvisExpr
      |ElvisExpr: ElvisExpr = AdditiveExpr WS "?:" WS ElvisExpr {ElvisOp(value=$0, ifNull=$4)}
      |  | AdditiveExpr
      |AdditiveExpr: AdditiveExpr = PrefixNotExpr WS ('+' {%Op.ADD}) WS AdditiveExpr {BinOp(op=$2, lhs=$0, rhs=$4)}
      |  | PrefixNotExpr
      |PrefixNotExpr: PrefixNotExpr = '!' WS PrefixNotExpr {PrefixOp(op=%PreOp.NOT, expr=$2)}
      |  | Atom
      |Atom: Atom = Ref
      |  | BindExpr
      |  | NamedConstructExpr
      |  | FuncCallOrConstructExpr
      |  | ArrayExpr
      |  | Literal
      |  | EnumValue
      |  | '(' WS PExpr WS ')' {ExprParen(body=$2)}
      |
      |BindExpr = ValRef BinderExpr {BindExpr(ctx=$0, binder=$1)}
      |BinderExpr: BinderExpr = Ref
      |  | BindExpr
      |  | PExprBlock
      |NamedConstructExpr = TypeName (WS SuperTypes)? WS NamedConstructParams {NamedConstructExpr(typeName=$0, params=$3, supers=$1)}
      |NamedConstructParams = '(' WS (NamedParam (WS ',' WS NamedParam)* WS {[$0] + $1}) ')' $2
      |NamedParam = ParamName (WS ':' WS TypeDesc)? WS '=' WS PExpr {NamedParam(name=$0, typeDesc=$1, expr=$5)}
      |FuncCallOrConstructExpr = TypeOrFuncName WS CallParams {FuncCallOrConstructExpr(funcName=$0, params=$2)}
      |CallParams: [PExpr] = '(' WS (PExpr (WS ',' WS PExpr)* WS {[$0] + $1})? ')' {$2 ?: []}
      |ArrayExpr = '[' WS (PExpr (WS ',' WS PExpr)* WS)? ']' {ArrayExpr(elems: [PExpr]=$2{[$0] + $1} ?: [])}
      |Literal: Literal = "null" {NullLiteral()}
      |  | ("true" {true} | "false" {false}) {BoolLiteral(value=$0)}
      |  | '\'' CharChar '\'' {CharLiteral(value=$1)}
      |  | '"' StrChar* '"' {StrLiteral(value=$1)}
      |EnumValue: AbstractEnumValue = <CanonicalEnumValue | ShortenedEnumValue>
      |CanonicalEnumValue = EnumTypeName '.' EnumValueName {CanonicalEnumValue(enumName=$0, valueName=$2)}
      |// ShortenedEnumValue only can be used when it is clear of which enum type of the value.
      |ShortenedEnumValue = '%' EnumValueName {ShortenedEnumValue(valueName=$1)}
      |
      |
      |// TypeDef
      |TypeDef: TypeDef = ClassDef
      |  | SuperDef // SuperDef is defining super class by listing all its subclasses.
      |  | EnumTypeDef
      |ClassDef: ClassDef = TypeName WS SuperTypes {AbstractClassDef(name=$0, supers=$2)}
      |  | TypeName WS ClassParamsDef {ConcreteClassDef(name=$0, supers: [TypeName]?=null, params=$2)}
      |  | TypeName WS SuperTypes WS ClassParamsDef {ConcreteClassDef(name=$0, supers=$2, params=$4)}
      |SuperTypes: [TypeName] = '<' WS (TypeName (WS ',' WS TypeName)* WS {[$0] + $1})? '>' {$2 ?: []}
      |ClassParamsDef: [ClassParamDef] = '(' WS (ClassParamDef (WS ',' WS ClassParamDef)* WS {[$0] + $1})? WS ')' {$2 ?: []}
      |ClassParamDef = ParamName (WS ':' WS TypeDesc)? {ClassParamDef(name=$0, typeDesc=$1)}
      |
      |SuperDef = TypeName (WS SuperTypes)? WS '{' (WS SubTypes)? WS '}' {SuperDef(typeName=$0, subs=$4, supers=$1)}
      |SubTypes = SubType (WS ',' WS SubType)* {[$0] + $1}
      |SubType: SubType = TypeName | ClassDef | SuperDef
      |
      |EnumTypeDef = EnumTypeName WS '{' WS (Id (WS ',' WS Id)* {[$0] + $1}) WS '}' {EnumTypeDef(name=$0, values=$4)}
      |
      |
      |// TypeDesc
      |TypeDesc = NonNullTypeDesc (WS '?')? {TypeDesc(typ=$0, optional=ispresent($1))}
      |NonNullTypeDesc: NonNullTypeDesc = TypeName
      |  | '[' WS TypeDesc WS ']' {ArrayTypeDesc(elemType=$2)}
      |  | ValueType
      |  | AnyType
      |  | EnumTypeName
      |  | TypeDef
      |
      |ValueType: ValueType = "boolean" {BooleanType()}
      |  | "char" {CharType()}
      |  | "string" {StringType()}
      |AnyType = "any" {AnyType()}
      |EnumTypeName = '%' Id {EnumTypeName(name=str($1))}
      |// If EnumTypeDef defines all its values, no other values can be used.
      |
      |
      |// Common
      |// TODO TypeName, NonterminalName에서 `` 사이에는 Id 말고 다른거(공백, keyword 등도 쓸 수 있는)
      |TypeName = IdNoKeyword {TypeName(name=$0)}
      |  | '`' Id '`' {TypeName(name=$1)}
      |NonterminalName = IdNoKeyword {NonterminalName(name=$0)}
      |  | '`' Id '`' {NonterminalName(name=$1)}
      |TypeOrFuncName = IdNoKeyword {TypeOrFuncName(name=$0)}
      |  | '`' Id '`' {TypeOrFuncName(name=$1)}
      |ParamName = IdNoKeyword {ParamName(name=$0)}
      |  | '`' Id '`' {ParamName(name=$1)}
      |EnumValueName = Id {EnumValueName(name=$0)}
      |Keyword: %KeyWord = "boolean" {%BOOLEAN}
      |  | "char" {%CHAR}
      |  | "string" {%STRING}
      |  | "true" {%TRUE}
      |  | "false" {%FALSE}
      |  | "null" {%NULL}
      |StrChar = StringChar
      |CharChar = TerminalChar
      |
      |RefIdx = <'0' | '1-9' '0-9'*> {str(\$0)}
      |Id = <'a-zA-Z_' 'a-zA-Z0-9_'*> {str(\$0)}
      |IdNoKeyword = Id-Keyword {str(\$0)}
      |WS = (' \n\r\t' | Comment)*
      |WSNL = <(' \r\t' | Comment)* '\n' WS>
      |Comment = LineComment | BlockComment
      |LineComment = "//" (.-'\n')* (EOF | '\n')
      |BlockComment = "/*" ((. !"*/")* .)? "*/"
      |EOF = !.
      |""".stripMargin
}
