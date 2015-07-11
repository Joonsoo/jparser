package com.giyeok.moonparser.tests

import com.giyeok.moonparser.Grammar
import com.giyeok.moonparser.Symbols._
import com.giyeok.moonparser.SymbolHelper._
import scala.collection.immutable.ListMap
import scala.collection.immutable.ListSet

class JavaGrammar extends Grammar {
    val name = "Java 8"

    private val whitespace = ListSet[Symbol](n("WhiteSpace"), n("Comment"))

    def seqWS(s: Symbol*): Sequence = seq(s.toList, whitespace)

    // starWS
    implicit class RepeatWS(symbol: Symbol) {
        def starWS: Symbol = ???
    }

    // both symbol
    def t(s: Symbol): Symbol = ??? // symbol s is a token as well(완결된 단어라는 의미로)
    def ti(s: String) = t(i(s))

    val lexical: RuleMap = ListMap(
        // Productions from §3 (Lexical Structure)
        // 3.5
        "Input" -> ListSet(
            seq(n("InputElement").star, n("Sub").opt)),
        "InputElement" -> ListSet(
            n("WhiteSpace"),
            n("Comment"),
            n("Token")),
        "Token" -> ListSet(
            n("Identifier"),
            n("Keyword"),
            n("Literal"),
            n("Separator"),
            n("Operator")),
        "Sub" -> ListSet(
            c('\u001a')),
        // 3.6
        "WhiteSpace" -> ListSet(
            c(' '),
            c('\u0009'),
            c('\u000c'),
            n("LineTerminator")),
        // 3.8
        "Identifier" -> ListSet(
            n("IdentifierChars").butnot(n("Keyword"), n("BooleanLiteral"), n("NullLiteral"))),
        "IdentifierChars" -> ListSet(
            seq(n("JavaLetter"), n("JavaLetterOrDigit").star, lookahead_except(n("JavaLetterOrDigit")))),
        "JavaLetter" -> ListSet(c(Character.isJavaIdentifierStart _)),
        "JavaLetterOrDigit" -> ListSet(c(Character.isJavaIdentifierPart _)),
        "Literal" -> ListSet(
            n("IntegerLiteral"),
            n("FloatingPointLiteral"),
            n("BooleanLiteral"),
            n("CharacterLiteral"),
            n("StringLiteral"),
            n("NullLiteral")),
        // 3.9
        "Keyword" -> ListSet(
            i("abstract"), i("continue"), i("for"), i("new"), i("switch"),
            i("assert"), i("default"), i("if"), i("package"), i("synchronized"),
            i("boolean"), i("do"), i("goto"), i("private"), i("this"),
            i("break"), i("double"), i("implements"), i("protected"), i("throw"),
            i("byte"), i("else"), i("import"), i("public"), i("throws"),
            i("case"), i("enum"), i("instanceof"), i("return"), i("transient"),
            i("catch"), i("extends"), i("int"), i("short"), i("try"),
            i("char"), i("final"), i("interface"), i("static"), i("void"),
            i("class"), i("finally"), i("long"), i("strictfp"), i("volatile"),
            i("const"), i("float"), i("native"), i("super"), i("while")))

    val cfg: RuleMap = ListMap(
        // Productions from §4 (Types, Values, and Variables)
        "Type" -> ListSet(
            n("PrimitiveType"),
            n("ReferenceType")),
        "PrimitiveType" -> ListSet(
            seqWS(n("Annotation").starWS, n("NumericType")),
            seqWS(n("Annotation").starWS, ti("boolean"))),
        "NumericType" -> ListSet(
            n("IntegralType"),
            n("FloatingPointType")),
        "IntegralType" -> ListSet(
            ti("byte"),
            ti("short"),
            ti("int"),
            ti("long"),
            ti("char")),
        "FloatingPointType" -> ListSet(
            ti("float"),
            ti("double")),
        "ReferenceType" -> ListSet(
            n("ClassOrInterfaceType"),
            n("TypeVariable"),
            n("ArrayType")),
        "ClassOrInterfaceType" -> ListSet(
            n("ClassType"),
            n("InterfaceType")),
        "ClassType" -> ListSet(
            seqWS(n("Annotation").star, n("Identifier"), n("TypeArguments").opt),
            seqWS(n("ClassOrInterfaceType"), i("."), n("Annotation").star, n("Identifier"), n("TypeArguments").opt)),
        "InterfaceType" -> ListSet(
            n("ClassType")),
        "TypeVariable" -> ListSet(
            seqWS(n("Annotation").star, n("Identifier"))),
        "ArrayType" -> ListSet(
            seqWS(n("PrimitiveType"), n("Dims")),
            seqWS(n("ClassOrInterfaceType"), n("Dims")),
            seqWS(n("TypeVariable"), n("Dims"))),
        "Dims" -> ListSet(
            seqWS(n("Annotation").star, i("["), i("]"), seqWS(n("Annotation").star, i("["), i("]")).star)),
        "TypeParameter" -> ListSet(
            seqWS(n("TypeParameterModifier").star, n("Identifier"), n("TypeBound").opt)),
        "TypeParameterModifier" -> ListSet(
            n("Annotation")),
        "TypeBound" -> ListSet(
            seqWS(i("extends"), n("TypeVariable")),
            seqWS(i("extends"), n("ClassOrInterfaceType"), n("AdditionalBound").star)),
        "AdditionalBound" -> ListSet(
            seqWS(i("&"), n("InterfaceType"))),
        "TypeArguments" -> ListSet(
            seqWS(i("<"), n("TypeArgumentList"), i(">"))),
        "TypeArgumentList" -> ListSet(
            seqWS(n("TypeArgument"), seqWS(i(","), n("TypeArgument")).star)),
        "TypeArgument" -> ListSet(
            n("ReferenceType"),
            n("Wildcard")),
        "Wildcard" -> ListSet(
            seqWS(n("Annotation"), i("?"), n("WildcardBounds").opt)),
        "WildcardBounds" -> ListSet(
            seqWS(i("extends"), n("ReferenceType")),
            seqWS(i("super"), n("ReferenceType"))),

        // Productions from §6 (Names)
        "TypeName" -> ListSet(
            n("Identifier"),
            seqWS(n("PackageOrTypeName"), i("."), n("Identifier"))),
        "PackageOrTypeName" -> ListSet(
            n("Identifier"),
            seqWS(n("PackageOrTypeName"), i("."), n("Identifier"))),
        "ExpressionName" -> ListSet(
            n("Identifier"),
            seqWS(n("AmbiguousName"), i("."), n("Identifier"))),
        "MethodName" -> ListSet(n("Identifier")),
        "PackageName" -> ListSet(
            n("Identifier"),
            seqWS(n("PackageName"), i("."), n("Identifier"))),
        "AmbiguousName" -> ListSet(
            n("Identifier"),
            seqWS(n("AmbiguousName"), i("."), n("Identifier"))),

        // Productions from §7 (Packages)
        "CompilationUnit" -> ListSet(
            seqWS(n("PackageDeclaration").opt, n("ImportDeclaration").star, n("TypeDeclaration").star)),
        "PackageDeclaration" -> ListSet(
            seqWS(n("PackageModifier").star, i("package"), n("Identifier"), seqWS(i("."), n("Identifier")).star, i(";"))),
        "PackageModifier" -> ListSet(
            n("Annotation")),
        "ImportDeclaration" -> ListSet(
            n("SingleTypeImportDeclaration"),
            n("TypeImportOnDemandDeclaration"),
            n("SingleStaticImportDeclaration"),
            n("StaticImportOnDemandDeclaration")),
        "SingleTypeImportDeclaration" -> ListSet(
            seqWS(i("import"), n("TypeName"), i(";"))),
        "TypeImportOnDemandDeclaration" -> ListSet(
            seqWS(i("import"), n("PackageOrTypeName"), i("."), i("*"), i(";"))),
        "SingleStaticImportDeclaration" -> ListSet(
            seqWS(i("import"), i("static"), n("TypeName"), i("."), n("Identifier"), i(";"))),
        "StaticImportOnDemandDeclaration" -> ListSet(
            seqWS(i("import"), i("static"), n("TypeName"), i("."), i("*"), i(";"))),
        "TypeDeclaration" -> ListSet(
            n("ClassDeclaration"),
            n("InterfaceDeclaration")),

        // Productions from §8 (Classes)
        "ClassDeclaration" -> ListSet(
            n("NormalClassDeclaration"),
            n("EnumDeclaration")),
        "NormalClassDeclaration" -> ListSet(
            seqWS(n("ClassModifier").star, i("class"), n("Identifier"), n("TypeParameter").opt, n("Superclass").opt, n("Superinterface").opt, n("Classbody"))),
        "ClassModifier" -> ListSet(
            n("Annotation"),
            i("public"),
            i("protected"),
            i("private"),
            i("abstract"),
            i("static"),
            i("final"),
            i("strictfp")),
        "TypeParameters" -> ListSet(
            seqWS(i("<"), n("TypeParameterList"), i(">"))),
        "TypeParameterList" -> ListSet(
            seqWS(n("TypeParameter"), seqWS(i(","), n("TypeParameter")).star)),
        "Superclass" -> ListSet(
            seqWS(i("extends"), n("ClassType"))),
        "Superinterfaces" -> ListSet(
            seqWS(i("implements"), n("InterfaceTypeList"))),
        "InterfaceTypeList" -> ListSet(
            seqWS(n("InterfaceType"), seqWS(i(","), n("InterfaceType")).star)),
        "ClassBody" -> ListSet(
            seqWS(i("{"), n("ClassBodyDeclaration").star, i("}"))),
        "ClassBodyDeclaration" -> ListSet(
            n("ClassMemberDeclaration"),
            n("InstanceInitializer"),
            n("StaticInitializer"),
            n("ConstructorDeclaration")),
        "ClassMemberDeclaration" -> ListSet(
            n("FieldDeclaration"),
            n("MethodDeclaration"),
            n("ClassDeclaration"),
            n("InterfaceDeclaration"),
            i(";")),
        "FieldDeclaration" -> ListSet(
            // {FieldModifier} UnannType VariableDeclaratorList ;
            seqWS(n("FieldModifier").star, n("UnannType"), n("VariableDeclaratorList"), i(";"))),
        "FieldModifier" -> ListSet(
            n("Annotation"),
            i("public"),
            i("protected"),
            i("private"),
            i("static"),
            i("final"),
            i("transient"),
            i("volatile")),
        "VariableDeclaratorList" -> ListSet(
            seqWS(n("VariableDeclarator"), seqWS(i(","), n("VariableDeclarator")).star)),
        "VariableDeclarator" -> ListSet(
            seqWS(n("VariableDeclaratorId"), seqWS(i("="), n("VariableDeclaratorId")).opt)),
        "VariableDeclaratorId" -> ListSet(
            seqWS(n("Identifier"), n("Dims").opt)),
        "VariableInitializer" -> ListSet(
            n("Expression"),
            n("ArrayInitializer")),
        "UnannType" -> ListSet(
            n("UnannPrimitiveType"),
            n("UnannReferenceType")),
        "UnannPrimitiveType" -> ListSet(
            n("NumericType"),
            i("boolean")),
        "UnannReferenceType" -> ListSet(
            n("UnannClassOrInterfaceType"),
            n("UnannTypeVariable"),
            n("UnannArrayType")),
        "UnannClassOrInterfaceType" -> ListSet(
            n("UnannClassType"),
            n("UnannInterfaceType")),
        "UnannClassType" -> ListSet(),
        "UnannInterfaceType" -> ListSet(),
        "UnannTypeVariable" -> ListSet(),
        "UnannArrayType" -> ListSet(),
        "MethodDeclaration" -> ListSet(),
        "MethodModifier" -> ListSet(),
        "MethodHeader" -> ListSet(),
        "Result" -> ListSet(),
        "MethodDeclarator" -> ListSet(),
        "FormalParameterList" -> ListSet(),
        "FormalParameters" -> ListSet(),
        "FormalParameter" -> ListSet(),
        "VariableModifier" -> ListSet(),
        "LastFormalParameter" -> ListSet(),
        "ReceiverParameter" -> ListSet(),
        "Throws" -> ListSet(),
        "ExceptionTypeList" -> ListSet(),
        "ExceptionType" -> ListSet(),
        "MethodBody" -> ListSet(),
        "InstanceInitializer" -> ListSet(),
        "StaticInitializer" -> ListSet(),
        "ConstructorDeclaration" -> ListSet(),
        "ConstructorModifier" -> ListSet(),
        "ConstructorDeclarator" -> ListSet(),
        "SimpleTypeName" -> ListSet(),
        "ConstructorBody" -> ListSet(),
        "ExplicitConstructorInvocation" -> ListSet(),
        "EnumDeclaration" -> ListSet(),
        "EnumBody" -> ListSet(),
        "EnumConstantList" -> ListSet(),
        "EnumConstant" -> ListSet(),
        "EnumConstantModifier" -> ListSet(),
        "EnumBodyDeclarations" -> ListSet())

    val rules: RuleMap = lexical ++ cfg
    val startSymbol = n("CompilationUnit")
}
