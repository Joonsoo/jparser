package com.giyeok.jparser.j1

import com.giyeok.jparser.ParseResultTree._
import com.giyeok.jparser.j1.J1Grammar._
import com.giyeok.jparser.milestone.MilestoneParser
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat0
import com.giyeok.jparser.proto.{MilestoneParserDataProto, MilestoneParserProtobufConverter}
import com.giyeok.jparser.{Inputs, ParseForest, ParsingErrors}

import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream

object J1Grammar {

  sealed trait WithIdAndParseNode {
    val id: Int;
    val parseNode: Node
  }

  case class AdditionalBound(interfaceType: ClassOrInterfaceType)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  sealed trait AdditiveExpression extends ShiftExpression with WithIdAndParseNode

  case class AmbiguousName(name: List[Identifier])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  sealed trait AndExpression extends ExclusiveOrExpression with WithIdAndParseNode

  sealed trait ArrayAccess extends LeftHandSide with PrimaryNoNewArray with WithIdAndParseNode

  sealed trait ArrayCreationExpression extends Primary with WithIdAndParseNode

  sealed trait ArrayCreationExpressionWithInitializer extends ArrayCreationExpression with WithIdAndParseNode

  sealed trait ArrayCreationExpressionWithoutInitializer extends ArrayCreationExpression with WithIdAndParseNode

  case class ArrayInitializer(elems: List[VariableInitializer])(override val id: Int, override val parseNode: Node) extends VariableInitializer with WithIdAndParseNode

  case class ArrayType(elemType: Type, dims: List[Char])(override val id: Int, override val parseNode: Node) extends ReferenceType with WithIdAndParseNode

  case class Assignment(op: AssignmentOperator.Value, lhs: LeftHandSide, rhs: Expression)(override val id: Int, override val parseNode: Node) extends AssignmentExpression with StatementExpression with WithIdAndParseNode

  sealed trait AssignmentExpression extends Expression with WithIdAndParseNode

  case class BasicForStatement(forInit: Option[ForInit], cond: Option[Expression], forUpdate: Option[StatementExpressionList], body: Statement)(override val id: Int, override val parseNode: Node) extends StatementNoShortIf with WithIdAndParseNode

  case class BinOp(op: BinOps.Value, lhs: ConditionalOrExpression, rhs: ConditionalAndExpression)(override val id: Int, override val parseNode: Node) extends MultiplicativeExpression with WithIdAndParseNode

  case class BinaryIntegerLiteral(numeral: BinaryNumeral, suffix: Option[IntegerTypeSuffix.Value])(override val id: Int, override val parseNode: Node) extends IntegerLiteral with WithIdAndParseNode

  case class BinaryNumeral(value: String)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class Block(stmts: List[BlockStatement])(override val id: Int, override val parseNode: Node) extends LambdaBody with MethodBody with StatementWithoutTrailingSubstatement with WithIdAndParseNode

  sealed trait BlockStatement extends WithIdAndParseNode

  sealed trait BooleanLiteral extends Literal with WithIdAndParseNode

  case class BooleanType()(override val id: Int, override val parseNode: Node) extends PrimitiveType with WithIdAndParseNode

  case class BreakStatement(label: Option[Identifier])(override val id: Int, override val parseNode: Node) extends StatementWithoutTrailingSubstatement with WithIdAndParseNode

  case class ByteType()(override val id: Int, override val parseNode: Node) extends IntegralType with WithIdAndParseNode

  case class CanonicalClassOrInterfaceType(pkg: PackageName, typeId: TypeIdentifier, typeArgs: Option[TypeArguments])(override val id: Int, override val parseNode: Node) extends ClassOrInterfaceType with WithIdAndParseNode

  case class CanonicalLambdaParameters(params: List[LambdaParameter])(override val id: Int, override val parseNode: Node) extends LambdaParameterList with WithIdAndParseNode

  sealed trait CastExpression extends UnaryExpressionNotPlusMinus with WithIdAndParseNode

  case class CastLambda(typ: ReferenceType, bounds: List[AdditionalBound], operand: LambdaExpression)(override val id: Int, override val parseNode: Node) extends CastExpression with WithIdAndParseNode

  case class CastToPrimitiveType(typ: PrimitiveType, operand: UnaryExpression)(override val id: Int, override val parseNode: Node) extends CastExpression with WithIdAndParseNode

  case class CastToReferenceType(typ: ReferenceType, bounds: List[AdditionalBound], operand: UnaryExpressionNotPlusMinus)(override val id: Int, override val parseNode: Node) extends CastExpression with WithIdAndParseNode

  case class CatchClause(catchParams: CatchFormalParameter, body: Block)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class CatchFormalParameter(modifiers: List[VariableModifier.Value], typ: CatchType, decl: VariableDeclaratorId)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class CatchType(types: List[ClassOrInterfaceType])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class CharType()(override val id: Int, override val parseNode: Node) extends IntegralType with WithIdAndParseNode

  sealed trait CharacterLiteral extends Literal with WithIdAndParseNode

  case class ClassBody(decls: List[ClassBodyDeclaration])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  sealed trait ClassBodyDeclaration extends WithIdAndParseNode

  sealed trait ClassDeclaration extends TopLevelClassOrInterfaceDeclaration with WithIdAndParseNode

  case class ClassExtends(ext: ClassOrInterfaceType)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class ClassImplements(impls: List[ClassOrInterfaceType])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  sealed trait ClassInstanceCreationExpression extends PrimaryNoNewArray with StatementExpression with WithIdAndParseNode

  sealed trait ClassLiteral extends PrimaryNoNewArray with WithIdAndParseNode

  sealed trait ClassMemberDeclaration extends ClassBodyDeclaration with WithIdAndParseNode

  case class ClassOfBooleanType(dims: List[Char])(override val id: Int, override val parseNode: Node) extends ClassLiteral with WithIdAndParseNode

  case class ClassOfNumericType(typ: NumericType, dims: List[Char])(override val id: Int, override val parseNode: Node) extends ClassLiteral with WithIdAndParseNode

  case class ClassOfTypeName(typeName: TypeName, dims: List[Char])(override val id: Int, override val parseNode: Node) extends ClassLiteral with WithIdAndParseNode

  case class ClassOfVoidType()(override val id: Int, override val parseNode: Node) extends ClassLiteral with WithIdAndParseNode

  case class ClassOrInterfaceArrayCreationExpr(typ: ClassOrInterfaceType, dimExprs: List[DimExpr], dims: Option[List[Char]])(override val id: Int, override val parseNode: Node) extends ArrayCreationExpressionWithoutInitializer with WithIdAndParseNode

  case class ClassOrInterfaceArrayCreationExprWithInit(typ: ClassOrInterfaceType, dims: List[Char], initializer: ArrayInitializer)(override val id: Int, override val parseNode: Node) extends ArrayCreationExpressionWithInitializer with WithIdAndParseNode

  case class ClassOrInterfaceOfOtherType(parent: ClassOrInterfaceType, typeId: TypeIdentifier, typeArgs: Option[TypeArguments])(override val id: Int, override val parseNode: Node) extends ClassOrInterfaceType with WithIdAndParseNode

  sealed trait ClassOrInterfaceType extends ExceptionType with ReferenceType with WithIdAndParseNode

  sealed trait ConditionalAndExpression extends ConditionalOrExpression with WithIdAndParseNode

  sealed trait ConditionalExpression extends AssignmentExpression with WithIdAndParseNode

  case class ConditionalLambdaExpression(condition: ConditionalOrExpression, thenExpr: Expression, elseExpr: LambdaExpression)(override val id: Int, override val parseNode: Node) extends ConditionalExpression with WithIdAndParseNode

  sealed trait ConditionalOrExpression extends ConditionalExpression with WithIdAndParseNode

  case class ConditionalValueExpression(condition: ConditionalOrExpression, thenExpr: Expression, elseExpr: ConditionalExpression)(override val id: Int, override val parseNode: Node) extends ConditionalExpression with WithIdAndParseNode

  case class ConstantDeclaration(modifiers: List[ConstantModifier.Value], typ: Type, decls: List[VariableDeclarator])(override val id: Int, override val parseNode: Node) extends InterfaceMemberDeclaration with WithIdAndParseNode

  case class ConstructorBody(constructorInvocation: Option[Char], stmts: List[BlockStatement])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class ConstructorDeclaration(modifiers: List[ConstructorModifier.Value], decl: ConstructorDeclarator, thros: Option[Throws], body: ConstructorBody)(override val id: Int, override val parseNode: Node) extends ClassBodyDeclaration with WithIdAndParseNode

  case class ConstructorDeclarator(typeParams: Option[TypeParameters], name: TypeIdentifier, params: List[FormalParameter])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class ContextClassInstanceCreationExpressionByName(context: ExpressionName, creation: UnqualifiedClassInstanceCreationExpression)(override val id: Int, override val parseNode: Node) extends ClassInstanceCreationExpression with WithIdAndParseNode

  case class ContextClassInstanceCreationExpressionByPrimary(context: Primary, creation: UnqualifiedClassInstanceCreationExpression)(override val id: Int, override val parseNode: Node) extends ClassInstanceCreationExpression with WithIdAndParseNode

  case class ContinueStatement(label: Option[Identifier])(override val id: Int, override val parseNode: Node) extends StatementWithoutTrailingSubstatement with WithIdAndParseNode

  case class CreatedArrayAccess(array: ArrayCreationExpressionWithInitializer, index: Expression)(override val id: Int, override val parseNode: Node) extends ArrayAccess with WithIdAndParseNode

  case class DecimalIntegerLiteral(numeral: DecimalNumeral, suffix: Option[IntegerTypeSuffix.Value])(override val id: Int, override val parseNode: Node) extends IntegerLiteral with WithIdAndParseNode

  case class DecimalNumeral(value: String)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class DiamondTypeArguments()(override val id: Int, override val parseNode: Node) extends TypeArgumentsOrDiamond with WithIdAndParseNode

  case class DimExpr(expr: Expression)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class DoStatement(body: Statement, condition: Expression)(override val id: Int, override val parseNode: Node) extends StatementWithoutTrailingSubstatement with WithIdAndParseNode

  case class DoubleType()(override val id: Int, override val parseNode: Node) extends FloatingPointType with WithIdAndParseNode

  case class EmptyMethodBody()(override val id: Int, override val parseNode: Node) extends MethodBody with WithIdAndParseNode

  case class EmptyStatement()(override val id: Int, override val parseNode: Node) extends StatementWithoutTrailingSubstatement with WithIdAndParseNode

  case class EnumBody(consts: List[EnumConstant], decls: Option[List[ClassBodyDeclaration]])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class EnumConstant(name: Identifier, args: List[Expression])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class EnumDeclaration(modifiers: List[ClassModifier.Value], name: TypeIdentifier, impls: Option[ClassImplements], body: EnumBody)(override val id: Int, override val parseNode: Node) extends ClassDeclaration with WithIdAndParseNode

  sealed trait EqualityExpression extends AndExpression with WithIdAndParseNode

  case class EscapeCharacterLiteral(escapeSeq: EscapeSequence)(override val id: Int, override val parseNode: Node) extends CharacterLiteral with WithIdAndParseNode

  case class EscapeCode(code: Char)(override val id: Int, override val parseNode: Node) extends EscapeSequence with WithIdAndParseNode

  sealed trait EscapeSequence extends StringCharacter with WithIdAndParseNode

  sealed trait ExceptionType extends WithIdAndParseNode

  sealed trait ExclusiveOrExpression extends InclusiveOrExpression with WithIdAndParseNode

  case class ExponentPart(exponent: SignedInteger)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  sealed trait Expression extends LambdaBody with VariableInitializer with WithIdAndParseNode

  sealed trait ExpressionName extends LeftHandSide with PostfixExpression with WithIdAndParseNode

  case class ExpressionStatement(expression: StatementExpression)(override val id: Int, override val parseNode: Node) extends StatementWithoutTrailingSubstatement with WithIdAndParseNode

  case class ExtendingConcrete(typ: ClassOrInterfaceType, additional: List[AdditionalBound])(override val id: Int, override val parseNode: Node) extends TypeBound with WithIdAndParseNode

  case class ExtendsBound(typ: ReferenceType)(override val id: Int, override val parseNode: Node) extends WildcardBounds with WithIdAndParseNode

  case class FalseLiteral()(override val id: Int, override val parseNode: Node) extends BooleanLiteral with WithIdAndParseNode

  sealed trait FieldAccess extends LeftHandSide with PrimaryNoNewArray with WithIdAndParseNode

  case class FieldDeclaration(modifiers: List[FieldModifier.Value], typ: Type, decls: List[VariableDeclarator])(override val id: Int, override val parseNode: Node) extends ClassMemberDeclaration with WithIdAndParseNode

  case class FloatType()(override val id: Int, override val parseNode: Node) extends FloatingPointType with WithIdAndParseNode

  case class FloatingPointLiteral(intPart: Option[String], fracPart: Option[String], expPart: Option[ExponentPart], typeSuffix: Option[FloatTypeSuffix.Value])(override val id: Int, override val parseNode: Node) extends Literal with WithIdAndParseNode

  sealed trait FloatingPointType extends NumericType with WithIdAndParseNode

  sealed trait ForInit extends WithIdAndParseNode

  sealed trait FormalParameter extends WithIdAndParseNode

  case class HexIntegerLiteral(numeral: HexNumeral, suffix: Option[IntegerTypeSuffix.Value])(override val id: Int, override val parseNode: Node) extends IntegerLiteral with WithIdAndParseNode

  case class HexNumeral(value: String)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class Identifier(name: String)(override val id: Int, override val parseNode: Node) extends ExpressionName with FieldAccess with WithIdAndParseNode

  case class IfStatement(cond: Expression, thenPart: Statement, elsePart: Option[Statement])(override val id: Int, override val parseNode: Node) extends StatementNoShortIf with WithIdAndParseNode

  sealed trait ImportDeclaration extends WithIdAndParseNode

  sealed trait InclusiveOrExpression extends ConditionalAndExpression with WithIdAndParseNode

  case class InstanceInitializer(block: Block)(override val id: Int, override val parseNode: Node) extends ClassBodyDeclaration with WithIdAndParseNode

  case class InstanceofExpression(lhs: RelationalExpression, rhs: ReferenceType)(override val id: Int, override val parseNode: Node) extends RelationalExpression with WithIdAndParseNode

  case class IntType()(override val id: Int, override val parseNode: Node) extends IntegralType with WithIdAndParseNode

  sealed trait IntegerLiteral extends Literal with WithIdAndParseNode

  sealed trait IntegralType extends NumericType with WithIdAndParseNode

  case class InterfaceBody(decls: List[InterfaceMemberDeclaration])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  sealed trait InterfaceDeclaration extends TopLevelClassOrInterfaceDeclaration with WithIdAndParseNode

  case class InterfaceExtends(extendings: List[ClassOrInterfaceType])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  sealed trait InterfaceMemberDeclaration extends WithIdAndParseNode

  case class InterfaceMethodDeclaration(modifiers: List[InterfaceMethodModifier.Value], header: MethodHeader, body: MethodBody)(override val id: Int, override val parseNode: Node) extends InterfaceMemberDeclaration with WithIdAndParseNode

  case class LabeledStatement(label: Identifier, stmt: Statement)(override val id: Int, override val parseNode: Node) extends StatementNoShortIf with WithIdAndParseNode

  sealed trait LambdaBody extends WithIdAndParseNode

  case class LambdaExpression(params: LambdaParameters, body: LambdaBody)(override val id: Int, override val parseNode: Node) extends Expression with WithIdAndParseNode

  sealed trait LambdaParameter extends WithIdAndParseNode

  sealed trait LambdaParameterList extends WithIdAndParseNode

  sealed trait LambdaParameters extends WithIdAndParseNode

  sealed trait LeftHandSide extends WithIdAndParseNode

  sealed trait Literal extends PrimaryNoNewArray with WithIdAndParseNode

  case class LocalVariableDeclaration(modifiers: List[VariableModifier.Value], typ: Type, decls: List[VariableDeclarator])(override val id: Int, override val parseNode: Node) extends ForInit with WithIdAndParseNode

  case class LocalVariableDeclarationStatement(decl: LocalVariableDeclaration)(override val id: Int, override val parseNode: Node) extends BlockStatement with WithIdAndParseNode

  case class LongExpressionName(ambigName: AmbiguousName, name: Identifier)(override val id: Int, override val parseNode: Node) extends ExpressionName with WithIdAndParseNode

  case class LongType()(override val id: Int, override val parseNode: Node) extends IntegralType with WithIdAndParseNode

  sealed trait MethodBody extends WithIdAndParseNode

  case class MethodDeclaration(modifiers: List[MethodModifier.Value], header: MethodHeader, body: MethodBody)(override val id: Int, override val parseNode: Node) extends ClassMemberDeclaration with WithIdAndParseNode

  case class MethodDeclarator(name: Identifier, params: List[FormalParameter])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class MethodHeader(typeParams: Option[TypeParameters], resultType: Result, decl: MethodDeclarator, throws: Option[Throws])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  sealed trait MethodInvocation extends PrimaryNoNewArray with StatementExpression with WithIdAndParseNode

  case class MethodName(name: Identifier)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  sealed trait MethodReference extends PrimaryNoNewArray with WithIdAndParseNode

  sealed trait MultiplicativeExpression extends AdditiveExpression with WithIdAndParseNode

  case class NameArrayAccess(name: ExpressionName, index: Expression)(override val id: Int, override val parseNode: Node) extends ArrayAccess with WithIdAndParseNode

  case class NameMethodInvocation(target: ExpressionName, typeArgs: Option[TypeArguments], name: Identifier, args: List[Expression])(override val id: Int, override val parseNode: Node) extends MethodInvocation with WithIdAndParseNode

  case class NameOnlyLambdaParameters(params: List[Identifier])(override val id: Int, override val parseNode: Node) extends LambdaParameterList with WithIdAndParseNode

  case class NewArrayMethodReference(arrType: ArrayType)(override val id: Int, override val parseNode: Node) extends MethodReference with WithIdAndParseNode

  case class NewMethodReference(cls: ClassOrInterfaceType, typeArgs: Option[TypeArguments])(override val id: Int, override val parseNode: Node) extends MethodReference with WithIdAndParseNode

  case class NonSingleLambdaParameters(params: Option[LambdaParameterList])(override val id: Int, override val parseNode: Node) extends LambdaParameters with WithIdAndParseNode

  case class NormalClassDeclaration(mods: List[ClassModifier.Value], name: TypeIdentifier, typeParams: Option[TypeParameters], extendings: Option[ClassExtends], impls: Option[ClassImplements], body: ClassBody)(override val id: Int, override val parseNode: Node) extends ClassDeclaration with WithIdAndParseNode

  case class NormalInterfaceDeclaration(modifiers: List[InterfaceModifier.Value], name: TypeIdentifier, typeParams: Option[TypeParameters], extendings: Option[InterfaceExtends], body: InterfaceBody)(override val id: Int, override val parseNode: Node) extends InterfaceDeclaration with WithIdAndParseNode

  case class NullLiteral()(override val id: Int, override val parseNode: Node) extends Literal with WithIdAndParseNode

  sealed trait NumericType extends PrimitiveType with WithIdAndParseNode

  case class OctalEscapeSequence(value: String)(override val id: Int, override val parseNode: Node) extends EscapeSequence with WithIdAndParseNode

  case class OctalIntegerLiteral(numeral: OctalNumeral, suffix: Option[IntegerTypeSuffix.Value])(override val id: Int, override val parseNode: Node) extends IntegerLiteral with WithIdAndParseNode

  case class OctalNumeral(value: String)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class OrdinaryCompilationUnit(pkgDecl: Option[PackageDeclaration], imports: List[ImportDeclaration], decls: List[TopLevelClassOrInterfaceDeclaration])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class PackageDeclaration(pkg: List[Identifier])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class PackageName(name: List[Identifier])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class PackageOrTypeName(name: List[Identifier])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class Paren(expr: Expression)(override val id: Int, override val parseNode: Node) extends PrimaryNoNewArray with WithIdAndParseNode

  case class PlainMethodInvocation(target: MethodName, args: List[Expression])(override val id: Int, override val parseNode: Node) extends MethodInvocation with WithIdAndParseNode

  case class PlainNameMethodReference(cls: ExpressionName, typeArgs: Option[TypeArguments], name: Identifier)(override val id: Int, override val parseNode: Node) extends MethodReference with WithIdAndParseNode

  case class PlainPrimaryMethodReference(cls: Primary, typeArgs: Option[TypeArguments], name: Identifier)(override val id: Int, override val parseNode: Node) extends MethodReference with WithIdAndParseNode

  case class PlainRefMethodReference(cls: ReferenceType, typeArgs: Option[TypeArguments], name: Identifier)(override val id: Int, override val parseNode: Node) extends MethodReference with WithIdAndParseNode

  case class PostDecrementExpression(operand: PostfixExpression)(override val id: Int, override val parseNode: Node) extends PostfixExpression with StatementExpression with WithIdAndParseNode

  case class PostIncrementExpression(operand: PostfixExpression)(override val id: Int, override val parseNode: Node) extends PostfixExpression with StatementExpression with WithIdAndParseNode

  sealed trait PostfixExpression extends UnaryExpressionNotPlusMinus with WithIdAndParseNode

  sealed trait Primary extends PostfixExpression with WithIdAndParseNode

  case class PrimaryArrayAccess(name: PrimaryNoNewArray, index: Expression)(override val id: Int, override val parseNode: Node) extends ArrayAccess with WithIdAndParseNode

  case class PrimaryMethodInvocation(target: Primary, typeArgs: Option[TypeArguments], name: Identifier, args: List[Expression])(override val id: Int, override val parseNode: Node) extends MethodInvocation with WithIdAndParseNode

  sealed trait PrimaryNoNewArray extends Primary with WithIdAndParseNode

  case class PrimitiveArrayCreationExpr(typ: PrimitiveType, dimExprs: List[DimExpr], dims: Option[List[Char]])(override val id: Int, override val parseNode: Node) extends ArrayCreationExpressionWithoutInitializer with WithIdAndParseNode

  case class PrimitiveArrayCreationExprWithInit(typ: PrimitiveType, dims: List[Char], initializer: ArrayInitializer)(override val id: Int, override val parseNode: Node) extends ArrayCreationExpressionWithInitializer with WithIdAndParseNode

  sealed trait PrimitiveType extends Type with WithIdAndParseNode

  sealed trait ReferenceType extends Type with TypeArgument with WithIdAndParseNode

  sealed trait RelationalExpression extends EqualityExpression with WithIdAndParseNode

  sealed trait Result extends WithIdAndParseNode

  case class ReturnStatement(returnValue: Option[Expression])(override val id: Int, override val parseNode: Node) extends StatementWithoutTrailingSubstatement with WithIdAndParseNode

  sealed trait ShiftExpression extends RelationalExpression with WithIdAndParseNode

  case class ShortType()(override val id: Int, override val parseNode: Node) extends IntegralType with WithIdAndParseNode

  case class SignedInteger(sign: Option[Char], digits: String)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class SingleCharacter(value: Char)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class SingleCharacterLiteral(value: SingleCharacter)(override val id: Int, override val parseNode: Node) extends CharacterLiteral with WithIdAndParseNode

  case class SingleLambdaParameter(name: Identifier)(override val id: Int, override val parseNode: Node) extends LambdaParameters with WithIdAndParseNode

  case class SingleTypeImportDeclaration(name: TypeName)(override val id: Int, override val parseNode: Node) extends ImportDeclaration with WithIdAndParseNode

  case class SoloLambdaParameter(modifiers: List[VariableModifier.Value], typ: Type, decl: VariableDeclaratorId)(override val id: Int, override val parseNode: Node) extends LambdaParameter with WithIdAndParseNode

  case class SoloParameter(modifiers: List[VariableModifier.Value], paramType: Type, name: VariableDeclaratorId)(override val id: Int, override val parseNode: Node) extends FormalParameter with WithIdAndParseNode

  sealed trait Statement extends BlockStatement with WithIdAndParseNode

  sealed trait StatementExpression extends WithIdAndParseNode

  case class StatementExpressionList(exprs: List[StatementExpression])(override val id: Int, override val parseNode: Node) extends ForInit with WithIdAndParseNode

  sealed trait StatementNoShortIf extends Statement with WithIdAndParseNode

  sealed trait StatementWithoutTrailingSubstatement extends StatementNoShortIf with WithIdAndParseNode

  case class StaticInitializer(block: Block)(override val id: Int, override val parseNode: Node) extends ClassBodyDeclaration with WithIdAndParseNode

  sealed trait StringCharacter extends WithIdAndParseNode

  case class StringLiteral(chars: List[StringCharacter])(override val id: Int, override val parseNode: Node) extends Literal with WithIdAndParseNode

  case class StringSingleCharacter(value: Char)(override val id: Int, override val parseNode: Node) extends StringCharacter with WithIdAndParseNode

  case class SuperBound(typ: ReferenceType)(override val id: Int, override val parseNode: Node) extends WildcardBounds with WithIdAndParseNode

  case class SuperMethodInvocation(target: Option[TypeName], typeArgs: Option[TypeArguments], name: Identifier, args: List[Expression])(override val id: Int, override val parseNode: Node) extends MethodInvocation with WithIdAndParseNode

  case class SuperMethodReference(target: Option[TypeName], typeArgs: Option[TypeArguments], name: Identifier)(override val id: Int, override val parseNode: Node) extends MethodReference with WithIdAndParseNode

  case class This(parent: Option[TypeName])(override val id: Int, override val parseNode: Node) extends PrimaryNoNewArray with WithIdAndParseNode

  case class ThrowStatement(throwValue: Expression)(override val id: Int, override val parseNode: Node) extends StatementWithoutTrailingSubstatement with WithIdAndParseNode

  case class Throws(types: List[ExceptionType])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  sealed trait TopLevelClassOrInterfaceDeclaration extends WithIdAndParseNode

  case class TrueLiteral()(override val id: Int, override val parseNode: Node) extends BooleanLiteral with WithIdAndParseNode

  case class TryStatement(body: Block, catches: List[CatchClause], fin: Option[Block])(override val id: Int, override val parseNode: Node) extends StatementWithoutTrailingSubstatement with WithIdAndParseNode

  sealed trait Type extends Result with WithIdAndParseNode

  sealed trait TypeArgument extends WithIdAndParseNode

  case class TypeArguments(typeArgs: List[TypeArgument])(override val id: Int, override val parseNode: Node) extends TypeArgumentsOrDiamond with WithIdAndParseNode

  sealed trait TypeArgumentsOrDiamond extends WithIdAndParseNode

  sealed trait TypeBound extends WithIdAndParseNode

  case class TypeIdentifier(name: Identifier)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class TypeName(pkg: Option[PackageOrTypeName], name: TypeIdentifier)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class TypeParameter(typeName: TypeIdentifier, bound: Option[TypeBound])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class TypeParameters(params: List[TypeParameter])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class TypeToInstantiate(name: List[Identifier], typeArgs: Option[TypeArgumentsOrDiamond])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  sealed trait UnaryExpression extends MultiplicativeExpression with WithIdAndParseNode

  sealed trait UnaryExpressionNotPlusMinus extends UnaryExpression with WithIdAndParseNode

  case class UnaryOp(op: UnaryOps.Value, operand: UnaryExpression)(override val id: Int, override val parseNode: Node) extends StatementExpression with UnaryExpressionNotPlusMinus with WithIdAndParseNode

  case class UnqualifiedClassInstanceCreationExpression(typeArgs: Option[TypeArguments], typeToInstantiate: TypeToInstantiate, args: List[Expression], body: Option[ClassBody])(override val id: Int, override val parseNode: Node) extends ClassInstanceCreationExpression with WithIdAndParseNode

  case class UnqualifiedClassOrInterfaceType(name: TypeIdentifier, typeArgs: Option[TypeArguments])(override val id: Int, override val parseNode: Node) extends ClassOrInterfaceType with WithIdAndParseNode

  case class VariableArityParameter(modifiers: List[VariableModifier.Value], paramType: Type, name: Identifier)(override val id: Int, override val parseNode: Node) extends FormalParameter with LambdaParameter with WithIdAndParseNode

  case class VariableDeclarator(ident: VariableDeclaratorId, initializer: Option[VariableInitializer])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class VariableDeclaratorId(name: Identifier, dims: Option[List[Char]])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  sealed trait VariableInitializer extends WithIdAndParseNode

  case class VoidResult()(override val id: Int, override val parseNode: Node) extends Result with WithIdAndParseNode

  case class WhileStatement(cond: Expression, body: Statement)(override val id: Int, override val parseNode: Node) extends StatementNoShortIf with WithIdAndParseNode

  case class Wildcard(bounds: Option[WildcardBounds])(override val id: Int, override val parseNode: Node) extends TypeArgument with WithIdAndParseNode

  sealed trait WildcardBounds extends WithIdAndParseNode

  object AssignmentOperator extends Enumeration {
    val ADD_ASSIGN, AND_ASSIGN, ASSIGN, DIV_ASSIGN, MUL_ASSIGN, OR_ASSIGN, REM_ASSIGN, SHL_ASSIGN, SHRZ_ASSIGN, SHR_ASSIGN, SUB_ASSIGN, XOR_ASSIGN = Value
  }

  object BinOps extends Enumeration {
    val ADD, AND, CONDITIONAL_AND, CONDITIONAL_OR, DIV, EQ, EXCLUSIVE_OR, GE, GT, INCLUSIVE_OR, LE, LT, MUL, NE, REM, SHL, SHR, SHRZ, SUB = Value
  }

  object ClassModifier extends Enumeration {
    val ABSTRACT, FINAL, PRIVATE, PROTECTED, PUBLIC, STATIC = Value
  }

  object ConstantModifier extends Enumeration {
    val FINAL, PUBLIC, STATIC = Value
  }

  object ConstructorModifier extends Enumeration {
    val PRIVATE, PROTECTED, PUBLIC = Value
  }

  object FieldModifier extends Enumeration {
    val FINAL, PRIVATE, PROTECTED, PUBLIC, STATIC = Value
  }

  object FloatTypeSuffix extends Enumeration {
    val DOUBLE_TYPE, FLOAT_TYPE = Value
  }

  object IntegerTypeSuffix extends Enumeration {
    val LONG_TYPE = Value
  }

  object InterfaceMethodModifier extends Enumeration {
    val ABSTRACT, DEFAULT, PRIVATE, PUBLIC, STATIC = Value
  }

  object InterfaceModifier extends Enumeration {
    val ABSTRACT, PRIVATE, PROTECTED, PUBLIC, STATIC = Value
  }

  object MethodModifier extends Enumeration {
    val ABSTRACT, FINAL, NATIVE, PRIVATE, PROTECTED, PUBLIC, STATIC = Value
  }

  object UnaryOps extends Enumeration {
    val DECREMENT, INCREMENT, MINUS, NEG, PLUS, TILDE = Value
  }

  object VariableModifier extends Enumeration {
    val FINAL = Value
  }

  val milestoneParserData = MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(
    MilestoneParserDataProto.MilestoneParserData.parseFrom(
      new GZIPInputStream(new BufferedInputStream(getClass.getResourceAsStream("/j1_parserdata.pb.gz")))))

  val milestoneParser = new MilestoneParser(milestoneParserData)

  def parse(text: String): Either[ParseForest, ParsingErrors.ParsingError] =
    milestoneParser.parseAndReconstructToForest(text)

  def parseAst(text: String): Either[OrdinaryCompilationUnit, ParsingErrors.ParsingError] =
    parse(text) match {
      case Left(forest) =>
        if (forest.trees.size != 1) {
          val candidates = forest.trees.map(new J1Grammar().matchStart(_))
          throw new IllegalStateException(s"Ambiguous grammar: ${forest.trees.size}, \n${candidates.mkString("\n")}")
        }
        Left(new J1Grammar().matchStart(forest.trees.head))
      case Right(error) => Right(error)
    }
}

class J1Grammar {
  private var idCounter = 0

  def nextId(): Int = {
    idCounter += 1
    idCounter
  }

  def matchStart(node: Node): OrdinaryCompilationUnit = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchCompilationUnit(body)
  }

  def matchAdditionalBound(node: Node): AdditionalBound = {
    val BindNode(v1, v2) = node
    val v6 = v1.id match {
      case 575 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 576)
        AdditionalBound(matchInterfaceType(v5))(nextId(), v2)
    }
    v6
  }

  def matchAdditiveExpression(node: Node): AdditiveExpression = {
    val BindNode(v7, v8) = node
    val v24 = v7.id match {
      case 1175 =>
        val v9 = v8.asInstanceOf[SequenceNode].children.head
        val BindNode(v10, v11) = v9
        assert(v10.id == 1176)
        matchMultiplicativeExpression(v11)
      case 1184 =>
        val v12 = v8.asInstanceOf[SequenceNode].children.head
        val BindNode(v13, v14) = v12
        assert(v13.id == 1174)
        val v15 = v8.asInstanceOf[SequenceNode].children(4)
        val BindNode(v16, v17) = v15
        assert(v16.id == 1176)
        BinOp(BinOps.ADD, matchAdditiveExpression(v14), matchMultiplicativeExpression(v17))(nextId(), v8)
      case 1185 =>
        val v18 = v8.asInstanceOf[SequenceNode].children.head
        val BindNode(v19, v20) = v18
        assert(v19.id == 1174)
        val v21 = v8.asInstanceOf[SequenceNode].children(4)
        val BindNode(v22, v23) = v21
        assert(v22.id == 1176)
        BinOp(BinOps.SUB, matchAdditiveExpression(v20), matchMultiplicativeExpression(v23))(nextId(), v8)
    }
    v24
  }

  def matchAmbiguousName(node: Node): AmbiguousName = {
    val BindNode(v25, v26) = node
    val v42 = v25.id match {
      case 469 =>
        val v27 = v26.asInstanceOf[SequenceNode].children.head
        val BindNode(v28, v29) = v27
        assert(v28.id == 168)
        val v30 = v26.asInstanceOf[SequenceNode].children(1)
        val v31 = unrollRepeat0(v30).map { elem =>
          val BindNode(v32, v33) = elem
          assert(v32.id == 426)
          val BindNode(v34, v35) = v33
          val v41 = v34.id match {
            case 427 =>
              val BindNode(v36, v37) = v35
              assert(v36.id == 428)
              val v38 = v37.asInstanceOf[SequenceNode].children(3)
              val BindNode(v39, v40) = v38
              assert(v39.id == 168)
              matchIdentifier(v40)
          }
          v41
        }
        AmbiguousName(List(matchIdentifier(v29)) ++ v31)(nextId(), v26)
    }
    v42
  }

  def matchAndExpression(node: Node): AndExpression = {
    val BindNode(v43, v44) = node
    val v54 = v43.id match {
      case 1167 =>
        val v45 = v44.asInstanceOf[SequenceNode].children.head
        val BindNode(v46, v47) = v45
        assert(v46.id == 1168)
        matchEqualityExpression(v47)
      case 1208 =>
        val v48 = v44.asInstanceOf[SequenceNode].children.head
        val BindNode(v49, v50) = v48
        assert(v49.id == 1166)
        val v51 = v44.asInstanceOf[SequenceNode].children(4)
        val BindNode(v52, v53) = v51
        assert(v52.id == 1168)
        BinOp(BinOps.AND, matchAndExpression(v50), matchEqualityExpression(v53))(nextId(), v44)
    }
    v54
  }

  def matchArgumentList(node: Node): List[Expression] = {
    val BindNode(v55, v56) = node
    val v72 = v55.id match {
      case 901 =>
        val v57 = v56.asInstanceOf[SequenceNode].children.head
        val BindNode(v58, v59) = v57
        assert(v58.id == 644)
        val v60 = v56.asInstanceOf[SequenceNode].children(1)
        val v61 = unrollRepeat0(v60).map { elem =>
          val BindNode(v62, v63) = elem
          assert(v62.id == 904)
          val BindNode(v64, v65) = v63
          val v71 = v64.id match {
            case 905 =>
              val BindNode(v66, v67) = v65
              assert(v66.id == 906)
              val v68 = v67.asInstanceOf[SequenceNode].children(3)
              val BindNode(v69, v70) = v68
              assert(v69.id == 644)
              matchExpression(v70)
          }
          v71
        }
        List(matchExpression(v59)) ++ v61
    }
    v72
  }

  def matchArrayAccess(node: Node): ArrayAccess = {
    val BindNode(v73, v74) = node
    val v93 = v73.id match {
      case 918 =>
        val v75 = v74.asInstanceOf[SequenceNode].children.head
        val BindNode(v76, v77) = v75
        assert(v76.id == 720)
        val v78 = v74.asInstanceOf[SequenceNode].children(4)
        val BindNode(v79, v80) = v78
        assert(v79.id == 644)
        NameArrayAccess(matchExpressionName(v77), matchExpression(v80))(nextId(), v74)
      case 919 =>
        val v81 = v74.asInstanceOf[SequenceNode].children.head
        val BindNode(v82, v83) = v81
        assert(v82.id == 728)
        val v84 = v74.asInstanceOf[SequenceNode].children(4)
        val BindNode(v85, v86) = v84
        assert(v85.id == 644)
        PrimaryArrayAccess(matchPrimaryNoNewArray(v83), matchExpression(v86))(nextId(), v74)
      case 920 =>
        val v87 = v74.asInstanceOf[SequenceNode].children.head
        val BindNode(v88, v89) = v87
        assert(v88.id == 921)
        val v90 = v74.asInstanceOf[SequenceNode].children(4)
        val BindNode(v91, v92) = v90
        assert(v91.id == 644)
        CreatedArrayAccess(matchArrayCreationExpressionWithInitializer(v89), matchExpression(v92))(nextId(), v74)
    }
    v93
  }

  def matchArrayCreationExpression(node: Node): ArrayCreationExpression = {
    val BindNode(v94, v95) = node
    val v102 = v94.id match {
      case 969 =>
        val v96 = v95.asInstanceOf[SequenceNode].children.head
        val BindNode(v97, v98) = v96
        assert(v97.id == 970)
        matchArrayCreationExpressionWithoutInitializer(v98)
      case 982 =>
        val v99 = v95.asInstanceOf[SequenceNode].children.head
        val BindNode(v100, v101) = v99
        assert(v100.id == 921)
        matchArrayCreationExpressionWithInitializer(v101)
    }
    v102
  }

  def matchArrayCreationExpressionWithInitializer(node: Node): ArrayCreationExpressionWithInitializer = {
    val BindNode(v103, v104) = node
    val v123 = v103.id match {
      case 922 =>
        val v105 = v104.asInstanceOf[SequenceNode].children(2)
        val BindNode(v106, v107) = v105
        assert(v106.id == 525)
        val v108 = v104.asInstanceOf[SequenceNode].children(4)
        val BindNode(v109, v110) = v108
        assert(v109.id == 538)
        val v111 = v104.asInstanceOf[SequenceNode].children(6)
        val BindNode(v112, v113) = v111
        assert(v112.id == 923)
        PrimitiveArrayCreationExprWithInit(matchPrimitiveType(v107), matchDims(v110), matchArrayInitializer(v113))(nextId(), v104)
      case 941 =>
        val v114 = v104.asInstanceOf[SequenceNode].children(2)
        val BindNode(v115, v116) = v114
        assert(v115.id == 508)
        val v117 = v104.asInstanceOf[SequenceNode].children(4)
        val BindNode(v118, v119) = v117
        assert(v118.id == 538)
        val v120 = v104.asInstanceOf[SequenceNode].children(6)
        val BindNode(v121, v122) = v120
        assert(v121.id == 923)
        ClassOrInterfaceArrayCreationExprWithInit(matchClassOrInterfaceType(v116), matchDims(v119), matchArrayInitializer(v122))(nextId(), v104)
    }
    v123
  }

  def matchArrayCreationExpressionWithoutInitializer(node: Node): ArrayCreationExpressionWithoutInitializer = {
    val BindNode(v124, v125) = node
    val v166 = v124.id match {
      case 971 =>
        val v126 = v125.asInstanceOf[SequenceNode].children(2)
        val BindNode(v127, v128) = v126
        assert(v127.id == 525)
        val v129 = v125.asInstanceOf[SequenceNode].children(4)
        val BindNode(v130, v131) = v129
        assert(v130.id == 972)
        val v132 = v125.asInstanceOf[SequenceNode].children(5)
        val BindNode(v133, v134) = v132
        assert(v133.id == 634)
        val BindNode(v135, v136) = v134
        val v145 = v135.id match {
          case 430 =>
            None
          case 635 =>
            val BindNode(v137, v138) = v136
            val v144 = v137.id match {
              case 636 =>
                val BindNode(v139, v140) = v138
                assert(v139.id == 637)
                val v141 = v140.asInstanceOf[SequenceNode].children(1)
                val BindNode(v142, v143) = v141
                assert(v142.id == 538)
                matchDims(v143)
            }
            Some(v144)
        }
        PrimitiveArrayCreationExpr(matchPrimitiveType(v128), matchDimExprs(v131), v145)(nextId(), v125)
      case 981 =>
        val v146 = v125.asInstanceOf[SequenceNode].children(2)
        val BindNode(v147, v148) = v146
        assert(v147.id == 508)
        val v149 = v125.asInstanceOf[SequenceNode].children(4)
        val BindNode(v150, v151) = v149
        assert(v150.id == 972)
        val v152 = v125.asInstanceOf[SequenceNode].children(5)
        val BindNode(v153, v154) = v152
        assert(v153.id == 634)
        val BindNode(v155, v156) = v154
        val v165 = v155.id match {
          case 430 =>
            None
          case 635 =>
            val BindNode(v157, v158) = v156
            val v164 = v157.id match {
              case 636 =>
                val BindNode(v159, v160) = v158
                assert(v159.id == 637)
                val v161 = v160.asInstanceOf[SequenceNode].children(1)
                val BindNode(v162, v163) = v161
                assert(v162.id == 538)
                matchDims(v163)
            }
            Some(v164)
        }
        ClassOrInterfaceArrayCreationExpr(matchClassOrInterfaceType(v148), matchDimExprs(v151), v165)(nextId(), v125)
    }
    v166
  }

  def matchArrayInitializer(node: Node): ArrayInitializer = {
    val BindNode(v167, v168) = node
    val v184 = v167.id match {
      case 924 =>
        val v170 = v168.asInstanceOf[SequenceNode].children(1)
        val BindNode(v171, v172) = v170
        assert(v171.id == 925)
        val BindNode(v173, v174) = v172
        val v183 = v173.id match {
          case 430 =>
            None
          case 926 =>
            val BindNode(v175, v176) = v174
            val v182 = v175.id match {
              case 927 =>
                val BindNode(v177, v178) = v176
                assert(v177.id == 928)
                val v179 = v178.asInstanceOf[SequenceNode].children(1)
                val BindNode(v180, v181) = v179
                assert(v180.id == 929)
                matchVariableInitializerList(v181)
            }
            Some(v182)
        }
        val v169 = v183
        ArrayInitializer(if (v169.isDefined) v169.get else List())(nextId(), v168)
    }
    v184
  }

  def matchArrayType(node: Node): ArrayType = {
    val BindNode(v185, v186) = node
    val v199 = v185.id match {
      case 524 =>
        val v187 = v186.asInstanceOf[SequenceNode].children.head
        val BindNode(v188, v189) = v187
        assert(v188.id == 525)
        val v190 = v186.asInstanceOf[SequenceNode].children(2)
        val BindNode(v191, v192) = v190
        assert(v191.id == 538)
        ArrayType(matchPrimitiveType(v189), matchDims(v192))(nextId(), v186)
      case 547 =>
        val v193 = v186.asInstanceOf[SequenceNode].children.head
        val BindNode(v194, v195) = v193
        assert(v194.id == 508)
        val v196 = v186.asInstanceOf[SequenceNode].children(2)
        val BindNode(v197, v198) = v196
        assert(v197.id == 538)
        ArrayType(matchClassOrInterfaceType(v195), matchDims(v198))(nextId(), v186)
    }
    v199
  }

  def matchAssignment(node: Node): Assignment = {
    val BindNode(v200, v201) = node
    val v211 = v200.id match {
      case 717 =>
        val v202 = v201.asInstanceOf[SequenceNode].children(2)
        val BindNode(v203, v204) = v202
        assert(v203.id == 985)
        val v205 = v201.asInstanceOf[SequenceNode].children.head
        val BindNode(v206, v207) = v205
        assert(v206.id == 718)
        val v208 = v201.asInstanceOf[SequenceNode].children(4)
        val BindNode(v209, v210) = v208
        assert(v209.id == 644)
        Assignment(matchAssignmentOperator(v204), matchLeftHandSide(v207), matchExpression(v210))(nextId(), v201)
    }
    v211
  }

  def matchAssignmentExpression(node: Node): AssignmentExpression = {
    val BindNode(v212, v213) = node
    val v220 = v212.id match {
      case 1155 =>
        val v214 = v213.asInstanceOf[SequenceNode].children.head
        val BindNode(v215, v216) = v214
        assert(v215.id == 1156)
        matchConditionalExpression(v216)
      case 715 =>
        val v217 = v213.asInstanceOf[SequenceNode].children.head
        val BindNode(v218, v219) = v217
        assert(v218.id == 716)
        matchAssignment(v219)
    }
    v220
  }

  def matchAssignmentOperator(node: Node): AssignmentOperator.Value = {
    val BindNode(v221, v222) = node
    val v256 = v221.id match {
      case 986 =>
        val v223 = v222.asInstanceOf[SequenceNode].children.head
        val BindNode(v224, v225) = v223
        assert(v224.id == 987)
        val JoinNode(_, v226, _) = v225
        val BindNode(v227, v228) = v226
        assert(v227.id == 988)
        val BindNode(v229, v230) = v228
        val v255 = v229.id match {
          case 992 =>
            val BindNode(v231, v232) = v230
            assert(v231.id == 51)
            AssignmentOperator.REM_ASSIGN
          case 995 =>
            val BindNode(v233, v234) = v230
            assert(v233.id == 117)
            AssignmentOperator.SHL_ASSIGN
          case 991 =>
            val BindNode(v235, v236) = v230
            assert(v235.id == 103)
            AssignmentOperator.DIV_ASSIGN
          case 989 =>
            val BindNode(v237, v238) = v230
            assert(v237.id == 123)
            AssignmentOperator.ASSIGN
          case 994 =>
            val BindNode(v239, v240) = v230
            assert(v239.id == 88)
            AssignmentOperator.SUB_ASSIGN
          case 993 =>
            val BindNode(v241, v242) = v230
            assert(v241.id == 78)
            AssignmentOperator.ADD_ASSIGN
          case 997 =>
            val BindNode(v243, v244) = v230
            assert(v243.id == 144)
            AssignmentOperator.SHRZ_ASSIGN
          case 990 =>
            val BindNode(v245, v246) = v230
            assert(v245.id == 68)
            AssignmentOperator.MUL_ASSIGN
          case 996 =>
            val BindNode(v247, v248) = v230
            assert(v247.id == 138)
            AssignmentOperator.SHR_ASSIGN
          case 999 =>
            val BindNode(v249, v250) = v230
            assert(v249.id == 151)
            AssignmentOperator.XOR_ASSIGN
          case 998 =>
            val BindNode(v251, v252) = v230
            assert(v251.id == 61)
            AssignmentOperator.AND_ASSIGN
          case 1000 =>
            val BindNode(v253, v254) = v230
            assert(v253.id == 158)
            AssignmentOperator.OR_ASSIGN
        }
        v255
    }
    v256
  }

  def matchBasicForStatement(node: Node): BasicForStatement = {
    val BindNode(v257, v258) = node
    val v304 = v257.id match {
      case 1147 =>
        val v259 = v258.asInstanceOf[SequenceNode].children(3)
        val BindNode(v260, v261) = v259
        assert(v260.id == 1121)
        val BindNode(v262, v263) = v261
        val v272 = v262.id match {
          case 430 =>
            None
          case 1122 =>
            val BindNode(v264, v265) = v263
            val v271 = v264.id match {
              case 1123 =>
                val BindNode(v266, v267) = v265
                assert(v266.id == 1124)
                val v268 = v267.asInstanceOf[SequenceNode].children(1)
                val BindNode(v269, v270) = v268
                assert(v269.id == 1125)
                matchForInit(v270)
            }
            Some(v271)
        }
        val v273 = v258.asInstanceOf[SequenceNode].children(6)
        val BindNode(v274, v275) = v273
        assert(v274.id == 1054)
        val BindNode(v276, v277) = v275
        val v286 = v276.id match {
          case 430 =>
            None
          case 1055 =>
            val BindNode(v278, v279) = v277
            val v285 = v278.id match {
              case 1056 =>
                val BindNode(v280, v281) = v279
                assert(v280.id == 1057)
                val v282 = v281.asInstanceOf[SequenceNode].children(1)
                val BindNode(v283, v284) = v282
                assert(v283.id == 644)
                matchExpression(v284)
            }
            Some(v285)
        }
        val v287 = v258.asInstanceOf[SequenceNode].children(9)
        val BindNode(v288, v289) = v287
        assert(v288.id == 1135)
        val BindNode(v290, v291) = v289
        val v300 = v290.id match {
          case 430 =>
            None
          case 1136 =>
            val BindNode(v292, v293) = v291
            val v299 = v292.id match {
              case 1137 =>
                val BindNode(v294, v295) = v293
                assert(v294.id == 1138)
                val v296 = v295.asInstanceOf[SequenceNode].children(1)
                val BindNode(v297, v298) = v296
                assert(v297.id == 1139)
                matchForUpdate(v298)
            }
            Some(v299)
        }
        val v301 = v258.asInstanceOf[SequenceNode].children(13)
        val BindNode(v302, v303) = v301
        assert(v302.id == 705)
        BasicForStatement(v272, v286, v300, matchStatement(v303))(nextId(), v258)
    }
    v304
  }

  def matchBasicForStatementNoShortIf(node: Node): BasicForStatement = {
    val BindNode(v305, v306) = node
    val v352 = v305.id match {
      case 1119 =>
        val v307 = v306.asInstanceOf[SequenceNode].children(3)
        val BindNode(v308, v309) = v307
        assert(v308.id == 1121)
        val BindNode(v310, v311) = v309
        val v320 = v310.id match {
          case 430 =>
            None
          case 1122 =>
            val BindNode(v312, v313) = v311
            val v319 = v312.id match {
              case 1123 =>
                val BindNode(v314, v315) = v313
                assert(v314.id == 1124)
                val v316 = v315.asInstanceOf[SequenceNode].children(1)
                val BindNode(v317, v318) = v316
                assert(v317.id == 1125)
                matchForInit(v318)
            }
            Some(v319)
        }
        val v321 = v306.asInstanceOf[SequenceNode].children(6)
        val BindNode(v322, v323) = v321
        assert(v322.id == 1054)
        val BindNode(v324, v325) = v323
        val v334 = v324.id match {
          case 430 =>
            None
          case 1055 =>
            val BindNode(v326, v327) = v325
            val v333 = v326.id match {
              case 1056 =>
                val BindNode(v328, v329) = v327
                assert(v328.id == 1057)
                val v330 = v329.asInstanceOf[SequenceNode].children(1)
                val BindNode(v331, v332) = v330
                assert(v331.id == 644)
                matchExpression(v332)
            }
            Some(v333)
        }
        val v335 = v306.asInstanceOf[SequenceNode].children(9)
        val BindNode(v336, v337) = v335
        assert(v336.id == 1135)
        val BindNode(v338, v339) = v337
        val v348 = v338.id match {
          case 430 =>
            None
          case 1136 =>
            val BindNode(v340, v341) = v339
            val v347 = v340.id match {
              case 1137 =>
                val BindNode(v342, v343) = v341
                assert(v342.id == 1138)
                val v344 = v343.asInstanceOf[SequenceNode].children(1)
                val BindNode(v345, v346) = v344
                assert(v345.id == 1139)
                matchForUpdate(v346)
            }
            Some(v347)
        }
        val v349 = v306.asInstanceOf[SequenceNode].children(13)
        val BindNode(v350, v351) = v349
        assert(v350.id == 1103)
        BasicForStatement(v320, v334, v348, matchStatementNoShortIf(v351))(nextId(), v306)
    }
    v352
  }

  def matchBinaryDigit(node: Node): Char = {
    val BindNode(v353, v354) = node
    val v358 = v353.id match {
      case 795 =>
        val v355 = v354.asInstanceOf[SequenceNode].children(1)
        val BindNode(v356, v357) = v355
        assert(v356.id == 791)
        v357.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v358
  }

  def matchBinaryDigits(node: Node): String = {
    val BindNode(v359, v360) = node
    val v368 = v359.id match {
      case 790 =>
        val v361 = v360.asInstanceOf[SequenceNode].children.head
        val BindNode(v362, v363) = v361
        assert(v362.id == 791)
        val v364 = v360.asInstanceOf[SequenceNode].children(1)
        val v365 = unrollRepeat0(v364).map { elem =>
          val BindNode(v366, v367) = elem
          assert(v366.id == 794)
          matchBinaryDigit(v367)
        }
        v363.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v365.map(x => x.toString).mkString("")
    }
    v368
  }

  def matchBinaryIntegerLiteral(node: Node): BinaryIntegerLiteral = {
    val BindNode(v369, v370) = node
    val v388 = v369.id match {
      case 785 =>
        val v371 = v370.asInstanceOf[SequenceNode].children.head
        val BindNode(v372, v373) = v371
        assert(v372.id == 786)
        val v374 = v370.asInstanceOf[SequenceNode].children(1)
        val BindNode(v375, v376) = v374
        assert(v375.id == 751)
        val BindNode(v377, v378) = v376
        val v387 = v377.id match {
          case 430 =>
            None
          case 752 =>
            val BindNode(v379, v380) = v378
            val v386 = v379.id match {
              case 753 =>
                val BindNode(v381, v382) = v380
                assert(v381.id == 754)
                val v383 = v382.asInstanceOf[SequenceNode].children(1)
                val BindNode(v384, v385) = v383
                assert(v384.id == 755)
                matchIntegerTypeSuffix(v385)
            }
            Some(v386)
        }
        BinaryIntegerLiteral(matchBinaryNumeral(v373), v387)(nextId(), v370)
    }
    v388
  }

  def matchBinaryNumeral(node: Node): BinaryNumeral = {
    val BindNode(v389, v390) = node
    val v394 = v389.id match {
      case 787 =>
        val v391 = v390.asInstanceOf[SequenceNode].children(2)
        val BindNode(v392, v393) = v391
        assert(v392.id == 789)
        BinaryNumeral(matchBinaryDigits(v393))(nextId(), v390)
    }
    v394
  }

  def matchBlock(node: Node): Block = {
    val BindNode(v395, v396) = node
    val v412 = v395.id match {
      case 690 =>
        val v398 = v396.asInstanceOf[SequenceNode].children(1)
        val BindNode(v399, v400) = v398
        assert(v399.id == 691)
        val BindNode(v401, v402) = v400
        val v411 = v401.id match {
          case 430 =>
            None
          case 692 =>
            val BindNode(v403, v404) = v402
            val v410 = v403.id match {
              case 693 =>
                val BindNode(v405, v406) = v404
                assert(v405.id == 694)
                val v407 = v406.asInstanceOf[SequenceNode].children(1)
                val BindNode(v408, v409) = v407
                assert(v408.id == 695)
                matchBlockStatements(v409)
            }
            Some(v410)
        }
        val v397 = v411
        Block(if (v397.isDefined) v397.get else List())(nextId(), v396)
    }
    v412
  }

  def matchBlockStatement(node: Node): BlockStatement = {
    val BindNode(v413, v414) = node
    val v421 = v413.id match {
      case 698 =>
        val v415 = v414.asInstanceOf[SequenceNode].children.head
        val BindNode(v416, v417) = v415
        assert(v416.id == 699)
        matchLocalVariableDeclarationStatement(v417)
      case 704 =>
        val v418 = v414.asInstanceOf[SequenceNode].children.head
        val BindNode(v419, v420) = v418
        assert(v419.id == 705)
        matchStatement(v420)
    }
    v421
  }

  def matchBlockStatements(node: Node): List[BlockStatement] = {
    val BindNode(v422, v423) = node
    val v439 = v422.id match {
      case 696 =>
        val v424 = v423.asInstanceOf[SequenceNode].children.head
        val BindNode(v425, v426) = v424
        assert(v425.id == 697)
        val v427 = v423.asInstanceOf[SequenceNode].children(1)
        val v428 = unrollRepeat0(v427).map { elem =>
          val BindNode(v429, v430) = elem
          assert(v429.id == 1150)
          val BindNode(v431, v432) = v430
          val v438 = v431.id match {
            case 1151 =>
              val BindNode(v433, v434) = v432
              assert(v433.id == 1152)
              val v435 = v434.asInstanceOf[SequenceNode].children(1)
              val BindNode(v436, v437) = v435
              assert(v436.id == 697)
              matchBlockStatement(v437)
          }
          v438
        }
        List(matchBlockStatement(v426)) ++ v428
    }
    v439
  }

  def matchBooleanLiteral(node: Node): BooleanLiteral = {
    val BindNode(v440, v441) = node
    val v455 = v440.id match {
      case 821 =>
        val v442 = v441.asInstanceOf[SequenceNode].children.head
        val BindNode(v443, v444) = v442
        assert(v443.id == 822)
        val JoinNode(_, v445, _) = v444
        val BindNode(v446, v447) = v445
        assert(v446.id == 823)
        val BindNode(v448, v449) = v447
        val v454 = v448.id match {
          case 412 =>
            val BindNode(v450, v451) = v449
            assert(v450.id == 413)
            TrueLiteral()(nextId(), v451)
          case 416 =>
            val BindNode(v452, v453) = v449
            assert(v452.id == 417)
            FalseLiteral()(nextId(), v453)
        }
        v454
    }
    v455
  }

  def matchBreakStatement(node: Node): BreakStatement = {
    val BindNode(v456, v457) = node
    val v472 = v456.id match {
      case 1040 =>
        val v458 = v457.asInstanceOf[SequenceNode].children(1)
        val BindNode(v459, v460) = v458
        assert(v459.id == 1042)
        val BindNode(v461, v462) = v460
        val v471 = v461.id match {
          case 430 =>
            None
          case 1043 =>
            val BindNode(v463, v464) = v462
            val v470 = v463.id match {
              case 1044 =>
                val BindNode(v465, v466) = v464
                assert(v465.id == 1045)
                val v467 = v466.asInstanceOf[SequenceNode].children(1)
                val BindNode(v468, v469) = v467
                assert(v468.id == 168)
                matchIdentifier(v469)
            }
            Some(v470)
        }
        BreakStatement(v471)(nextId(), v457)
    }
    v472
  }

  def matchCastExpression(node: Node): CastExpression = {
    val BindNode(v473, v474) = node
    val v517 = v473.id match {
      case 1031 =>
        val v475 = v474.asInstanceOf[SequenceNode].children(2)
        val BindNode(v476, v477) = v475
        assert(v476.id == 525)
        val v478 = v474.asInstanceOf[SequenceNode].children(6)
        val BindNode(v479, v480) = v478
        assert(v479.id == 1005)
        CastToPrimitiveType(matchPrimitiveType(v477), matchUnaryExpression(v480))(nextId(), v474)
      case 1032 =>
        val v481 = v474.asInstanceOf[SequenceNode].children(2)
        val BindNode(v482, v483) = v481
        assert(v482.id == 520)
        val v484 = v474.asInstanceOf[SequenceNode].children(3)
        val v485 = unrollRepeat0(v484).map { elem =>
          val BindNode(v486, v487) = elem
          assert(v486.id == 571)
          val BindNode(v488, v489) = v487
          val v495 = v488.id match {
            case 572 =>
              val BindNode(v490, v491) = v489
              assert(v490.id == 573)
              val v492 = v491.asInstanceOf[SequenceNode].children(1)
              val BindNode(v493, v494) = v492
              assert(v493.id == 574)
              matchAdditionalBound(v494)
          }
          v495
        }
        val v496 = v474.asInstanceOf[SequenceNode].children(7)
        val BindNode(v497, v498) = v496
        assert(v497.id == 1015)
        CastToReferenceType(matchReferenceType(v483), v485, matchUnaryExpressionNotPlusMinus(v498))(nextId(), v474)
      case 1033 =>
        val v499 = v474.asInstanceOf[SequenceNode].children(2)
        val BindNode(v500, v501) = v499
        assert(v500.id == 520)
        val v502 = v474.asInstanceOf[SequenceNode].children(3)
        val v503 = unrollRepeat0(v502).map { elem =>
          val BindNode(v504, v505) = elem
          assert(v504.id == 571)
          val BindNode(v506, v507) = v505
          val v513 = v506.id match {
            case 572 =>
              val BindNode(v508, v509) = v507
              assert(v508.id == 573)
              val v510 = v509.asInstanceOf[SequenceNode].children(1)
              val BindNode(v511, v512) = v510
              assert(v511.id == 574)
              matchAdditionalBound(v512)
          }
          v513
        }
        val v514 = v474.asInstanceOf[SequenceNode].children(7)
        val BindNode(v515, v516) = v514
        assert(v515.id == 646)
        CastLambda(matchReferenceType(v501), v503, matchLambdaExpression(v516))(nextId(), v474)
    }
    v517
  }

  def matchCatchClause(node: Node): CatchClause = {
    val BindNode(v518, v519) = node
    val v526 = v518.id match {
      case 1069 =>
        val v520 = v519.asInstanceOf[SequenceNode].children(4)
        val BindNode(v521, v522) = v520
        assert(v521.id == 1071)
        val v523 = v519.asInstanceOf[SequenceNode].children(8)
        val BindNode(v524, v525) = v523
        assert(v524.id == 689)
        CatchClause(matchCatchFormalParameter(v522), matchBlock(v525))(nextId(), v519)
    }
    v526
  }

  def matchCatchFormalParameter(node: Node): CatchFormalParameter = {
    val BindNode(v527, v528) = node
    val v546 = v527.id match {
      case 1072 =>
        val v529 = v528.asInstanceOf[SequenceNode].children.head
        val v530 = unrollRepeat0(v529).map { elem =>
          val BindNode(v531, v532) = elem
          assert(v531.id == 662)
          val BindNode(v533, v534) = v532
          assert(v533.id == 663)
          val BindNode(v535, v536) = v534
          assert(v535.id == 664)
          val v537 = v536.asInstanceOf[SequenceNode].children.head
          val BindNode(v538, v539) = v537
          assert(v538.id == 665)
          matchVariableModifier(v539)
        }
        val v540 = v528.asInstanceOf[SequenceNode].children(1)
        val BindNode(v541, v542) = v540
        assert(v541.id == 1073)
        val v543 = v528.asInstanceOf[SequenceNode].children(3)
        val BindNode(v544, v545) = v543
        assert(v544.id == 632)
        CatchFormalParameter(v530, matchCatchType(v542), matchVariableDeclaratorId(v545))(nextId(), v528)
    }
    v546
  }

  def matchCatchType(node: Node): CatchType = {
    val BindNode(v547, v548) = node
    val v564 = v547.id match {
      case 1074 =>
        val v549 = v548.asInstanceOf[SequenceNode].children.head
        val BindNode(v550, v551) = v549
        assert(v550.id == 588)
        val v552 = v548.asInstanceOf[SequenceNode].children(1)
        val v553 = unrollRepeat0(v552).map { elem =>
          val BindNode(v554, v555) = elem
          assert(v554.id == 1077)
          val BindNode(v556, v557) = v555
          val v563 = v556.id match {
            case 1078 =>
              val BindNode(v558, v559) = v557
              assert(v558.id == 1079)
              val v560 = v559.asInstanceOf[SequenceNode].children(3)
              val BindNode(v561, v562) = v560
              assert(v561.id == 588)
              matchClassType(v562)
          }
          v563
        }
        CatchType(List(matchClassType(v551)) ++ v553)(nextId(), v548)
    }
    v564
  }

  def matchCatches(node: Node): List[CatchClause] = {
    val BindNode(v565, v566) = node
    val v582 = v565.id match {
      case 1067 =>
        val v567 = v566.asInstanceOf[SequenceNode].children.head
        val BindNode(v568, v569) = v567
        assert(v568.id == 1068)
        val v570 = v566.asInstanceOf[SequenceNode].children(1)
        val v571 = unrollRepeat0(v570).map { elem =>
          val BindNode(v572, v573) = elem
          assert(v572.id == 1082)
          val BindNode(v574, v575) = v573
          val v581 = v574.id match {
            case 1083 =>
              val BindNode(v576, v577) = v575
              assert(v576.id == 1084)
              val v578 = v577.asInstanceOf[SequenceNode].children(1)
              val BindNode(v579, v580) = v578
              assert(v579.id == 1068)
              matchCatchClause(v580)
          }
          v581
        }
        List(matchCatchClause(v569)) ++ v571
    }
    v582
  }

  def matchCharacterLiteral(node: Node): CharacterLiteral = {
    val BindNode(v583, v584) = node
    val v591 = v583.id match {
      case 826 =>
        val v585 = v584.asInstanceOf[SequenceNode].children(1)
        val BindNode(v586, v587) = v585
        assert(v586.id == 828)
        SingleCharacterLiteral(matchSingleCharacter(v587))(nextId(), v584)
      case 837 =>
        val v588 = v584.asInstanceOf[SequenceNode].children(1)
        val BindNode(v589, v590) = v588
        assert(v589.id == 838)
        EscapeCharacterLiteral(matchEscapeSequence(v590))(nextId(), v584)
    }
    v591
  }

  def matchClassBody(node: Node): ClassBody = {
    val BindNode(v592, v593) = node
    val v606 = v592.id match {
      case 604 =>
        val v594 = v593.asInstanceOf[SequenceNode].children(1)
        val v595 = unrollRepeat0(v594).map { elem =>
          val BindNode(v596, v597) = elem
          assert(v596.id == 608)
          val BindNode(v598, v599) = v597
          val v605 = v598.id match {
            case 609 =>
              val BindNode(v600, v601) = v599
              assert(v600.id == 610)
              val v602 = v601.asInstanceOf[SequenceNode].children(1)
              val BindNode(v603, v604) = v602
              assert(v603.id == 611)
              matchClassBodyDeclaration(v604)
          }
          v605
        }
        ClassBody(v595)(nextId(), v593)
    }
    v606
  }

  def matchClassBodyDeclaration(node: Node): ClassBodyDeclaration = {
    val BindNode(v607, v608) = node
    val v621 = v607.id match {
      case 612 =>
        val v609 = v608.asInstanceOf[SequenceNode].children.head
        val BindNode(v610, v611) = v609
        assert(v610.id == 613)
        matchClassMemberDeclaration(v611)
      case 1277 =>
        val v612 = v608.asInstanceOf[SequenceNode].children.head
        val BindNode(v613, v614) = v612
        assert(v613.id == 1278)
        matchInstanceInitializer(v614)
      case 1279 =>
        val v615 = v608.asInstanceOf[SequenceNode].children.head
        val BindNode(v616, v617) = v615
        assert(v616.id == 1280)
        matchStaticInitializer(v617)
      case 1283 =>
        val v618 = v608.asInstanceOf[SequenceNode].children.head
        val BindNode(v619, v620) = v618
        assert(v619.id == 1284)
        matchConstructorDeclaration(v620)
    }
    v621
  }

  def matchClassDeclaration(node: Node): ClassDeclaration = {
    val BindNode(v622, v623) = node
    val v630 = v622.id match {
      case 478 =>
        val v624 = v623.asInstanceOf[SequenceNode].children.head
        val BindNode(v625, v626) = v624
        assert(v625.id == 479)
        matchNormalClassDeclaration(v626)
      case 1313 =>
        val v627 = v623.asInstanceOf[SequenceNode].children.head
        val BindNode(v628, v629) = v627
        assert(v628.id == 1314)
        matchEnumDeclaration(v629)
    }
    v630
  }

  def matchClassExtends(node: Node): ClassExtends = {
    val BindNode(v631, v632) = node
    val v636 = v631.id match {
      case 587 =>
        val v633 = v632.asInstanceOf[SequenceNode].children(2)
        val BindNode(v634, v635) = v633
        assert(v634.id == 588)
        ClassExtends(matchClassType(v635))(nextId(), v632)
    }
    v636
  }

  def matchClassImplements(node: Node): ClassImplements = {
    val BindNode(v637, v638) = node
    val v642 = v637.id match {
      case 594 =>
        val v639 = v638.asInstanceOf[SequenceNode].children(2)
        val BindNode(v640, v641) = v639
        assert(v640.id == 596)
        ClassImplements(matchInterfaceTypeList(v641))(nextId(), v638)
    }
    v642
  }

  def matchClassInstanceCreationExpression(node: Node): ClassInstanceCreationExpression = {
    val BindNode(v643, v644) = node
    val v660 = v643.id match {
      case 883 =>
        val v645 = v644.asInstanceOf[SequenceNode].children.head
        val BindNode(v646, v647) = v645
        assert(v646.id == 884)
        matchUnqualifiedClassInstanceCreationExpression(v647)
      case 911 =>
        val v648 = v644.asInstanceOf[SequenceNode].children.head
        val BindNode(v649, v650) = v648
        assert(v649.id == 720)
        val v651 = v644.asInstanceOf[SequenceNode].children(4)
        val BindNode(v652, v653) = v651
        assert(v652.id == 884)
        ContextClassInstanceCreationExpressionByName(matchExpressionName(v650), matchUnqualifiedClassInstanceCreationExpression(v653))(nextId(), v644)
      case 915 =>
        val v654 = v644.asInstanceOf[SequenceNode].children.head
        val BindNode(v655, v656) = v654
        assert(v655.id == 726)
        val v657 = v644.asInstanceOf[SequenceNode].children(4)
        val BindNode(v658, v659) = v657
        assert(v658.id == 884)
        ContextClassInstanceCreationExpressionByPrimary(matchPrimary(v656), matchUnqualifiedClassInstanceCreationExpression(v659))(nextId(), v644)
    }
    v660
  }

  def matchClassLiteral(node: Node): ClassLiteral = {
    val BindNode(v661, v662) = node
    val v705 = v661.id match {
      case 871 =>
        val v663 = v662.asInstanceOf[SequenceNode].children.head
        val BindNode(v664, v665) = v663
        assert(v664.id == 441)
        val v666 = v662.asInstanceOf[SequenceNode].children(1)
        val v667 = unrollRepeat0(v666).map { elem =>
          val BindNode(v668, v669) = elem
          assert(v668.id == 544)
          val BindNode(v670, v671) = v669
          val v677 = v670.id match {
            case 545 =>
              val BindNode(v672, v673) = v671
              assert(v672.id == 546)
              val v674 = v673.asInstanceOf[SequenceNode].children(3)
              val BindNode(v675, v676) = v674
              assert(v675.id == 541)
              v676.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
          }
          v677
        }
        ClassOfTypeName(matchTypeName(v665), v667)(nextId(), v662)
      case 872 =>
        val v678 = v662.asInstanceOf[SequenceNode].children.head
        val BindNode(v679, v680) = v678
        assert(v679.id == 527)
        val v681 = v662.asInstanceOf[SequenceNode].children(1)
        val v682 = unrollRepeat0(v681).map { elem =>
          val BindNode(v683, v684) = elem
          assert(v683.id == 544)
          val BindNode(v685, v686) = v684
          val v692 = v685.id match {
            case 545 =>
              val BindNode(v687, v688) = v686
              assert(v687.id == 546)
              val v689 = v688.asInstanceOf[SequenceNode].children(3)
              val BindNode(v690, v691) = v689
              assert(v690.id == 541)
              v691.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
          }
          v692
        }
        ClassOfNumericType(matchNumericType(v680), v682)(nextId(), v662)
      case 873 =>
        val v693 = v662.asInstanceOf[SequenceNode].children(1)
        val v694 = unrollRepeat0(v693).map { elem =>
          val BindNode(v695, v696) = elem
          assert(v695.id == 544)
          val BindNode(v697, v698) = v696
          val v704 = v697.id match {
            case 545 =>
              val BindNode(v699, v700) = v698
              assert(v699.id == 546)
              val v701 = v700.asInstanceOf[SequenceNode].children(3)
              val BindNode(v702, v703) = v701
              assert(v702.id == 541)
              v703.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
          }
          v704
        }
        ClassOfBooleanType(v694)(nextId(), v662)
      case 875 =>
        ClassOfVoidType()(nextId(), v662)
    }
    v705
  }

  def matchClassMemberDeclaration(node: Node): ClassMemberDeclaration = {
    val BindNode(v706, v707) = node
    val v714 = v706.id match {
      case 614 =>
        val v708 = v707.asInstanceOf[SequenceNode].children.head
        val BindNode(v709, v710) = v708
        assert(v709.id == 615)
        matchFieldDeclaration(v710)
      case 1226 =>
        val v711 = v707.asInstanceOf[SequenceNode].children.head
        val BindNode(v712, v713) = v711
        assert(v712.id == 1227)
        matchMethodDeclaration(v713)
    }
    v714
  }

  def matchClassModifier(node: Node): ClassModifier.Value = {
    val BindNode(v715, v716) = node
    val v738 = v715.id match {
      case 487 =>
        val v717 = v716.asInstanceOf[SequenceNode].children.head
        val BindNode(v718, v719) = v717
        assert(v718.id == 488)
        val JoinNode(_, v720, _) = v719
        val BindNode(v721, v722) = v720
        assert(v721.id == 489)
        val BindNode(v723, v724) = v722
        val v737 = v723.id match {
          case 351 =>
            val BindNode(v725, v726) = v724
            assert(v725.id == 352)
            ClassModifier.FINAL
          case 256 =>
            val BindNode(v727, v728) = v724
            assert(v727.id == 257)
            ClassModifier.PRIVATE
          case 359 =>
            val BindNode(v729, v730) = v724
            assert(v729.id == 360)
            ClassModifier.STATIC
          case 191 =>
            val BindNode(v731, v732) = v724
            assert(v731.id == 192)
            ClassModifier.ABSTRACT
          case 298 =>
            val BindNode(v733, v734) = v724
            assert(v733.id == 299)
            ClassModifier.PUBLIC
          case 278 =>
            val BindNode(v735, v736) = v724
            assert(v735.id == 279)
            ClassModifier.PROTECTED
        }
        v737
    }
    v738
  }

  def matchClassOrInterfaceType(node: Node): ClassOrInterfaceType = {
    val BindNode(v739, v740) = node
    val v798 = v739.id match {
      case 509 =>
        val v741 = v740.asInstanceOf[SequenceNode].children.head
        val BindNode(v742, v743) = v741
        assert(v742.id == 443)
        val v744 = v740.asInstanceOf[SequenceNode].children(1)
        val BindNode(v745, v746) = v744
        assert(v745.id == 510)
        val BindNode(v747, v748) = v746
        val v757 = v747.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v749, v750) = v748
            val v756 = v749.id match {
              case 512 =>
                val BindNode(v751, v752) = v750
                assert(v751.id == 513)
                val v753 = v752.asInstanceOf[SequenceNode].children(1)
                val BindNode(v754, v755) = v753
                assert(v754.id == 514)
                matchTypeArguments(v755)
            }
            Some(v756)
        }
        UnqualifiedClassOrInterfaceType(matchTypeIdentifier(v743), v757)(nextId(), v740)
      case 566 =>
        val v758 = v740.asInstanceOf[SequenceNode].children.head
        val BindNode(v759, v760) = v758
        assert(v759.id == 567)
        val v761 = v740.asInstanceOf[SequenceNode].children(4)
        val BindNode(v762, v763) = v761
        assert(v762.id == 443)
        val v764 = v740.asInstanceOf[SequenceNode].children(5)
        val BindNode(v765, v766) = v764
        assert(v765.id == 510)
        val BindNode(v767, v768) = v766
        val v777 = v767.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v769, v770) = v768
            val v776 = v769.id match {
              case 512 =>
                val BindNode(v771, v772) = v770
                assert(v771.id == 513)
                val v773 = v772.asInstanceOf[SequenceNode].children(1)
                val BindNode(v774, v775) = v773
                assert(v774.id == 514)
                matchTypeArguments(v775)
            }
            Some(v776)
        }
        CanonicalClassOrInterfaceType(matchPackageName(v760), matchTypeIdentifier(v763), v777)(nextId(), v740)
      case 568 =>
        val v778 = v740.asInstanceOf[SequenceNode].children.head
        val BindNode(v779, v780) = v778
        assert(v779.id == 508)
        val v781 = v740.asInstanceOf[SequenceNode].children(4)
        val BindNode(v782, v783) = v781
        assert(v782.id == 443)
        val v784 = v740.asInstanceOf[SequenceNode].children(5)
        val BindNode(v785, v786) = v784
        assert(v785.id == 510)
        val BindNode(v787, v788) = v786
        val v797 = v787.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v789, v790) = v788
            val v796 = v789.id match {
              case 512 =>
                val BindNode(v791, v792) = v790
                assert(v791.id == 513)
                val v793 = v792.asInstanceOf[SequenceNode].children(1)
                val BindNode(v794, v795) = v793
                assert(v794.id == 514)
                matchTypeArguments(v795)
            }
            Some(v796)
        }
        ClassOrInterfaceOfOtherType(matchClassOrInterfaceType(v780), matchTypeIdentifier(v783), v797)(nextId(), v740)
    }
    v798
  }

  def matchClassOrInterfaceTypeToInstantiate(node: Node): TypeToInstantiate = {
    val BindNode(v799, v800) = node
    val v830 = v799.id match {
      case 888 =>
        val v801 = v800.asInstanceOf[SequenceNode].children.head
        val BindNode(v802, v803) = v801
        assert(v802.id == 168)
        val v804 = v800.asInstanceOf[SequenceNode].children(1)
        val v805 = unrollRepeat0(v804).map { elem =>
          val BindNode(v806, v807) = elem
          assert(v806.id == 426)
          val BindNode(v808, v809) = v807
          val v815 = v808.id match {
            case 427 =>
              val BindNode(v810, v811) = v809
              assert(v810.id == 428)
              val v812 = v811.asInstanceOf[SequenceNode].children(3)
              val BindNode(v813, v814) = v812
              assert(v813.id == 168)
              matchIdentifier(v814)
          }
          v815
        }
        val v816 = v800.asInstanceOf[SequenceNode].children(2)
        val BindNode(v817, v818) = v816
        assert(v817.id == 889)
        val BindNode(v819, v820) = v818
        val v829 = v819.id match {
          case 430 =>
            None
          case 890 =>
            val BindNode(v821, v822) = v820
            val v828 = v821.id match {
              case 891 =>
                val BindNode(v823, v824) = v822
                assert(v823.id == 892)
                val v825 = v824.asInstanceOf[SequenceNode].children(1)
                val BindNode(v826, v827) = v825
                assert(v826.id == 893)
                matchTypeArgumentsOrDiamond(v827)
            }
            Some(v828)
        }
        TypeToInstantiate(List(matchIdentifier(v803)) ++ v805, v829)(nextId(), v800)
    }
    v830
  }

  def matchClassType(node: Node): ClassOrInterfaceType = {
    val BindNode(v831, v832) = node
    val v836 = v831.id match {
      case 521 =>
        val v833 = v832.asInstanceOf[SequenceNode].children.head
        val BindNode(v834, v835) = v833
        assert(v834.id == 508)
        matchClassOrInterfaceType(v835)
    }
    v836
  }

  def matchCompilationUnit(node: Node): OrdinaryCompilationUnit = {
    val BindNode(v837, v838) = node
    val v842 = v837.id match {
      case 3 =>
        val v839 = v838.asInstanceOf[SequenceNode].children.head
        val BindNode(v840, v841) = v839
        assert(v840.id == 4)
        matchOrdinaryCompilationUnit(v841)
    }
    v842
  }

  def matchConditionalAndExpression(node: Node): ConditionalAndExpression = {
    val BindNode(v843, v844) = node
    val v854 = v843.id match {
      case 1161 =>
        val v845 = v844.asInstanceOf[SequenceNode].children.head
        val BindNode(v846, v847) = v845
        assert(v846.id == 1162)
        matchInclusiveOrExpression(v847)
      case 1214 =>
        val v848 = v844.asInstanceOf[SequenceNode].children.head
        val BindNode(v849, v850) = v848
        assert(v849.id == 1160)
        val v851 = v844.asInstanceOf[SequenceNode].children(4)
        val BindNode(v852, v853) = v851
        assert(v852.id == 1162)
        BinOp(BinOps.CONDITIONAL_AND, matchConditionalAndExpression(v850), matchInclusiveOrExpression(v853))(nextId(), v844)
    }
    v854
  }

  def matchConditionalExpression(node: Node): ConditionalExpression = {
    val BindNode(v855, v856) = node
    val v878 = v855.id match {
      case 1157 =>
        val v857 = v856.asInstanceOf[SequenceNode].children.head
        val BindNode(v858, v859) = v857
        assert(v858.id == 1158)
        matchConditionalOrExpression(v859)
      case 1218 =>
        val v860 = v856.asInstanceOf[SequenceNode].children.head
        val BindNode(v861, v862) = v860
        assert(v861.id == 1158)
        val v863 = v856.asInstanceOf[SequenceNode].children(4)
        val BindNode(v864, v865) = v863
        assert(v864.id == 644)
        val v866 = v856.asInstanceOf[SequenceNode].children(8)
        val BindNode(v867, v868) = v866
        assert(v867.id == 1156)
        ConditionalValueExpression(matchConditionalOrExpression(v862), matchExpression(v865), matchConditionalExpression(v868))(nextId(), v856)
      case 1219 =>
        val v869 = v856.asInstanceOf[SequenceNode].children.head
        val BindNode(v870, v871) = v869
        assert(v870.id == 1158)
        val v872 = v856.asInstanceOf[SequenceNode].children(4)
        val BindNode(v873, v874) = v872
        assert(v873.id == 644)
        val v875 = v856.asInstanceOf[SequenceNode].children(8)
        val BindNode(v876, v877) = v875
        assert(v876.id == 646)
        ConditionalLambdaExpression(matchConditionalOrExpression(v871), matchExpression(v874), matchLambdaExpression(v877))(nextId(), v856)
    }
    v878
  }

  def matchConditionalOrExpression(node: Node): ConditionalOrExpression = {
    val BindNode(v879, v880) = node
    val v890 = v879.id match {
      case 1159 =>
        val v881 = v880.asInstanceOf[SequenceNode].children.head
        val BindNode(v882, v883) = v881
        assert(v882.id == 1160)
        matchConditionalAndExpression(v883)
      case 1216 =>
        val v884 = v880.asInstanceOf[SequenceNode].children.head
        val BindNode(v885, v886) = v884
        assert(v885.id == 1158)
        val v887 = v880.asInstanceOf[SequenceNode].children(4)
        val BindNode(v888, v889) = v887
        assert(v888.id == 1160)
        BinOp(BinOps.CONDITIONAL_OR, matchConditionalOrExpression(v886), matchConditionalAndExpression(v889))(nextId(), v880)
    }
    v890
  }

  def matchConstantDeclaration(node: Node): ConstantDeclaration = {
    val BindNode(v891, v892) = node
    val v910 = v891.id match {
      case 1373 =>
        val v893 = v892.asInstanceOf[SequenceNode].children.head
        val v894 = unrollRepeat0(v893).map { elem =>
          val BindNode(v895, v896) = elem
          assert(v895.id == 1376)
          val BindNode(v897, v898) = v896
          assert(v897.id == 1377)
          val BindNode(v899, v900) = v898
          assert(v899.id == 1378)
          val v901 = v900.asInstanceOf[SequenceNode].children.head
          val BindNode(v902, v903) = v901
          assert(v902.id == 1379)
          matchConstantModifier(v903)
        }
        val v904 = v892.asInstanceOf[SequenceNode].children(1)
        val BindNode(v905, v906) = v904
        assert(v905.id == 626)
        val v907 = v892.asInstanceOf[SequenceNode].children(3)
        val BindNode(v908, v909) = v907
        assert(v908.id == 628)
        ConstantDeclaration(v894, matchType(v906), matchVariableDeclaratorList(v909))(nextId(), v892)
    }
    v910
  }

  def matchConstantModifier(node: Node): ConstantModifier.Value = {
    val BindNode(v911, v912) = node
    val v928 = v911.id match {
      case 1380 =>
        val v913 = v912.asInstanceOf[SequenceNode].children.head
        val BindNode(v914, v915) = v913
        assert(v914.id == 1381)
        val JoinNode(_, v916, _) = v915
        val BindNode(v917, v918) = v916
        assert(v917.id == 1382)
        val BindNode(v919, v920) = v918
        val v927 = v919.id match {
          case 298 =>
            val BindNode(v921, v922) = v920
            assert(v921.id == 299)
            ConstantModifier.PUBLIC
          case 359 =>
            val BindNode(v923, v924) = v920
            assert(v923.id == 360)
            ConstantModifier.STATIC
          case 351 =>
            val BindNode(v925, v926) = v920
            assert(v925.id == 352)
            ConstantModifier.FINAL
        }
        v927
    }
    v928
  }

  def matchConstructorBody(node: Node): ConstructorBody = {
    val BindNode(v929, v930) = node
    val v960 = v929.id match {
      case 1299 =>
        val v931 = v930.asInstanceOf[SequenceNode].children(1)
        val BindNode(v932, v933) = v931
        assert(v932.id == 1300)
        val BindNode(v934, v935) = v933
        val v944 = v934.id match {
          case 430 =>
            None
          case 1301 =>
            val BindNode(v936, v937) = v935
            val v943 = v936.id match {
              case 1302 =>
                val BindNode(v938, v939) = v937
                assert(v938.id == 1303)
                val v940 = v939.asInstanceOf[SequenceNode].children(1)
                val BindNode(v941, v942) = v940
                assert(v941.id == 1304)
                matchExplicitConstructorInvocation(v942)
            }
            Some(v943)
        }
        val v946 = v930.asInstanceOf[SequenceNode].children(2)
        val BindNode(v947, v948) = v946
        assert(v947.id == 691)
        val BindNode(v949, v950) = v948
        val v959 = v949.id match {
          case 430 =>
            None
          case 692 =>
            val BindNode(v951, v952) = v950
            val v958 = v951.id match {
              case 693 =>
                val BindNode(v953, v954) = v952
                assert(v953.id == 694)
                val v955 = v954.asInstanceOf[SequenceNode].children(1)
                val BindNode(v956, v957) = v955
                assert(v956.id == 695)
                matchBlockStatements(v957)
            }
            Some(v958)
        }
        val v945 = v959
        ConstructorBody(v944, if (v945.isDefined) v945.get else List())(nextId(), v930)
    }
    v960
  }

  def matchConstructorDeclaration(node: Node): ConstructorDeclaration = {
    val BindNode(v961, v962) = node
    val v994 = v961.id match {
      case 1285 =>
        val v963 = v962.asInstanceOf[SequenceNode].children.head
        val v964 = unrollRepeat0(v963).map { elem =>
          val BindNode(v965, v966) = elem
          assert(v965.id == 1288)
          val BindNode(v967, v968) = v966
          assert(v967.id == 1289)
          val BindNode(v969, v970) = v968
          assert(v969.id == 1290)
          val v971 = v970.asInstanceOf[SequenceNode].children.head
          val BindNode(v972, v973) = v971
          assert(v972.id == 1291)
          matchConstructorModifier(v973)
        }
        val v974 = v962.asInstanceOf[SequenceNode].children(1)
        val BindNode(v975, v976) = v974
        assert(v975.id == 1295)
        val v977 = v962.asInstanceOf[SequenceNode].children(2)
        val BindNode(v978, v979) = v977
        assert(v978.id == 1261)
        val BindNode(v980, v981) = v979
        val v990 = v980.id match {
          case 430 =>
            None
          case 1262 =>
            val BindNode(v982, v983) = v981
            val v989 = v982.id match {
              case 1263 =>
                val BindNode(v984, v985) = v983
                assert(v984.id == 1264)
                val v986 = v985.asInstanceOf[SequenceNode].children(1)
                val BindNode(v987, v988) = v986
                assert(v987.id == 1265)
                matchThrows(v988)
            }
            Some(v989)
        }
        val v991 = v962.asInstanceOf[SequenceNode].children(4)
        val BindNode(v992, v993) = v991
        assert(v992.id == 1298)
        ConstructorDeclaration(v964, matchConstructorDeclarator(v976), v990, matchConstructorBody(v993))(nextId(), v962)
    }
    v994
  }

  def matchConstructorDeclarator(node: Node): ConstructorDeclarator = {
    val BindNode(v995, v996) = node
    val v1028 = v995.id match {
      case 1296 =>
        val v997 = v996.asInstanceOf[SequenceNode].children.head
        val BindNode(v998, v999) = v997
        assert(v998.id == 1240)
        val BindNode(v1000, v1001) = v999
        val v1009 = v1000.id match {
          case 430 =>
            None
          case 1241 =>
            val BindNode(v1002, v1003) = v1001
            assert(v1002.id == 1242)
            val BindNode(v1004, v1005) = v1003
            assert(v1004.id == 1243)
            val v1006 = v1005.asInstanceOf[SequenceNode].children.head
            val BindNode(v1007, v1008) = v1006
            assert(v1007.id == 495)
            Some(matchTypeParameters(v1008))
        }
        val v1010 = v996.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1011, v1012) = v1010
        assert(v1011.id == 1297)
        val v1014 = v996.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1015, v1016) = v1014
        assert(v1015.id == 1248)
        val BindNode(v1017, v1018) = v1016
        val v1027 = v1017.id match {
          case 430 =>
            None
          case 1249 =>
            val BindNode(v1019, v1020) = v1018
            val v1026 = v1019.id match {
              case 1250 =>
                val BindNode(v1021, v1022) = v1020
                assert(v1021.id == 1251)
                val v1023 = v1022.asInstanceOf[SequenceNode].children(1)
                val BindNode(v1024, v1025) = v1023
                assert(v1024.id == 1252)
                matchFormalParameterList(v1025)
            }
            Some(v1026)
        }
        val v1013 = v1027
        ConstructorDeclarator(v1009, matchSimpleTypeName(v1012), if (v1013.isDefined) v1013.get else List())(nextId(), v996)
    }
    v1028
  }

  def matchConstructorModifier(node: Node): ConstructorModifier.Value = {
    val BindNode(v1029, v1030) = node
    val v1046 = v1029.id match {
      case 1292 =>
        val v1031 = v1030.asInstanceOf[SequenceNode].children.head
        val BindNode(v1032, v1033) = v1031
        assert(v1032.id == 1293)
        val JoinNode(_, v1034, _) = v1033
        val BindNode(v1035, v1036) = v1034
        assert(v1035.id == 1294)
        val BindNode(v1037, v1038) = v1036
        val v1045 = v1037.id match {
          case 298 =>
            val BindNode(v1039, v1040) = v1038
            assert(v1039.id == 299)
            ConstructorModifier.PUBLIC
          case 278 =>
            val BindNode(v1041, v1042) = v1038
            assert(v1041.id == 279)
            ConstructorModifier.PROTECTED
          case 256 =>
            val BindNode(v1043, v1044) = v1038
            assert(v1043.id == 257)
            ConstructorModifier.PRIVATE
        }
        v1045
    }
    v1046
  }

  def matchContinueStatement(node: Node): ContinueStatement = {
    val BindNode(v1047, v1048) = node
    val v1063 = v1047.id match {
      case 1048 =>
        val v1049 = v1048.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1050, v1051) = v1049
        assert(v1050.id == 1042)
        val BindNode(v1052, v1053) = v1051
        val v1062 = v1052.id match {
          case 430 =>
            None
          case 1043 =>
            val BindNode(v1054, v1055) = v1053
            val v1061 = v1054.id match {
              case 1044 =>
                val BindNode(v1056, v1057) = v1055
                assert(v1056.id == 1045)
                val v1058 = v1057.asInstanceOf[SequenceNode].children(1)
                val BindNode(v1059, v1060) = v1058
                assert(v1059.id == 168)
                matchIdentifier(v1060)
            }
            Some(v1061)
        }
        ContinueStatement(v1062)(nextId(), v1048)
    }
    v1063
  }

  def matchDecimalFloatingPointLiteral(node: Node): FloatingPointLiteral = {
    val BindNode(v1064, v1065) = node
    val v1123 = v1064.id match {
      case 800 =>
        val v1066 = v1065.asInstanceOf[SequenceNode].children.head
        val BindNode(v1067, v1068) = v1066
        assert(v1067.id == 742)
        val v1069 = v1065.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1070, v1071) = v1069
        assert(v1070.id == 741)
        val BindNode(v1072, v1073) = v1071
        val v1074 = v1072.id match {
          case 430 =>
            None
          case 742 =>
            Some(matchDigits(v1073))
        }
        val v1075 = v1065.asInstanceOf[SequenceNode].children(3)
        val BindNode(v1076, v1077) = v1075
        assert(v1076.id == 801)
        val BindNode(v1078, v1079) = v1077
        val v1080 = v1078.id match {
          case 430 =>
            None
          case 802 =>
            Some(matchExponentPart(v1079))
        }
        val v1081 = v1065.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1082, v1083) = v1081
        assert(v1082.id == 809)
        val BindNode(v1084, v1085) = v1083
        val v1086 = v1084.id match {
          case 430 =>
            None
          case 810 =>
            Some(matchFloatTypeSuffix(v1085))
        }
        FloatingPointLiteral(Some(matchDigits(v1068)), v1074, v1080, v1086)(nextId(), v1065)
      case 816 =>
        val v1087 = v1065.asInstanceOf[SequenceNode].children.head
        val BindNode(v1088, v1089) = v1087
        assert(v1088.id == 742)
        val v1090 = v1065.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1091, v1092) = v1090
        assert(v1091.id == 802)
        val v1093 = v1065.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1094, v1095) = v1093
        assert(v1094.id == 810)
        FloatingPointLiteral(Some(matchDigits(v1089)), None, Some(matchExponentPart(v1092)), Some(matchFloatTypeSuffix(v1095)))(nextId(), v1065)
      case 817 =>
        val v1096 = v1065.asInstanceOf[SequenceNode].children.head
        val BindNode(v1097, v1098) = v1096
        assert(v1097.id == 742)
        val v1099 = v1065.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1100, v1101) = v1099
        assert(v1100.id == 802)
        FloatingPointLiteral(Some(matchDigits(v1098)), None, Some(matchExponentPart(v1101)), None)(nextId(), v1065)
      case 815 =>
        val v1102 = v1065.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1103, v1104) = v1102
        assert(v1103.id == 742)
        val v1105 = v1065.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1106, v1107) = v1105
        assert(v1106.id == 801)
        val BindNode(v1108, v1109) = v1107
        val v1110 = v1108.id match {
          case 430 =>
            None
          case 802 =>
            Some(matchExponentPart(v1109))
        }
        val v1111 = v1065.asInstanceOf[SequenceNode].children(3)
        val BindNode(v1112, v1113) = v1111
        assert(v1112.id == 809)
        val BindNode(v1114, v1115) = v1113
        val v1116 = v1114.id match {
          case 430 =>
            None
          case 810 =>
            Some(matchFloatTypeSuffix(v1115))
        }
        FloatingPointLiteral(None, Some(matchDigits(v1104)), v1110, v1116)(nextId(), v1065)
      case 818 =>
        val v1117 = v1065.asInstanceOf[SequenceNode].children.head
        val BindNode(v1118, v1119) = v1117
        assert(v1118.id == 742)
        val v1120 = v1065.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1121, v1122) = v1120
        assert(v1121.id == 810)
        FloatingPointLiteral(Some(matchDigits(v1119)), None, None, Some(matchFloatTypeSuffix(v1122)))(nextId(), v1065)
    }
    v1123
  }

  def matchDecimalIntegerLiteral(node: Node): DecimalIntegerLiteral = {
    val BindNode(v1124, v1125) = node
    val v1143 = v1124.id match {
      case 735 =>
        val v1126 = v1125.asInstanceOf[SequenceNode].children.head
        val BindNode(v1127, v1128) = v1126
        assert(v1127.id == 736)
        val v1129 = v1125.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1130, v1131) = v1129
        assert(v1130.id == 751)
        val BindNode(v1132, v1133) = v1131
        val v1142 = v1132.id match {
          case 430 =>
            None
          case 752 =>
            val BindNode(v1134, v1135) = v1133
            val v1141 = v1134.id match {
              case 753 =>
                val BindNode(v1136, v1137) = v1135
                assert(v1136.id == 754)
                val v1138 = v1137.asInstanceOf[SequenceNode].children(1)
                val BindNode(v1139, v1140) = v1138
                assert(v1139.id == 755)
                matchIntegerTypeSuffix(v1140)
            }
            Some(v1141)
        }
        DecimalIntegerLiteral(matchDecimalNumeral(v1128), v1142)(nextId(), v1125)
    }
    v1143
  }

  def matchDecimalNumeral(node: Node): DecimalNumeral = {
    val BindNode(v1144, v1145) = node
    val v1155 = v1144.id match {
      case 737 =>
        DecimalNumeral("0")(nextId(), v1145)
      case 739 =>
        val v1146 = v1145.asInstanceOf[SequenceNode].children.head
        val BindNode(v1147, v1148) = v1146
        assert(v1147.id == 740)
        val v1149 = v1145.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1150, v1151) = v1149
        assert(v1150.id == 741)
        val BindNode(v1152, v1153) = v1151
        val v1154 = v1152.id match {
          case 430 =>
            None
          case 742 =>
            Some(matchDigits(v1153))
        }
        DecimalNumeral(v1148.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v1154.map(x => x).getOrElse(""))(nextId(), v1145)
    }
    v1155
  }

  def matchDigits(node: Node): String = {
    val BindNode(v1156, v1157) = node
    val v1173 = v1156.id match {
      case 743 =>
        val v1158 = v1157.asInstanceOf[SequenceNode].children.head
        val BindNode(v1159, v1160) = v1158
        assert(v1159.id == 744)
        val v1161 = v1157.asInstanceOf[SequenceNode].children(1)
        val v1162 = unrollRepeat0(v1161).map { elem =>
          val BindNode(v1163, v1164) = elem
          assert(v1163.id == 747)
          val BindNode(v1165, v1166) = v1164
          val v1172 = v1165.id match {
            case 748 =>
              val BindNode(v1167, v1168) = v1166
              assert(v1167.id == 749)
              val v1169 = v1168.asInstanceOf[SequenceNode].children(1)
              val BindNode(v1170, v1171) = v1169
              assert(v1170.id == 744)
              v1171.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
          }
          v1172
        }
        v1160.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v1162.map(x => x.toString).mkString("")
    }
    v1173
  }

  def matchDimExpr(node: Node): DimExpr = {
    val BindNode(v1174, v1175) = node
    val v1179 = v1174.id match {
      case 975 =>
        val v1176 = v1175.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1177, v1178) = v1176
        assert(v1177.id == 644)
        DimExpr(matchExpression(v1178))(nextId(), v1175)
    }
    v1179
  }

  def matchDimExprs(node: Node): List[DimExpr] = {
    val BindNode(v1180, v1181) = node
    val v1197 = v1180.id match {
      case 973 =>
        val v1182 = v1181.asInstanceOf[SequenceNode].children.head
        val BindNode(v1183, v1184) = v1182
        assert(v1183.id == 974)
        val v1185 = v1181.asInstanceOf[SequenceNode].children(1)
        val v1186 = unrollRepeat0(v1185).map { elem =>
          val BindNode(v1187, v1188) = elem
          assert(v1187.id == 978)
          val BindNode(v1189, v1190) = v1188
          val v1196 = v1189.id match {
            case 979 =>
              val BindNode(v1191, v1192) = v1190
              assert(v1191.id == 980)
              val v1193 = v1192.asInstanceOf[SequenceNode].children(1)
              val BindNode(v1194, v1195) = v1193
              assert(v1194.id == 974)
              matchDimExpr(v1195)
          }
          v1196
        }
        List(matchDimExpr(v1184)) ++ v1186
    }
    v1197
  }

  def matchDims(node: Node): List[Char] = {
    val BindNode(v1198, v1199) = node
    val v1215 = v1198.id match {
      case 539 =>
        val v1200 = v1199.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1201, v1202) = v1200
        assert(v1201.id == 541)
        val v1203 = v1199.asInstanceOf[SequenceNode].children(3)
        val v1204 = unrollRepeat0(v1203).map { elem =>
          val BindNode(v1205, v1206) = elem
          assert(v1205.id == 544)
          val BindNode(v1207, v1208) = v1206
          val v1214 = v1207.id match {
            case 545 =>
              val BindNode(v1209, v1210) = v1208
              assert(v1209.id == 546)
              val v1211 = v1210.asInstanceOf[SequenceNode].children(3)
              val BindNode(v1212, v1213) = v1211
              assert(v1212.id == 541)
              v1213.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
          }
          v1214
        }
        List(v1202.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char) ++ v1204
    }
    v1215
  }

  def matchDoStatement(node: Node): DoStatement = {
    val BindNode(v1216, v1217) = node
    val v1224 = v1216.id match {
      case 1036 =>
        val v1218 = v1217.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1219, v1220) = v1218
        assert(v1219.id == 705)
        val v1221 = v1217.asInstanceOf[SequenceNode].children(8)
        val BindNode(v1222, v1223) = v1221
        assert(v1222.id == 644)
        DoStatement(matchStatement(v1220), matchExpression(v1223))(nextId(), v1217)
    }
    v1224
  }

  def matchEmptyStatement(node: Node): EmptyStatement = {
    val BindNode(v1225, v1226) = node
    val v1227 = v1225.id match {
      case 710 =>
        EmptyStatement()(nextId(), v1226)
    }
    v1227
  }

  def matchEnumBody(node: Node): EnumBody = {
    val BindNode(v1228, v1229) = node
    val v1259 = v1228.id match {
      case 1318 =>
        val v1231 = v1229.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1232, v1233) = v1231
        assert(v1232.id == 1319)
        val BindNode(v1234, v1235) = v1233
        val v1244 = v1234.id match {
          case 430 =>
            None
          case 1320 =>
            val BindNode(v1236, v1237) = v1235
            val v1243 = v1236.id match {
              case 1321 =>
                val BindNode(v1238, v1239) = v1237
                assert(v1238.id == 1322)
                val v1240 = v1239.asInstanceOf[SequenceNode].children(1)
                val BindNode(v1241, v1242) = v1240
                assert(v1241.id == 1323)
                matchEnumConstantList(v1242)
            }
            Some(v1243)
        }
        val v1230 = v1244
        val v1245 = v1229.asInstanceOf[SequenceNode].children(3)
        val BindNode(v1246, v1247) = v1245
        assert(v1246.id == 1336)
        val BindNode(v1248, v1249) = v1247
        val v1258 = v1248.id match {
          case 430 =>
            None
          case 1337 =>
            val BindNode(v1250, v1251) = v1249
            val v1257 = v1250.id match {
              case 1338 =>
                val BindNode(v1252, v1253) = v1251
                assert(v1252.id == 1339)
                val v1254 = v1253.asInstanceOf[SequenceNode].children(1)
                val BindNode(v1255, v1256) = v1254
                assert(v1255.id == 1340)
                matchEnumBodyDeclarations(v1256)
            }
            Some(v1257)
        }
        EnumBody(if (v1230.isDefined) v1230.get else List(), v1258)(nextId(), v1229)
    }
    v1259
  }

  def matchEnumBodyDeclarations(node: Node): List[ClassBodyDeclaration] = {
    val BindNode(v1260, v1261) = node
    val v1274 = v1260.id match {
      case 1341 =>
        val v1262 = v1261.asInstanceOf[SequenceNode].children(1)
        val v1263 = unrollRepeat0(v1262).map { elem =>
          val BindNode(v1264, v1265) = elem
          assert(v1264.id == 608)
          val BindNode(v1266, v1267) = v1265
          val v1273 = v1266.id match {
            case 609 =>
              val BindNode(v1268, v1269) = v1267
              assert(v1268.id == 610)
              val v1270 = v1269.asInstanceOf[SequenceNode].children(1)
              val BindNode(v1271, v1272) = v1270
              assert(v1271.id == 611)
              matchClassBodyDeclaration(v1272)
          }
          v1273
        }
        v1263
    }
    v1274
  }

  def matchEnumConstant(node: Node): EnumConstant = {
    val BindNode(v1275, v1276) = node
    val v1305 = v1275.id match {
      case 1326 =>
        val v1277 = v1276.asInstanceOf[SequenceNode].children.head
        val BindNode(v1278, v1279) = v1277
        assert(v1278.id == 168)
        val v1281 = v1276.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1282, v1283) = v1281
        assert(v1282.id == 1327)
        val BindNode(v1284, v1285) = v1283
        val v1304 = v1284.id match {
          case 430 =>
            None
          case 1328 =>
            val BindNode(v1286, v1287) = v1285
            assert(v1286.id == 1329)
            val BindNode(v1288, v1289) = v1287
            assert(v1288.id == 1330)
            val v1290 = v1289.asInstanceOf[SequenceNode].children(2)
            val BindNode(v1291, v1292) = v1290
            assert(v1291.id == 896)
            val BindNode(v1293, v1294) = v1292
            val v1303 = v1293.id match {
              case 430 =>
                None
              case 897 =>
                val BindNode(v1295, v1296) = v1294
                val v1302 = v1295.id match {
                  case 898 =>
                    val BindNode(v1297, v1298) = v1296
                    assert(v1297.id == 899)
                    val v1299 = v1298.asInstanceOf[SequenceNode].children(1)
                    val BindNode(v1300, v1301) = v1299
                    assert(v1300.id == 900)
                    matchArgumentList(v1301)
                }
                Some(v1302)
            }
            v1303
        }
        val v1280 = v1304
        EnumConstant(matchIdentifier(v1279), if (v1280.isDefined) v1280.get else List())(nextId(), v1276)
    }
    v1305
  }

  def matchEnumConstantList(node: Node): List[EnumConstant] = {
    val BindNode(v1306, v1307) = node
    val v1323 = v1306.id match {
      case 1324 =>
        val v1308 = v1307.asInstanceOf[SequenceNode].children.head
        val BindNode(v1309, v1310) = v1308
        assert(v1309.id == 1325)
        val v1311 = v1307.asInstanceOf[SequenceNode].children(1)
        val v1312 = unrollRepeat0(v1311).map { elem =>
          val BindNode(v1313, v1314) = elem
          assert(v1313.id == 1333)
          val BindNode(v1315, v1316) = v1314
          val v1322 = v1315.id match {
            case 1334 =>
              val BindNode(v1317, v1318) = v1316
              assert(v1317.id == 1335)
              val v1319 = v1318.asInstanceOf[SequenceNode].children(3)
              val BindNode(v1320, v1321) = v1319
              assert(v1320.id == 1325)
              matchEnumConstant(v1321)
          }
          v1322
        }
        List(matchEnumConstant(v1310)) ++ v1312
    }
    v1323
  }

  def matchEnumDeclaration(node: Node): EnumDeclaration = {
    val BindNode(v1324, v1325) = node
    val v1357 = v1324.id match {
      case 1315 =>
        val v1326 = v1325.asInstanceOf[SequenceNode].children.head
        val v1327 = unrollRepeat0(v1326).map { elem =>
          val BindNode(v1328, v1329) = elem
          assert(v1328.id == 483)
          val BindNode(v1330, v1331) = v1329
          assert(v1330.id == 484)
          val BindNode(v1332, v1333) = v1331
          assert(v1332.id == 485)
          val v1334 = v1333.asInstanceOf[SequenceNode].children.head
          val BindNode(v1335, v1336) = v1334
          assert(v1335.id == 486)
          matchClassModifier(v1336)
        }
        val v1337 = v1325.asInstanceOf[SequenceNode].children(3)
        val BindNode(v1338, v1339) = v1337
        assert(v1338.id == 443)
        val v1340 = v1325.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1341, v1342) = v1340
        assert(v1341.id == 589)
        val BindNode(v1343, v1344) = v1342
        val v1353 = v1343.id match {
          case 430 =>
            None
          case 590 =>
            val BindNode(v1345, v1346) = v1344
            val v1352 = v1345.id match {
              case 591 =>
                val BindNode(v1347, v1348) = v1346
                assert(v1347.id == 592)
                val v1349 = v1348.asInstanceOf[SequenceNode].children(1)
                val BindNode(v1350, v1351) = v1349
                assert(v1350.id == 593)
                matchClassImplements(v1351)
            }
            Some(v1352)
        }
        val v1354 = v1325.asInstanceOf[SequenceNode].children(6)
        val BindNode(v1355, v1356) = v1354
        assert(v1355.id == 1317)
        EnumDeclaration(v1327, matchTypeIdentifier(v1339), v1353, matchEnumBody(v1356))(nextId(), v1325)
    }
    v1357
  }

  def matchEqualityExpression(node: Node): EqualityExpression = {
    val BindNode(v1358, v1359) = node
    val v1375 = v1358.id match {
      case 1169 =>
        val v1360 = v1359.asInstanceOf[SequenceNode].children.head
        val BindNode(v1361, v1362) = v1360
        assert(v1361.id == 1170)
        matchRelationalExpression(v1362)
      case 1204 =>
        val v1363 = v1359.asInstanceOf[SequenceNode].children.head
        val BindNode(v1364, v1365) = v1363
        assert(v1364.id == 1168)
        val v1366 = v1359.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1367, v1368) = v1366
        assert(v1367.id == 1170)
        BinOp(BinOps.EQ, matchEqualityExpression(v1365), matchRelationalExpression(v1368))(nextId(), v1359)
      case 1206 =>
        val v1369 = v1359.asInstanceOf[SequenceNode].children.head
        val BindNode(v1370, v1371) = v1369
        assert(v1370.id == 1168)
        val v1372 = v1359.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1373, v1374) = v1372
        assert(v1373.id == 1170)
        BinOp(BinOps.NE, matchEqualityExpression(v1371), matchRelationalExpression(v1374))(nextId(), v1359)
    }
    v1375
  }

  def matchEscapeSequence(node: Node): EscapeSequence = {
    val BindNode(v1376, v1377) = node
    val v1413 = v1376.id match {
      case 839 =>
        val v1378 = v1377.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1379, v1380) = v1378
        assert(v1379.id == 841)
        EscapeCode(v1380.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v1377)
      case 842 =>
        val v1381 = v1377.asInstanceOf[SequenceNode].children.head
        val BindNode(v1382, v1383) = v1381
        assert(v1382.id == 843)
        val BindNode(v1384, v1385) = v1383
        assert(v1384.id == 844)
        val BindNode(v1386, v1387) = v1385
        val v1412 = v1386.id match {
          case 845 =>
            val BindNode(v1388, v1389) = v1387
            assert(v1388.id == 846)
            val v1390 = v1389.asInstanceOf[SequenceNode].children(1)
            val BindNode(v1391, v1392) = v1390
            assert(v1391.id == 778)
            OctalEscapeSequence(v1392.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString)(nextId(), v1389)
          case 847 =>
            val BindNode(v1393, v1394) = v1387
            assert(v1393.id == 848)
            val v1395 = v1394.asInstanceOf[SequenceNode].children(1)
            val BindNode(v1396, v1397) = v1395
            assert(v1396.id == 778)
            val v1398 = v1394.asInstanceOf[SequenceNode].children(2)
            val BindNode(v1399, v1400) = v1398
            assert(v1399.id == 778)
            OctalEscapeSequence(v1397.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v1400.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString)(nextId(), v1394)
          case 849 =>
            val BindNode(v1401, v1402) = v1387
            assert(v1401.id == 850)
            val v1403 = v1402.asInstanceOf[SequenceNode].children(1)
            val BindNode(v1404, v1405) = v1403
            assert(v1404.id == 851)
            val v1406 = v1402.asInstanceOf[SequenceNode].children(2)
            val BindNode(v1407, v1408) = v1406
            assert(v1407.id == 778)
            val v1409 = v1402.asInstanceOf[SequenceNode].children(3)
            val BindNode(v1410, v1411) = v1409
            assert(v1410.id == 778)
            OctalEscapeSequence(v1405.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v1408.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v1411.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString)(nextId(), v1402)
        }
        v1412
    }
    v1413
  }

  def matchExceptionType(node: Node): ExceptionType = {
    val BindNode(v1414, v1415) = node
    val v1419 = v1414.id match {
      case 521 =>
        val v1416 = v1415.asInstanceOf[SequenceNode].children.head
        val BindNode(v1417, v1418) = v1416
        assert(v1417.id == 508)
        matchClassOrInterfaceType(v1418)
    }
    v1419
  }

  def matchExceptionTypeList(node: Node): List[ExceptionType] = {
    val BindNode(v1420, v1421) = node
    val v1437 = v1420.id match {
      case 1269 =>
        val v1422 = v1421.asInstanceOf[SequenceNode].children.head
        val BindNode(v1423, v1424) = v1422
        assert(v1423.id == 1270)
        val v1425 = v1421.asInstanceOf[SequenceNode].children(1)
        val v1426 = unrollRepeat0(v1425).map { elem =>
          val BindNode(v1427, v1428) = elem
          assert(v1427.id == 1273)
          val BindNode(v1429, v1430) = v1428
          val v1436 = v1429.id match {
            case 1274 =>
              val BindNode(v1431, v1432) = v1430
              assert(v1431.id == 1275)
              val v1433 = v1432.asInstanceOf[SequenceNode].children(3)
              val BindNode(v1434, v1435) = v1433
              assert(v1434.id == 1270)
              matchExceptionType(v1435)
          }
          v1436
        }
        List(matchExceptionType(v1424)) ++ v1426
    }
    v1437
  }

  def matchExclusiveOrExpression(node: Node): ExclusiveOrExpression = {
    val BindNode(v1438, v1439) = node
    val v1449 = v1438.id match {
      case 1165 =>
        val v1440 = v1439.asInstanceOf[SequenceNode].children.head
        val BindNode(v1441, v1442) = v1440
        assert(v1441.id == 1166)
        matchAndExpression(v1442)
      case 1210 =>
        val v1443 = v1439.asInstanceOf[SequenceNode].children.head
        val BindNode(v1444, v1445) = v1443
        assert(v1444.id == 1164)
        val v1446 = v1439.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1447, v1448) = v1446
        assert(v1447.id == 1166)
        BinOp(BinOps.EXCLUSIVE_OR, matchExclusiveOrExpression(v1445), matchAndExpression(v1448))(nextId(), v1439)
    }
    v1449
  }

  def matchExplicitConstructorInvocation(node: Node): Char = {
    val BindNode(v1450, v1451) = node
    val v1464 = v1450.id match {
      case 1305 =>
        val v1452 = v1451.asInstanceOf[SequenceNode].children(8)
        val BindNode(v1453, v1454) = v1452
        assert(v1453.id == 429)
        v1454.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
      case 1310 =>
        val v1455 = v1451.asInstanceOf[SequenceNode].children(8)
        val BindNode(v1456, v1457) = v1455
        assert(v1456.id == 429)
        v1457.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
      case 1311 =>
        val v1458 = v1451.asInstanceOf[SequenceNode].children(12)
        val BindNode(v1459, v1460) = v1458
        assert(v1459.id == 429)
        v1460.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
      case 1312 =>
        val v1461 = v1451.asInstanceOf[SequenceNode].children(12)
        val BindNode(v1462, v1463) = v1461
        assert(v1462.id == 429)
        v1463.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v1464
  }

  def matchExponentPart(node: Node): ExponentPart = {
    val BindNode(v1465, v1466) = node
    val v1470 = v1465.id match {
      case 803 =>
        val v1467 = v1466.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1468, v1469) = v1467
        assert(v1468.id == 805)
        ExponentPart(matchSignedInteger(v1469))(nextId(), v1466)
    }
    v1470
  }

  def matchExpression(node: Node): Expression = {
    val BindNode(v1471, v1472) = node
    val v1479 = v1471.id match {
      case 645 =>
        val v1473 = v1472.asInstanceOf[SequenceNode].children.head
        val BindNode(v1474, v1475) = v1473
        assert(v1474.id == 646)
        matchLambdaExpression(v1475)
      case 1153 =>
        val v1476 = v1472.asInstanceOf[SequenceNode].children.head
        val BindNode(v1477, v1478) = v1476
        assert(v1477.id == 1154)
        matchAssignmentExpression(v1478)
    }
    v1479
  }

  def matchExpressionName(node: Node): ExpressionName = {
    val BindNode(v1480, v1481) = node
    val v1491 = v1480.id match {
      case 649 =>
        val v1482 = v1481.asInstanceOf[SequenceNode].children.head
        val BindNode(v1483, v1484) = v1482
        assert(v1483.id == 168)
        matchIdentifier(v1484)
      case 721 =>
        val v1485 = v1481.asInstanceOf[SequenceNode].children.head
        val BindNode(v1486, v1487) = v1485
        assert(v1486.id == 722)
        val v1488 = v1481.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1489, v1490) = v1488
        assert(v1489.id == 168)
        LongExpressionName(matchAmbiguousName(v1487), matchIdentifier(v1490))(nextId(), v1481)
    }
    v1491
  }

  def matchExpressionStatement(node: Node): ExpressionStatement = {
    val BindNode(v1492, v1493) = node
    val v1497 = v1492.id match {
      case 713 =>
        val v1494 = v1493.asInstanceOf[SequenceNode].children.head
        val BindNode(v1495, v1496) = v1494
        assert(v1495.id == 714)
        ExpressionStatement(matchStatementExpression(v1496))(nextId(), v1493)
    }
    v1497
  }

  def matchFieldAccess(node: Node): FieldAccess = {
    val BindNode(v1498, v1499) = node
    val v1509 = v1498.id match {
      case 725 =>
        val v1500 = v1499.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1501, v1502) = v1500
        assert(v1501.id == 168)
        matchIdentifier(v1502)
      case 983 =>
        val v1503 = v1499.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1504, v1505) = v1503
        assert(v1504.id == 168)
        matchIdentifier(v1505)
      case 984 =>
        val v1506 = v1499.asInstanceOf[SequenceNode].children(8)
        val BindNode(v1507, v1508) = v1506
        assert(v1507.id == 168)
        matchIdentifier(v1508)
    }
    v1509
  }

  def matchFieldDeclaration(node: Node): FieldDeclaration = {
    val BindNode(v1510, v1511) = node
    val v1529 = v1510.id match {
      case 616 =>
        val v1512 = v1511.asInstanceOf[SequenceNode].children.head
        val v1513 = unrollRepeat0(v1512).map { elem =>
          val BindNode(v1514, v1515) = elem
          assert(v1514.id == 619)
          val BindNode(v1516, v1517) = v1515
          assert(v1516.id == 620)
          val BindNode(v1518, v1519) = v1517
          assert(v1518.id == 621)
          val v1520 = v1519.asInstanceOf[SequenceNode].children.head
          val BindNode(v1521, v1522) = v1520
          assert(v1521.id == 622)
          matchFieldModifier(v1522)
        }
        val v1523 = v1511.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1524, v1525) = v1523
        assert(v1524.id == 626)
        val v1526 = v1511.asInstanceOf[SequenceNode].children(3)
        val BindNode(v1527, v1528) = v1526
        assert(v1527.id == 628)
        FieldDeclaration(v1513, matchType(v1525), matchVariableDeclaratorList(v1528))(nextId(), v1511)
    }
    v1529
  }

  def matchFieldModifier(node: Node): FieldModifier.Value = {
    val BindNode(v1530, v1531) = node
    val v1551 = v1530.id match {
      case 623 =>
        val v1532 = v1531.asInstanceOf[SequenceNode].children.head
        val BindNode(v1533, v1534) = v1532
        assert(v1533.id == 624)
        val JoinNode(_, v1535, _) = v1534
        val BindNode(v1536, v1537) = v1535
        assert(v1536.id == 625)
        val BindNode(v1538, v1539) = v1537
        val v1550 = v1538.id match {
          case 351 =>
            val BindNode(v1540, v1541) = v1539
            assert(v1540.id == 352)
            FieldModifier.FINAL
          case 256 =>
            val BindNode(v1542, v1543) = v1539
            assert(v1542.id == 257)
            FieldModifier.PRIVATE
          case 359 =>
            val BindNode(v1544, v1545) = v1539
            assert(v1544.id == 360)
            FieldModifier.STATIC
          case 298 =>
            val BindNode(v1546, v1547) = v1539
            assert(v1546.id == 299)
            FieldModifier.PUBLIC
          case 278 =>
            val BindNode(v1548, v1549) = v1539
            assert(v1548.id == 279)
            FieldModifier.PROTECTED
        }
        v1550
    }
    v1551
  }

  def matchFinally(node: Node): Block = {
    val BindNode(v1552, v1553) = node
    val v1557 = v1552.id match {
      case 1091 =>
        val v1554 = v1553.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1555, v1556) = v1554
        assert(v1555.id == 689)
        matchBlock(v1556)
    }
    v1557
  }

  def matchFloatTypeSuffix(node: Node): FloatTypeSuffix.Value = {
    val BindNode(v1558, v1559) = node
    val v1560 = v1558.id match {
      case 811 =>
        FloatTypeSuffix.FLOAT_TYPE
      case 813 =>
        FloatTypeSuffix.DOUBLE_TYPE
    }
    v1560
  }

  def matchFloatingPointLiteral(node: Node): FloatingPointLiteral = {
    val BindNode(v1561, v1562) = node
    val v1566 = v1561.id match {
      case 798 =>
        val v1563 = v1562.asInstanceOf[SequenceNode].children.head
        val BindNode(v1564, v1565) = v1563
        assert(v1564.id == 799)
        matchDecimalFloatingPointLiteral(v1565)
    }
    v1566
  }

  def matchFloatingPointType(node: Node): FloatingPointType = {
    val BindNode(v1567, v1568) = node
    val v1582 = v1567.id match {
      case 535 =>
        val v1569 = v1568.asInstanceOf[SequenceNode].children.head
        val BindNode(v1570, v1571) = v1569
        assert(v1570.id == 536)
        val JoinNode(_, v1572, _) = v1571
        val BindNode(v1573, v1574) = v1572
        assert(v1573.id == 537)
        val BindNode(v1575, v1576) = v1574
        val v1581 = v1575.id match {
          case 391 =>
            val BindNode(v1577, v1578) = v1576
            assert(v1577.id == 392)
            FloatType()(nextId(), v1578)
          case 269 =>
            val BindNode(v1579, v1580) = v1576
            assert(v1579.id == 270)
            DoubleType()(nextId(), v1580)
        }
        v1581
    }
    v1582
  }

  def matchForInit(node: Node): ForInit = {
    val BindNode(v1583, v1584) = node
    val v1591 = v1583.id match {
      case 1126 =>
        val v1585 = v1584.asInstanceOf[SequenceNode].children.head
        val BindNode(v1586, v1587) = v1585
        assert(v1586.id == 1127)
        matchStatementExpressionList(v1587)
      case 1134 =>
        val v1588 = v1584.asInstanceOf[SequenceNode].children.head
        val BindNode(v1589, v1590) = v1588
        assert(v1589.id == 701)
        matchLocalVariableDeclaration(v1590)
    }
    v1591
  }

  def matchForStatement(node: Node): BasicForStatement = {
    val BindNode(v1592, v1593) = node
    val v1597 = v1592.id match {
      case 1145 =>
        val v1594 = v1593.asInstanceOf[SequenceNode].children.head
        val BindNode(v1595, v1596) = v1594
        assert(v1595.id == 1146)
        matchBasicForStatement(v1596)
    }
    v1597
  }

  def matchForStatementNoShortIf(node: Node): BasicForStatement = {
    val BindNode(v1598, v1599) = node
    val v1603 = v1598.id match {
      case 1117 =>
        val v1600 = v1599.asInstanceOf[SequenceNode].children.head
        val BindNode(v1601, v1602) = v1600
        assert(v1601.id == 1118)
        matchBasicForStatementNoShortIf(v1602)
    }
    v1603
  }

  def matchForUpdate(node: Node): StatementExpressionList = {
    val BindNode(v1604, v1605) = node
    val v1609 = v1604.id match {
      case 1126 =>
        val v1606 = v1605.asInstanceOf[SequenceNode].children.head
        val BindNode(v1607, v1608) = v1606
        assert(v1607.id == 1127)
        matchStatementExpressionList(v1608)
    }
    v1609
  }

  def matchFormalParameter(node: Node): FormalParameter = {
    val BindNode(v1610, v1611) = node
    val v1632 = v1610.id match {
      case 1255 =>
        val v1612 = v1611.asInstanceOf[SequenceNode].children.head
        val v1613 = unrollRepeat0(v1612).map { elem =>
          val BindNode(v1614, v1615) = elem
          assert(v1614.id == 662)
          val BindNode(v1616, v1617) = v1615
          assert(v1616.id == 663)
          val BindNode(v1618, v1619) = v1617
          assert(v1618.id == 664)
          val v1620 = v1619.asInstanceOf[SequenceNode].children.head
          val BindNode(v1621, v1622) = v1620
          assert(v1621.id == 665)
          matchVariableModifier(v1622)
        }
        val v1623 = v1611.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1624, v1625) = v1623
        assert(v1624.id == 626)
        val v1626 = v1611.asInstanceOf[SequenceNode].children(3)
        val BindNode(v1627, v1628) = v1626
        assert(v1627.id == 632)
        SoloParameter(v1613, matchType(v1625), matchVariableDeclaratorId(v1628))(nextId(), v1611)
      case 670 =>
        val v1629 = v1611.asInstanceOf[SequenceNode].children.head
        val BindNode(v1630, v1631) = v1629
        assert(v1630.id == 671)
        matchVariableArityParameter(v1631)
    }
    v1632
  }

  def matchFormalParameterList(node: Node): List[FormalParameter] = {
    val BindNode(v1633, v1634) = node
    val v1650 = v1633.id match {
      case 1253 =>
        val v1635 = v1634.asInstanceOf[SequenceNode].children.head
        val BindNode(v1636, v1637) = v1635
        assert(v1636.id == 1254)
        val v1638 = v1634.asInstanceOf[SequenceNode].children(1)
        val v1639 = unrollRepeat0(v1638).map { elem =>
          val BindNode(v1640, v1641) = elem
          assert(v1640.id == 1258)
          val BindNode(v1642, v1643) = v1641
          val v1649 = v1642.id match {
            case 1259 =>
              val BindNode(v1644, v1645) = v1643
              assert(v1644.id == 1260)
              val v1646 = v1645.asInstanceOf[SequenceNode].children(3)
              val BindNode(v1647, v1648) = v1646
              assert(v1647.id == 1254)
              matchFormalParameter(v1648)
          }
          v1649
        }
        List(matchFormalParameter(v1637)) ++ v1639
    }
    v1650
  }

  def matchHexDigit(node: Node): Char = {
    val BindNode(v1651, v1652) = node
    val v1656 = v1651.id match {
      case 770 =>
        val v1653 = v1652.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1654, v1655) = v1653
        assert(v1654.id == 766)
        v1655.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v1656
  }

  def matchHexDigits(node: Node): String = {
    val BindNode(v1657, v1658) = node
    val v1666 = v1657.id match {
      case 765 =>
        val v1659 = v1658.asInstanceOf[SequenceNode].children.head
        val BindNode(v1660, v1661) = v1659
        assert(v1660.id == 766)
        val v1662 = v1658.asInstanceOf[SequenceNode].children(1)
        val v1663 = unrollRepeat0(v1662).map { elem =>
          val BindNode(v1664, v1665) = elem
          assert(v1664.id == 769)
          matchHexDigit(v1665)
        }
        v1661.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v1663.map(x => x.toString).mkString("")
    }
    v1666
  }

  def matchHexIntegerLiteral(node: Node): HexIntegerLiteral = {
    val BindNode(v1667, v1668) = node
    val v1686 = v1667.id match {
      case 760 =>
        val v1669 = v1668.asInstanceOf[SequenceNode].children.head
        val BindNode(v1670, v1671) = v1669
        assert(v1670.id == 761)
        val v1672 = v1668.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1673, v1674) = v1672
        assert(v1673.id == 751)
        val BindNode(v1675, v1676) = v1674
        val v1685 = v1675.id match {
          case 430 =>
            None
          case 752 =>
            val BindNode(v1677, v1678) = v1676
            val v1684 = v1677.id match {
              case 753 =>
                val BindNode(v1679, v1680) = v1678
                assert(v1679.id == 754)
                val v1681 = v1680.asInstanceOf[SequenceNode].children(1)
                val BindNode(v1682, v1683) = v1681
                assert(v1682.id == 755)
                matchIntegerTypeSuffix(v1683)
            }
            Some(v1684)
        }
        HexIntegerLiteral(matchHexNumeral(v1671), v1685)(nextId(), v1668)
    }
    v1686
  }

  def matchHexNumeral(node: Node): HexNumeral = {
    val BindNode(v1687, v1688) = node
    val v1692 = v1687.id match {
      case 762 =>
        val v1689 = v1688.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1690, v1691) = v1689
        assert(v1690.id == 764)
        HexNumeral(matchHexDigits(v1691))(nextId(), v1688)
    }
    v1692
  }

  def matchIdentifier(node: Node): Identifier = {
    val BindNode(v1693, v1694) = node
    val v1710 = v1693.id match {
      case 169 =>
        val v1695 = v1694.asInstanceOf[SequenceNode].children.head
        val BindNode(v1696, v1697) = v1695
        assert(v1696.id == 170)
        val BindNode(v1698, v1699) = v1697
        assert(v1698.id == 171)
        val BindNode(v1700, v1701) = v1699
        assert(v1700.id == 172)
        val BindNode(v1702, v1703) = v1701
        val v1709 = v1702.id match {
          case 173 =>
            val BindNode(v1704, v1705) = v1703
            assert(v1704.id == 174)
            val v1706 = v1705.asInstanceOf[SequenceNode].children.head
            val BindNode(v1707, v1708) = v1706
            assert(v1707.id == 175)
            matchIdentifierChars(v1708)
        }
        Identifier(v1709)(nextId(), v1694)
    }
    v1710
  }

  def matchIdentifierChars(node: Node): String = {
    val BindNode(v1711, v1712) = node
    val v1720 = v1711.id match {
      case 176 =>
        val v1713 = v1712.asInstanceOf[SequenceNode].children.head
        val BindNode(v1714, v1715) = v1713
        assert(v1714.id == 177)
        val v1716 = v1712.asInstanceOf[SequenceNode].children(1)
        val v1717 = unrollRepeat0(v1716).map { elem =>
          val BindNode(v1718, v1719) = elem
          assert(v1718.id == 182)
          matchJavaLetterOrDigit(v1719)
        }
        matchJavaLetter(v1715).toString + v1717.map(x => x.toString).mkString("")
    }
    v1720
  }

  def matchIfThenElseStatement(node: Node): IfStatement = {
    val BindNode(v1721, v1722) = node
    val v1732 = v1721.id match {
      case 1102 =>
        val v1723 = v1722.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1724, v1725) = v1723
        assert(v1724.id == 644)
        val v1726 = v1722.asInstanceOf[SequenceNode].children(8)
        val BindNode(v1727, v1728) = v1726
        assert(v1727.id == 1103)
        val v1729 = v1722.asInstanceOf[SequenceNode].children(12)
        val BindNode(v1730, v1731) = v1729
        assert(v1730.id == 705)
        IfStatement(matchExpression(v1725), matchStatementNoShortIf(v1728), Some(matchStatement(v1731)))(nextId(), v1722)
    }
    v1732
  }

  def matchIfThenElseStatementNoShortIf(node: Node): IfStatement = {
    val BindNode(v1733, v1734) = node
    val v1744 = v1733.id match {
      case 1109 =>
        val v1735 = v1734.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1736, v1737) = v1735
        assert(v1736.id == 644)
        val v1738 = v1734.asInstanceOf[SequenceNode].children(8)
        val BindNode(v1739, v1740) = v1738
        assert(v1739.id == 1103)
        val v1741 = v1734.asInstanceOf[SequenceNode].children(12)
        val BindNode(v1742, v1743) = v1741
        assert(v1742.id == 1103)
        IfStatement(matchExpression(v1737), matchStatementNoShortIf(v1740), Some(matchStatementNoShortIf(v1743)))(nextId(), v1734)
    }
    v1744
  }

  def matchIfThenStatement(node: Node): IfStatement = {
    val BindNode(v1745, v1746) = node
    val v1753 = v1745.id match {
      case 1098 =>
        val v1747 = v1746.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1748, v1749) = v1747
        assert(v1748.id == 644)
        val v1750 = v1746.asInstanceOf[SequenceNode].children(8)
        val BindNode(v1751, v1752) = v1750
        assert(v1751.id == 705)
        IfStatement(matchExpression(v1749), matchStatement(v1752), None)(nextId(), v1746)
    }
    v1753
  }

  def matchImportDeclaration(node: Node): ImportDeclaration = {
    val BindNode(v1754, v1755) = node
    val v1759 = v1754.id match {
      case 437 =>
        val v1756 = v1755.asInstanceOf[SequenceNode].children.head
        val BindNode(v1757, v1758) = v1756
        assert(v1757.id == 438)
        matchSingleTypeImportDeclaration(v1758)
    }
    v1759
  }

  def matchInclusiveOrExpression(node: Node): InclusiveOrExpression = {
    val BindNode(v1760, v1761) = node
    val v1771 = v1760.id match {
      case 1163 =>
        val v1762 = v1761.asInstanceOf[SequenceNode].children.head
        val BindNode(v1763, v1764) = v1762
        assert(v1763.id == 1164)
        matchExclusiveOrExpression(v1764)
      case 1212 =>
        val v1765 = v1761.asInstanceOf[SequenceNode].children.head
        val BindNode(v1766, v1767) = v1765
        assert(v1766.id == 1162)
        val v1768 = v1761.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1769, v1770) = v1768
        assert(v1769.id == 1164)
        BinOp(BinOps.INCLUSIVE_OR, matchInclusiveOrExpression(v1767), matchExclusiveOrExpression(v1770))(nextId(), v1761)
    }
    v1771
  }

  def matchInputCharacter(node: Node): Char = {
    val BindNode(v1772, v1773) = node
    val v1779 = v1772.id match {
      case 832 =>
        val v1774 = v1773.asInstanceOf[SequenceNode].children.head
        val BindNode(v1775, v1776) = v1774
        assert(v1775.id == 833)
        val BindNode(v1777, v1778) = v1776
        assert(v1777.id == 834)
        v1778.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v1779
  }

  def matchInstanceInitializer(node: Node): InstanceInitializer = {
    val BindNode(v1780, v1781) = node
    val v1785 = v1780.id match {
      case 688 =>
        val v1782 = v1781.asInstanceOf[SequenceNode].children.head
        val BindNode(v1783, v1784) = v1782
        assert(v1783.id == 689)
        InstanceInitializer(matchBlock(v1784))(nextId(), v1781)
    }
    v1785
  }

  def matchInstanceofExpression(node: Node): InstanceofExpression = {
    val BindNode(v1786, v1787) = node
    val v1794 = v1786.id match {
      case 1202 =>
        val v1788 = v1787.asInstanceOf[SequenceNode].children.head
        val BindNode(v1789, v1790) = v1788
        assert(v1789.id == 1170)
        val v1791 = v1787.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1792, v1793) = v1791
        assert(v1792.id == 520)
        InstanceofExpression(matchRelationalExpression(v1790), matchReferenceType(v1793))(nextId(), v1787)
    }
    v1794
  }

  def matchIntegerLiteral(node: Node): IntegerLiteral = {
    val BindNode(v1795, v1796) = node
    val v1809 = v1795.id match {
      case 733 =>
        val v1797 = v1796.asInstanceOf[SequenceNode].children.head
        val BindNode(v1798, v1799) = v1797
        assert(v1798.id == 734)
        matchDecimalIntegerLiteral(v1799)
      case 758 =>
        val v1800 = v1796.asInstanceOf[SequenceNode].children.head
        val BindNode(v1801, v1802) = v1800
        assert(v1801.id == 759)
        matchHexIntegerLiteral(v1802)
      case 771 =>
        val v1803 = v1796.asInstanceOf[SequenceNode].children.head
        val BindNode(v1804, v1805) = v1803
        assert(v1804.id == 772)
        matchOctalIntegerLiteral(v1805)
      case 783 =>
        val v1806 = v1796.asInstanceOf[SequenceNode].children.head
        val BindNode(v1807, v1808) = v1806
        assert(v1807.id == 784)
        matchBinaryIntegerLiteral(v1808)
    }
    v1809
  }

  def matchIntegerTypeSuffix(node: Node): IntegerTypeSuffix.Value = {
    val BindNode(v1810, v1811) = node
    val v1812 = v1810.id match {
      case 756 =>
        IntegerTypeSuffix.LONG_TYPE
    }
    v1812
  }

  def matchIntegralType(node: Node): IntegralType = {
    val BindNode(v1813, v1814) = node
    val v1834 = v1813.id match {
      case 530 =>
        val v1815 = v1814.asInstanceOf[SequenceNode].children.head
        val BindNode(v1816, v1817) = v1815
        assert(v1816.id == 531)
        val JoinNode(_, v1818, _) = v1817
        val BindNode(v1819, v1820) = v1818
        assert(v1819.id == 532)
        val BindNode(v1821, v1822) = v1820
        val v1833 = v1821.id match {
          case 347 =>
            val BindNode(v1823, v1824) = v1822
            assert(v1823.id == 348)
            CharType()(nextId(), v1824)
          case 339 =>
            val BindNode(v1825, v1826) = v1822
            assert(v1825.id == 340)
            ShortType()(nextId(), v1826)
          case 286 =>
            val BindNode(v1827, v1828) = v1822
            assert(v1827.id == 287)
            ByteType()(nextId(), v1828)
          case 375 =>
            val BindNode(v1829, v1830) = v1822
            assert(v1829.id == 376)
            LongType()(nextId(), v1830)
          case 335 =>
            val BindNode(v1831, v1832) = v1822
            assert(v1831.id == 336)
            IntType()(nextId(), v1832)
        }
        v1833
    }
    v1834
  }

  def matchInterfaceBody(node: Node): InterfaceBody = {
    val BindNode(v1835, v1836) = node
    val v1849 = v1835.id match {
      case 1364 =>
        val v1837 = v1836.asInstanceOf[SequenceNode].children(1)
        val v1838 = unrollRepeat0(v1837).map { elem =>
          val BindNode(v1839, v1840) = elem
          assert(v1839.id == 1367)
          val BindNode(v1841, v1842) = v1840
          val v1848 = v1841.id match {
            case 1368 =>
              val BindNode(v1843, v1844) = v1842
              assert(v1843.id == 1369)
              val v1845 = v1844.asInstanceOf[SequenceNode].children(1)
              val BindNode(v1846, v1847) = v1845
              assert(v1846.id == 1370)
              matchInterfaceMemberDeclaration(v1847)
          }
          v1848
        }
        InterfaceBody(v1838)(nextId(), v1836)
    }
    v1849
  }

  def matchInterfaceDeclaration(node: Node): InterfaceDeclaration = {
    val BindNode(v1850, v1851) = node
    val v1855 = v1850.id match {
      case 1344 =>
        val v1852 = v1851.asInstanceOf[SequenceNode].children.head
        val BindNode(v1853, v1854) = v1852
        assert(v1853.id == 1345)
        matchNormalInterfaceDeclaration(v1854)
    }
    v1855
  }

  def matchInterfaceExtends(node: Node): InterfaceExtends = {
    val BindNode(v1856, v1857) = node
    val v1861 = v1856.id match {
      case 1362 =>
        val v1858 = v1857.asInstanceOf[SequenceNode].children(2)
        val BindNode(v1859, v1860) = v1858
        assert(v1859.id == 596)
        InterfaceExtends(matchInterfaceTypeList(v1860))(nextId(), v1857)
    }
    v1861
  }

  def matchInterfaceMemberDeclaration(node: Node): InterfaceMemberDeclaration = {
    val BindNode(v1862, v1863) = node
    val v1870 = v1862.id match {
      case 1371 =>
        val v1864 = v1863.asInstanceOf[SequenceNode].children.head
        val BindNode(v1865, v1866) = v1864
        assert(v1865.id == 1372)
        matchConstantDeclaration(v1866)
      case 1383 =>
        val v1867 = v1863.asInstanceOf[SequenceNode].children.head
        val BindNode(v1868, v1869) = v1867
        assert(v1868.id == 1384)
        matchInterfaceMethodDeclaration(v1869)
    }
    v1870
  }

  def matchInterfaceMethodDeclaration(node: Node): InterfaceMethodDeclaration = {
    val BindNode(v1871, v1872) = node
    val v1890 = v1871.id match {
      case 1385 =>
        val v1873 = v1872.asInstanceOf[SequenceNode].children.head
        val v1874 = unrollRepeat0(v1873).map { elem =>
          val BindNode(v1875, v1876) = elem
          assert(v1875.id == 1388)
          val BindNode(v1877, v1878) = v1876
          assert(v1877.id == 1389)
          val BindNode(v1879, v1880) = v1878
          assert(v1879.id == 1390)
          val v1881 = v1880.asInstanceOf[SequenceNode].children.head
          val BindNode(v1882, v1883) = v1881
          assert(v1882.id == 1391)
          matchInterfaceMethodModifier(v1883)
        }
        val v1884 = v1872.asInstanceOf[SequenceNode].children(1)
        val BindNode(v1885, v1886) = v1884
        assert(v1885.id == 1238)
        val v1887 = v1872.asInstanceOf[SequenceNode].children(3)
        val BindNode(v1888, v1889) = v1887
        assert(v1888.id == 1276)
        InterfaceMethodDeclaration(v1874, matchMethodHeader(v1886), matchMethodBody(v1889))(nextId(), v1872)
    }
    v1890
  }

  def matchInterfaceMethodModifier(node: Node): InterfaceMethodModifier.Value = {
    val BindNode(v1891, v1892) = node
    val v1912 = v1891.id match {
      case 1392 =>
        val v1893 = v1892.asInstanceOf[SequenceNode].children.head
        val BindNode(v1894, v1895) = v1893
        assert(v1894.id == 1393)
        val JoinNode(_, v1896, _) = v1895
        val BindNode(v1897, v1898) = v1896
        assert(v1897.id == 1394)
        val BindNode(v1899, v1900) = v1898
        val v1911 = v1899.id match {
          case 298 =>
            val BindNode(v1901, v1902) = v1900
            assert(v1901.id == 299)
            InterfaceMethodModifier.PUBLIC
          case 256 =>
            val BindNode(v1903, v1904) = v1900
            assert(v1903.id == 257)
            InterfaceMethodModifier.PRIVATE
          case 359 =>
            val BindNode(v1905, v1906) = v1900
            assert(v1905.id == 360)
            InterfaceMethodModifier.STATIC
          case 226 =>
            val BindNode(v1907, v1908) = v1900
            assert(v1907.id == 227)
            InterfaceMethodModifier.DEFAULT
          case 191 =>
            val BindNode(v1909, v1910) = v1900
            assert(v1909.id == 192)
            InterfaceMethodModifier.ABSTRACT
        }
        v1911
    }
    v1912
  }

  def matchInterfaceModifier(node: Node): InterfaceModifier.Value = {
    val BindNode(v1913, v1914) = node
    val v1934 = v1913.id match {
      case 1353 =>
        val v1915 = v1914.asInstanceOf[SequenceNode].children.head
        val BindNode(v1916, v1917) = v1915
        assert(v1916.id == 1354)
        val JoinNode(_, v1918, _) = v1917
        val BindNode(v1919, v1920) = v1918
        assert(v1919.id == 1355)
        val BindNode(v1921, v1922) = v1920
        val v1933 = v1921.id match {
          case 256 =>
            val BindNode(v1923, v1924) = v1922
            assert(v1923.id == 257)
            InterfaceModifier.PRIVATE
          case 359 =>
            val BindNode(v1925, v1926) = v1922
            assert(v1925.id == 360)
            InterfaceModifier.STATIC
          case 191 =>
            val BindNode(v1927, v1928) = v1922
            assert(v1927.id == 192)
            InterfaceModifier.ABSTRACT
          case 298 =>
            val BindNode(v1929, v1930) = v1922
            assert(v1929.id == 299)
            InterfaceModifier.PUBLIC
          case 278 =>
            val BindNode(v1931, v1932) = v1922
            assert(v1931.id == 279)
            InterfaceModifier.PROTECTED
        }
        v1933
    }
    v1934
  }

  def matchInterfaceType(node: Node): ClassOrInterfaceType = {
    val BindNode(v1935, v1936) = node
    val v1940 = v1935.id match {
      case 521 =>
        val v1937 = v1936.asInstanceOf[SequenceNode].children.head
        val BindNode(v1938, v1939) = v1937
        assert(v1938.id == 508)
        matchClassOrInterfaceType(v1939)
    }
    v1940
  }

  def matchInterfaceTypeList(node: Node): List[ClassOrInterfaceType] = {
    val BindNode(v1941, v1942) = node
    val v1958 = v1941.id match {
      case 597 =>
        val v1943 = v1942.asInstanceOf[SequenceNode].children.head
        val BindNode(v1944, v1945) = v1943
        assert(v1944.id == 576)
        val v1946 = v1942.asInstanceOf[SequenceNode].children(1)
        val v1947 = unrollRepeat0(v1946).map { elem =>
          val BindNode(v1948, v1949) = elem
          assert(v1948.id == 600)
          val BindNode(v1950, v1951) = v1949
          val v1957 = v1950.id match {
            case 601 =>
              val BindNode(v1952, v1953) = v1951
              assert(v1952.id == 602)
              val v1954 = v1953.asInstanceOf[SequenceNode].children(3)
              val BindNode(v1955, v1956) = v1954
              assert(v1955.id == 576)
              matchInterfaceType(v1956)
          }
          v1957
        }
        List(matchInterfaceType(v1945)) ++ v1947
    }
    v1958
  }

  def matchJavaLetter(node: Node): Char = {
    val BindNode(v1959, v1960) = node
    val v1964 = v1959.id match {
      case 178 =>
        val v1961 = v1960.asInstanceOf[SequenceNode].children.head
        val BindNode(v1962, v1963) = v1961
        assert(v1962.id == 179)
        v1963.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v1964
  }

  def matchJavaLetterOrDigit(node: Node): Char = {
    val BindNode(v1965, v1966) = node
    val v1970 = v1965.id match {
      case 183 =>
        val v1967 = v1966.asInstanceOf[SequenceNode].children.head
        val BindNode(v1968, v1969) = v1967
        assert(v1968.id == 34)
        v1969.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v1970
  }

  def matchLabeledStatement(node: Node): LabeledStatement = {
    val BindNode(v1971, v1972) = node
    val v1979 = v1971.id match {
      case 1095 =>
        val v1973 = v1972.asInstanceOf[SequenceNode].children.head
        val BindNode(v1974, v1975) = v1973
        assert(v1974.id == 168)
        val v1976 = v1972.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1977, v1978) = v1976
        assert(v1977.id == 705)
        LabeledStatement(matchIdentifier(v1975), matchStatement(v1978))(nextId(), v1972)
    }
    v1979
  }

  def matchLabeledStatementNoShortIf(node: Node): LabeledStatement = {
    val BindNode(v1980, v1981) = node
    val v1988 = v1980.id match {
      case 1106 =>
        val v1982 = v1981.asInstanceOf[SequenceNode].children.head
        val BindNode(v1983, v1984) = v1982
        assert(v1983.id == 168)
        val v1985 = v1981.asInstanceOf[SequenceNode].children(4)
        val BindNode(v1986, v1987) = v1985
        assert(v1986.id == 1103)
        LabeledStatement(matchIdentifier(v1984), matchStatementNoShortIf(v1987))(nextId(), v1981)
    }
    v1988
  }

  def matchLambdaBody(node: Node): LambdaBody = {
    val BindNode(v1989, v1990) = node
    val v1997 = v1989.id match {
      case 643 =>
        val v1991 = v1990.asInstanceOf[SequenceNode].children.head
        val BindNode(v1992, v1993) = v1991
        assert(v1992.id == 644)
        matchExpression(v1993)
      case 688 =>
        val v1994 = v1990.asInstanceOf[SequenceNode].children.head
        val BindNode(v1995, v1996) = v1994
        assert(v1995.id == 689)
        matchBlock(v1996)
    }
    v1997
  }

  def matchLambdaExpression(node: Node): LambdaExpression = {
    val BindNode(v1998, v1999) = node
    val v2006 = v1998.id match {
      case 647 =>
        val v2000 = v1999.asInstanceOf[SequenceNode].children.head
        val BindNode(v2001, v2002) = v2000
        assert(v2001.id == 648)
        val v2003 = v1999.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2004, v2005) = v2003
        assert(v2004.id == 687)
        LambdaExpression(matchLambdaParameters(v2002), matchLambdaBody(v2005))(nextId(), v1999)
    }
    v2006
  }

  def matchLambdaParameter(node: Node): LambdaParameter = {
    val BindNode(v2007, v2008) = node
    val v2029 = v2007.id match {
      case 659 =>
        val v2009 = v2008.asInstanceOf[SequenceNode].children.head
        val v2010 = unrollRepeat0(v2009).map { elem =>
          val BindNode(v2011, v2012) = elem
          assert(v2011.id == 662)
          val BindNode(v2013, v2014) = v2012
          assert(v2013.id == 663)
          val BindNode(v2015, v2016) = v2014
          assert(v2015.id == 664)
          val v2017 = v2016.asInstanceOf[SequenceNode].children.head
          val BindNode(v2018, v2019) = v2017
          assert(v2018.id == 665)
          matchVariableModifier(v2019)
        }
        val v2020 = v2008.asInstanceOf[SequenceNode].children(1)
        val BindNode(v2021, v2022) = v2020
        assert(v2021.id == 668)
        val v2023 = v2008.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2024, v2025) = v2023
        assert(v2024.id == 632)
        SoloLambdaParameter(v2010, matchLambdaParameterType(v2022), matchVariableDeclaratorId(v2025))(nextId(), v2008)
      case 670 =>
        val v2026 = v2008.asInstanceOf[SequenceNode].children.head
        val BindNode(v2027, v2028) = v2026
        assert(v2027.id == 671)
        matchVariableArityParameter(v2028)
    }
    v2029
  }

  def matchLambdaParameterList(node: Node): LambdaParameterList = {
    val BindNode(v2030, v2031) = node
    val v2062 = v2030.id match {
      case 657 =>
        val v2032 = v2031.asInstanceOf[SequenceNode].children.head
        val BindNode(v2033, v2034) = v2032
        assert(v2033.id == 658)
        val v2035 = v2031.asInstanceOf[SequenceNode].children(1)
        val v2036 = unrollRepeat0(v2035).map { elem =>
          val BindNode(v2037, v2038) = elem
          assert(v2037.id == 676)
          val BindNode(v2039, v2040) = v2038
          val v2046 = v2039.id match {
            case 677 =>
              val BindNode(v2041, v2042) = v2040
              assert(v2041.id == 678)
              val v2043 = v2042.asInstanceOf[SequenceNode].children(3)
              val BindNode(v2044, v2045) = v2043
              assert(v2044.id == 658)
              matchLambdaParameter(v2045)
          }
          v2046
        }
        CanonicalLambdaParameters(List(matchLambdaParameter(v2034)) ++ v2036)(nextId(), v2031)
      case 679 =>
        val v2047 = v2031.asInstanceOf[SequenceNode].children.head
        val BindNode(v2048, v2049) = v2047
        assert(v2048.id == 168)
        val v2050 = v2031.asInstanceOf[SequenceNode].children(1)
        val v2051 = unrollRepeat0(v2050).map { elem =>
          val BindNode(v2052, v2053) = elem
          assert(v2052.id == 682)
          val BindNode(v2054, v2055) = v2053
          val v2061 = v2054.id match {
            case 683 =>
              val BindNode(v2056, v2057) = v2055
              assert(v2056.id == 684)
              val v2058 = v2057.asInstanceOf[SequenceNode].children(3)
              val BindNode(v2059, v2060) = v2058
              assert(v2059.id == 168)
              matchIdentifier(v2060)
          }
          v2061
        }
        NameOnlyLambdaParameters(List(matchIdentifier(v2049)) ++ v2051)(nextId(), v2031)
    }
    v2062
  }

  def matchLambdaParameterType(node: Node): Type = {
    val BindNode(v2063, v2064) = node
    val v2068 = v2063.id match {
      case 669 =>
        val v2065 = v2064.asInstanceOf[SequenceNode].children.head
        val BindNode(v2066, v2067) = v2065
        assert(v2066.id == 626)
        matchType(v2067)
    }
    v2068
  }

  def matchLambdaParameters(node: Node): LambdaParameters = {
    val BindNode(v2069, v2070) = node
    val v2088 = v2069.id match {
      case 649 =>
        val v2071 = v2070.asInstanceOf[SequenceNode].children.head
        val BindNode(v2072, v2073) = v2071
        assert(v2072.id == 168)
        SingleLambdaParameter(matchIdentifier(v2073))(nextId(), v2070)
      case 650 =>
        val v2074 = v2070.asInstanceOf[SequenceNode].children(1)
        val BindNode(v2075, v2076) = v2074
        assert(v2075.id == 652)
        val BindNode(v2077, v2078) = v2076
        val v2087 = v2077.id match {
          case 430 =>
            None
          case 653 =>
            val BindNode(v2079, v2080) = v2078
            val v2086 = v2079.id match {
              case 654 =>
                val BindNode(v2081, v2082) = v2080
                assert(v2081.id == 655)
                val v2083 = v2082.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2084, v2085) = v2083
                assert(v2084.id == 656)
                matchLambdaParameterList(v2085)
            }
            Some(v2086)
        }
        NonSingleLambdaParameters(v2087)(nextId(), v2070)
    }
    v2088
  }

  def matchLeftHandSide(node: Node): LeftHandSide = {
    val BindNode(v2089, v2090) = node
    val v2100 = v2089.id match {
      case 719 =>
        val v2091 = v2090.asInstanceOf[SequenceNode].children.head
        val BindNode(v2092, v2093) = v2091
        assert(v2092.id == 720)
        matchExpressionName(v2093)
      case 723 =>
        val v2094 = v2090.asInstanceOf[SequenceNode].children.head
        val BindNode(v2095, v2096) = v2094
        assert(v2095.id == 724)
        matchFieldAccess(v2096)
      case 916 =>
        val v2097 = v2090.asInstanceOf[SequenceNode].children.head
        val BindNode(v2098, v2099) = v2097
        assert(v2098.id == 917)
        matchArrayAccess(v2099)
    }
    v2100
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v2101, v2102) = node
    val v2121 = v2101.id match {
      case 852 =>
        val v2103 = v2102.asInstanceOf[SequenceNode].children.head
        val BindNode(v2104, v2105) = v2103
        assert(v2104.id == 853)
        matchStringLiteral(v2105)
      case 865 =>
        val v2106 = v2102.asInstanceOf[SequenceNode].children.head
        val BindNode(v2107, v2108) = v2106
        assert(v2107.id == 866)
        matchNullLiteral(v2108)
      case 819 =>
        val v2109 = v2102.asInstanceOf[SequenceNode].children.head
        val BindNode(v2110, v2111) = v2109
        assert(v2110.id == 820)
        matchBooleanLiteral(v2111)
      case 824 =>
        val v2112 = v2102.asInstanceOf[SequenceNode].children.head
        val BindNode(v2113, v2114) = v2112
        assert(v2113.id == 825)
        matchCharacterLiteral(v2114)
      case 796 =>
        val v2115 = v2102.asInstanceOf[SequenceNode].children.head
        val BindNode(v2116, v2117) = v2115
        assert(v2116.id == 797)
        matchFloatingPointLiteral(v2117)
      case 731 =>
        val v2118 = v2102.asInstanceOf[SequenceNode].children.head
        val BindNode(v2119, v2120) = v2118
        assert(v2119.id == 732)
        matchIntegerLiteral(v2120)
    }
    v2121
  }

  def matchLocalVariableDeclaration(node: Node): LocalVariableDeclaration = {
    val BindNode(v2122, v2123) = node
    val v2141 = v2122.id match {
      case 702 =>
        val v2124 = v2123.asInstanceOf[SequenceNode].children.head
        val v2125 = unrollRepeat0(v2124).map { elem =>
          val BindNode(v2126, v2127) = elem
          assert(v2126.id == 662)
          val BindNode(v2128, v2129) = v2127
          assert(v2128.id == 663)
          val BindNode(v2130, v2131) = v2129
          assert(v2130.id == 664)
          val v2132 = v2131.asInstanceOf[SequenceNode].children.head
          val BindNode(v2133, v2134) = v2132
          assert(v2133.id == 665)
          matchVariableModifier(v2134)
        }
        val v2135 = v2123.asInstanceOf[SequenceNode].children(1)
        val BindNode(v2136, v2137) = v2135
        assert(v2136.id == 703)
        val v2138 = v2123.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2139, v2140) = v2138
        assert(v2139.id == 628)
        LocalVariableDeclaration(v2125, matchLocalVariableType(v2137), matchVariableDeclaratorList(v2140))(nextId(), v2123)
    }
    v2141
  }

  def matchLocalVariableDeclarationStatement(node: Node): LocalVariableDeclarationStatement = {
    val BindNode(v2142, v2143) = node
    val v2147 = v2142.id match {
      case 700 =>
        val v2144 = v2143.asInstanceOf[SequenceNode].children.head
        val BindNode(v2145, v2146) = v2144
        assert(v2145.id == 701)
        LocalVariableDeclarationStatement(matchLocalVariableDeclaration(v2146))(nextId(), v2143)
    }
    v2147
  }

  def matchLocalVariableType(node: Node): Type = {
    val BindNode(v2148, v2149) = node
    val v2153 = v2148.id match {
      case 669 =>
        val v2150 = v2149.asInstanceOf[SequenceNode].children.head
        val BindNode(v2151, v2152) = v2150
        assert(v2151.id == 626)
        matchType(v2152)
    }
    v2153
  }

  def matchMethodArgumentList(node: Node): List[Expression] = {
    val BindNode(v2154, v2155) = node
    val v2171 = v2154.id match {
      case 951 =>
        val v2157 = v2155.asInstanceOf[SequenceNode].children(1)
        val BindNode(v2158, v2159) = v2157
        assert(v2158.id == 896)
        val BindNode(v2160, v2161) = v2159
        val v2170 = v2160.id match {
          case 430 =>
            None
          case 897 =>
            val BindNode(v2162, v2163) = v2161
            val v2169 = v2162.id match {
              case 898 =>
                val BindNode(v2164, v2165) = v2163
                assert(v2164.id == 899)
                val v2166 = v2165.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2167, v2168) = v2166
                assert(v2167.id == 900)
                matchArgumentList(v2168)
            }
            Some(v2169)
        }
        val v2156 = v2170
        if (v2156.isDefined) v2156.get else List()
    }
    v2171
  }

  def matchMethodBody(node: Node): MethodBody = {
    val BindNode(v2172, v2173) = node
    val v2177 = v2172.id match {
      case 688 =>
        val v2174 = v2173.asInstanceOf[SequenceNode].children.head
        val BindNode(v2175, v2176) = v2174
        assert(v2175.id == 689)
        matchBlock(v2176)
      case 710 =>
        EmptyMethodBody()(nextId(), v2173)
    }
    v2177
  }

  def matchMethodDeclaration(node: Node): MethodDeclaration = {
    val BindNode(v2178, v2179) = node
    val v2197 = v2178.id match {
      case 1228 =>
        val v2180 = v2179.asInstanceOf[SequenceNode].children.head
        val v2181 = unrollRepeat0(v2180).map { elem =>
          val BindNode(v2182, v2183) = elem
          assert(v2182.id == 1231)
          val BindNode(v2184, v2185) = v2183
          assert(v2184.id == 1232)
          val BindNode(v2186, v2187) = v2185
          assert(v2186.id == 1233)
          val v2188 = v2187.asInstanceOf[SequenceNode].children.head
          val BindNode(v2189, v2190) = v2188
          assert(v2189.id == 1234)
          matchMethodModifier(v2190)
        }
        val v2191 = v2179.asInstanceOf[SequenceNode].children(1)
        val BindNode(v2192, v2193) = v2191
        assert(v2192.id == 1238)
        val v2194 = v2179.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2195, v2196) = v2194
        assert(v2195.id == 1276)
        MethodDeclaration(v2181, matchMethodHeader(v2193), matchMethodBody(v2196))(nextId(), v2179)
    }
    v2197
  }

  def matchMethodDeclarator(node: Node): MethodDeclarator = {
    val BindNode(v2198, v2199) = node
    val v2218 = v2198.id match {
      case 1247 =>
        val v2200 = v2199.asInstanceOf[SequenceNode].children.head
        val BindNode(v2201, v2202) = v2200
        assert(v2201.id == 168)
        val v2204 = v2199.asInstanceOf[SequenceNode].children(2)
        val BindNode(v2205, v2206) = v2204
        assert(v2205.id == 1248)
        val BindNode(v2207, v2208) = v2206
        val v2217 = v2207.id match {
          case 430 =>
            None
          case 1249 =>
            val BindNode(v2209, v2210) = v2208
            val v2216 = v2209.id match {
              case 1250 =>
                val BindNode(v2211, v2212) = v2210
                assert(v2211.id == 1251)
                val v2213 = v2212.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2214, v2215) = v2213
                assert(v2214.id == 1252)
                matchFormalParameterList(v2215)
            }
            Some(v2216)
        }
        val v2203 = v2217
        MethodDeclarator(matchIdentifier(v2202), if (v2203.isDefined) v2203.get else List())(nextId(), v2199)
    }
    v2218
  }

  def matchMethodHeader(node: Node): MethodHeader = {
    val BindNode(v2219, v2220) = node
    val v2254 = v2219.id match {
      case 1239 =>
        val v2221 = v2220.asInstanceOf[SequenceNode].children.head
        val BindNode(v2222, v2223) = v2221
        assert(v2222.id == 1240)
        val BindNode(v2224, v2225) = v2223
        val v2233 = v2224.id match {
          case 430 =>
            None
          case 1241 =>
            val BindNode(v2226, v2227) = v2225
            assert(v2226.id == 1242)
            val BindNode(v2228, v2229) = v2227
            assert(v2228.id == 1243)
            val v2230 = v2229.asInstanceOf[SequenceNode].children.head
            val BindNode(v2231, v2232) = v2230
            assert(v2231.id == 495)
            Some(matchTypeParameters(v2232))
        }
        val v2234 = v2220.asInstanceOf[SequenceNode].children(1)
        val BindNode(v2235, v2236) = v2234
        assert(v2235.id == 1244)
        val v2237 = v2220.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2238, v2239) = v2237
        assert(v2238.id == 1246)
        val v2240 = v2220.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2241, v2242) = v2240
        assert(v2241.id == 1261)
        val BindNode(v2243, v2244) = v2242
        val v2253 = v2243.id match {
          case 430 =>
            None
          case 1262 =>
            val BindNode(v2245, v2246) = v2244
            val v2252 = v2245.id match {
              case 1263 =>
                val BindNode(v2247, v2248) = v2246
                assert(v2247.id == 1264)
                val v2249 = v2248.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2250, v2251) = v2249
                assert(v2250.id == 1265)
                matchThrows(v2251)
            }
            Some(v2252)
        }
        MethodHeader(v2233, matchResult(v2236), matchMethodDeclarator(v2239), v2253)(nextId(), v2220)
    }
    v2254
  }

  def matchMethodInvocation(node: Node): MethodInvocation = {
    val BindNode(v2255, v2256) = node
    val v2352 = v2255.id match {
      case 956 =>
        val v2257 = v2256.asInstanceOf[SequenceNode].children.head
        val BindNode(v2258, v2259) = v2257
        assert(v2258.id == 441)
        val v2260 = v2256.asInstanceOf[SequenceNode].children(7)
        val BindNode(v2261, v2262) = v2260
        assert(v2261.id == 510)
        val BindNode(v2263, v2264) = v2262
        val v2273 = v2263.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v2265, v2266) = v2264
            val v2272 = v2265.id match {
              case 512 =>
                val BindNode(v2267, v2268) = v2266
                assert(v2267.id == 513)
                val v2269 = v2268.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2270, v2271) = v2269
                assert(v2270.id == 514)
                matchTypeArguments(v2271)
            }
            Some(v2272)
        }
        val v2274 = v2256.asInstanceOf[SequenceNode].children(9)
        val BindNode(v2275, v2276) = v2274
        assert(v2275.id == 168)
        val v2277 = v2256.asInstanceOf[SequenceNode].children(11)
        val BindNode(v2278, v2279) = v2277
        assert(v2278.id == 950)
        SuperMethodInvocation(Some(matchTypeName(v2259)), v2273, matchIdentifier(v2276), matchMethodArgumentList(v2279))(nextId(), v2256)
      case 944 =>
        val v2280 = v2256.asInstanceOf[SequenceNode].children.head
        val BindNode(v2281, v2282) = v2280
        assert(v2281.id == 945)
        val v2283 = v2256.asInstanceOf[SequenceNode].children(2)
        val BindNode(v2284, v2285) = v2283
        assert(v2284.id == 950)
        PlainMethodInvocation(matchMethodName(v2282), matchMethodArgumentList(v2285))(nextId(), v2256)
      case 954 =>
        val v2286 = v2256.asInstanceOf[SequenceNode].children.head
        val BindNode(v2287, v2288) = v2286
        assert(v2287.id == 726)
        val v2289 = v2256.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2290, v2291) = v2289
        assert(v2290.id == 510)
        val BindNode(v2292, v2293) = v2291
        val v2302 = v2292.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v2294, v2295) = v2293
            val v2301 = v2294.id match {
              case 512 =>
                val BindNode(v2296, v2297) = v2295
                assert(v2296.id == 513)
                val v2298 = v2297.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2299, v2300) = v2298
                assert(v2299.id == 514)
                matchTypeArguments(v2300)
            }
            Some(v2301)
        }
        val v2303 = v2256.asInstanceOf[SequenceNode].children(5)
        val BindNode(v2304, v2305) = v2303
        assert(v2304.id == 168)
        val v2306 = v2256.asInstanceOf[SequenceNode].children(7)
        val BindNode(v2307, v2308) = v2306
        assert(v2307.id == 950)
        PrimaryMethodInvocation(matchPrimary(v2288), v2302, matchIdentifier(v2305), matchMethodArgumentList(v2308))(nextId(), v2256)
      case 955 =>
        val v2309 = v2256.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2310, v2311) = v2309
        assert(v2310.id == 510)
        val BindNode(v2312, v2313) = v2311
        val v2322 = v2312.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v2314, v2315) = v2313
            val v2321 = v2314.id match {
              case 512 =>
                val BindNode(v2316, v2317) = v2315
                assert(v2316.id == 513)
                val v2318 = v2317.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2319, v2320) = v2318
                assert(v2319.id == 514)
                matchTypeArguments(v2320)
            }
            Some(v2321)
        }
        val v2323 = v2256.asInstanceOf[SequenceNode].children(5)
        val BindNode(v2324, v2325) = v2323
        assert(v2324.id == 168)
        val v2326 = v2256.asInstanceOf[SequenceNode].children(7)
        val BindNode(v2327, v2328) = v2326
        assert(v2327.id == 950)
        SuperMethodInvocation(None, v2322, matchIdentifier(v2325), matchMethodArgumentList(v2328))(nextId(), v2256)
      case 952 =>
        val v2329 = v2256.asInstanceOf[SequenceNode].children.head
        val BindNode(v2330, v2331) = v2329
        assert(v2330.id == 953)
        val v2332 = v2256.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2333, v2334) = v2332
        assert(v2333.id == 510)
        val BindNode(v2335, v2336) = v2334
        val v2345 = v2335.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v2337, v2338) = v2336
            val v2344 = v2337.id match {
              case 512 =>
                val BindNode(v2339, v2340) = v2338
                assert(v2339.id == 513)
                val v2341 = v2340.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2342, v2343) = v2341
                assert(v2342.id == 514)
                matchTypeArguments(v2343)
            }
            Some(v2344)
        }
        val v2346 = v2256.asInstanceOf[SequenceNode].children(5)
        val BindNode(v2347, v2348) = v2346
        assert(v2347.id == 168)
        val v2349 = v2256.asInstanceOf[SequenceNode].children(7)
        val BindNode(v2350, v2351) = v2349
        assert(v2350.id == 950)
        NameMethodInvocation(matchTypeOrExpressionName(v2331), v2345, matchIdentifier(v2348), matchMethodArgumentList(v2351))(nextId(), v2256)
    }
    v2352
  }

  def matchMethodModifier(node: Node): MethodModifier.Value = {
    val BindNode(v2353, v2354) = node
    val v2378 = v2353.id match {
      case 1235 =>
        val v2355 = v2354.asInstanceOf[SequenceNode].children.head
        val BindNode(v2356, v2357) = v2355
        assert(v2356.id == 1236)
        val JoinNode(_, v2358, _) = v2357
        val BindNode(v2359, v2360) = v2358
        assert(v2359.id == 1237)
        val BindNode(v2361, v2362) = v2360
        val v2377 = v2361.id match {
          case 395 =>
            val BindNode(v2363, v2364) = v2362
            assert(v2363.id == 396)
            MethodModifier.NATIVE
          case 351 =>
            val BindNode(v2365, v2366) = v2362
            assert(v2365.id == 352)
            MethodModifier.FINAL
          case 256 =>
            val BindNode(v2367, v2368) = v2362
            assert(v2367.id == 257)
            MethodModifier.PRIVATE
          case 359 =>
            val BindNode(v2369, v2370) = v2362
            assert(v2369.id == 360)
            MethodModifier.STATIC
          case 191 =>
            val BindNode(v2371, v2372) = v2362
            assert(v2371.id == 192)
            MethodModifier.ABSTRACT
          case 298 =>
            val BindNode(v2373, v2374) = v2362
            assert(v2373.id == 299)
            MethodModifier.PUBLIC
          case 278 =>
            val BindNode(v2375, v2376) = v2362
            assert(v2375.id == 279)
            MethodModifier.PROTECTED
        }
        v2377
    }
    v2378
  }

  def matchMethodName(node: Node): MethodName = {
    val BindNode(v2379, v2380) = node
    val v2384 = v2379.id match {
      case 946 =>
        val v2381 = v2380.asInstanceOf[SequenceNode].children.head
        val BindNode(v2382, v2383) = v2381
        assert(v2382.id == 947)
        MethodName(matchUnqualifiedMethodIdentifier(v2383))(nextId(), v2380)
    }
    v2384
  }

  def matchMethodReference(node: Node): MethodReference = {
    val BindNode(v2385, v2386) = node
    val v2504 = v2385.id match {
      case 961 =>
        val v2387 = v2386.asInstanceOf[SequenceNode].children.head
        val BindNode(v2388, v2389) = v2387
        assert(v2388.id == 520)
        val v2390 = v2386.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2391, v2392) = v2390
        assert(v2391.id == 510)
        val BindNode(v2393, v2394) = v2392
        val v2403 = v2393.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v2395, v2396) = v2394
            val v2402 = v2395.id match {
              case 512 =>
                val BindNode(v2397, v2398) = v2396
                assert(v2397.id == 513)
                val v2399 = v2398.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2400, v2401) = v2399
                assert(v2400.id == 514)
                matchTypeArguments(v2401)
            }
            Some(v2402)
        }
        val v2404 = v2386.asInstanceOf[SequenceNode].children(5)
        val BindNode(v2405, v2406) = v2404
        assert(v2405.id == 168)
        PlainRefMethodReference(matchReferenceType(v2389), v2403, matchIdentifier(v2406))(nextId(), v2386)
      case 959 =>
        val v2407 = v2386.asInstanceOf[SequenceNode].children.head
        val BindNode(v2408, v2409) = v2407
        assert(v2408.id == 720)
        val v2410 = v2386.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2411, v2412) = v2410
        assert(v2411.id == 510)
        val BindNode(v2413, v2414) = v2412
        val v2423 = v2413.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v2415, v2416) = v2414
            val v2422 = v2415.id match {
              case 512 =>
                val BindNode(v2417, v2418) = v2416
                assert(v2417.id == 513)
                val v2419 = v2418.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2420, v2421) = v2419
                assert(v2420.id == 514)
                matchTypeArguments(v2421)
            }
            Some(v2422)
        }
        val v2424 = v2386.asInstanceOf[SequenceNode].children(5)
        val BindNode(v2425, v2426) = v2424
        assert(v2425.id == 168)
        PlainNameMethodReference(matchExpressionName(v2409), v2423, matchIdentifier(v2426))(nextId(), v2386)
      case 965 =>
        val v2427 = v2386.asInstanceOf[SequenceNode].children.head
        val BindNode(v2428, v2429) = v2427
        assert(v2428.id == 588)
        val v2430 = v2386.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2431, v2432) = v2430
        assert(v2431.id == 510)
        val BindNode(v2433, v2434) = v2432
        val v2443 = v2433.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v2435, v2436) = v2434
            val v2442 = v2435.id match {
              case 512 =>
                val BindNode(v2437, v2438) = v2436
                assert(v2437.id == 513)
                val v2439 = v2438.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2440, v2441) = v2439
                assert(v2440.id == 514)
                matchTypeArguments(v2441)
            }
            Some(v2442)
        }
        NewMethodReference(matchClassType(v2429), v2443)(nextId(), v2386)
      case 966 =>
        val v2444 = v2386.asInstanceOf[SequenceNode].children.head
        val BindNode(v2445, v2446) = v2444
        assert(v2445.id == 523)
        NewArrayMethodReference(matchArrayType(v2446))(nextId(), v2386)
      case 963 =>
        val v2447 = v2386.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2448, v2449) = v2447
        assert(v2448.id == 510)
        val BindNode(v2450, v2451) = v2449
        val v2460 = v2450.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v2452, v2453) = v2451
            val v2459 = v2452.id match {
              case 512 =>
                val BindNode(v2454, v2455) = v2453
                assert(v2454.id == 513)
                val v2456 = v2455.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2457, v2458) = v2456
                assert(v2457.id == 514)
                matchTypeArguments(v2458)
            }
            Some(v2459)
        }
        val v2461 = v2386.asInstanceOf[SequenceNode].children(5)
        val BindNode(v2462, v2463) = v2461
        assert(v2462.id == 168)
        SuperMethodReference(None, v2460, matchIdentifier(v2463))(nextId(), v2386)
      case 962 =>
        val v2464 = v2386.asInstanceOf[SequenceNode].children.head
        val BindNode(v2465, v2466) = v2464
        assert(v2465.id == 726)
        val v2467 = v2386.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2468, v2469) = v2467
        assert(v2468.id == 510)
        val BindNode(v2470, v2471) = v2469
        val v2480 = v2470.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v2472, v2473) = v2471
            val v2479 = v2472.id match {
              case 512 =>
                val BindNode(v2474, v2475) = v2473
                assert(v2474.id == 513)
                val v2476 = v2475.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2477, v2478) = v2476
                assert(v2477.id == 514)
                matchTypeArguments(v2478)
            }
            Some(v2479)
        }
        val v2481 = v2386.asInstanceOf[SequenceNode].children(5)
        val BindNode(v2482, v2483) = v2481
        assert(v2482.id == 168)
        PlainPrimaryMethodReference(matchPrimary(v2466), v2480, matchIdentifier(v2483))(nextId(), v2386)
      case 964 =>
        val v2484 = v2386.asInstanceOf[SequenceNode].children.head
        val BindNode(v2485, v2486) = v2484
        assert(v2485.id == 441)
        val v2487 = v2386.asInstanceOf[SequenceNode].children(7)
        val BindNode(v2488, v2489) = v2487
        assert(v2488.id == 510)
        val BindNode(v2490, v2491) = v2489
        val v2500 = v2490.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v2492, v2493) = v2491
            val v2499 = v2492.id match {
              case 512 =>
                val BindNode(v2494, v2495) = v2493
                assert(v2494.id == 513)
                val v2496 = v2495.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2497, v2498) = v2496
                assert(v2497.id == 514)
                matchTypeArguments(v2498)
            }
            Some(v2499)
        }
        val v2501 = v2386.asInstanceOf[SequenceNode].children(9)
        val BindNode(v2502, v2503) = v2501
        assert(v2502.id == 168)
        SuperMethodReference(Some(matchTypeName(v2486)), v2500, matchIdentifier(v2503))(nextId(), v2386)
    }
    v2504
  }

  def matchMultiplicativeExpression(node: Node): MultiplicativeExpression = {
    val BindNode(v2505, v2506) = node
    val v2528 = v2505.id match {
      case 1177 =>
        val v2507 = v2506.asInstanceOf[SequenceNode].children.head
        val BindNode(v2508, v2509) = v2507
        assert(v2508.id == 1005)
        matchUnaryExpression(v2509)
      case 1178 =>
        val v2510 = v2506.asInstanceOf[SequenceNode].children.head
        val BindNode(v2511, v2512) = v2510
        assert(v2511.id == 1176)
        val v2513 = v2506.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2514, v2515) = v2513
        assert(v2514.id == 1005)
        BinOp(BinOps.MUL, matchMultiplicativeExpression(v2512), matchUnaryExpression(v2515))(nextId(), v2506)
      case 1180 =>
        val v2516 = v2506.asInstanceOf[SequenceNode].children.head
        val BindNode(v2517, v2518) = v2516
        assert(v2517.id == 1176)
        val v2519 = v2506.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2520, v2521) = v2519
        assert(v2520.id == 1005)
        BinOp(BinOps.DIV, matchMultiplicativeExpression(v2518), matchUnaryExpression(v2521))(nextId(), v2506)
      case 1182 =>
        val v2522 = v2506.asInstanceOf[SequenceNode].children.head
        val BindNode(v2523, v2524) = v2522
        assert(v2523.id == 1176)
        val v2525 = v2506.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2526, v2527) = v2525
        assert(v2526.id == 1005)
        BinOp(BinOps.REM, matchMultiplicativeExpression(v2524), matchUnaryExpression(v2527))(nextId(), v2506)
    }
    v2528
  }

  def matchNormalClassDeclaration(node: Node): NormalClassDeclaration = {
    val BindNode(v2529, v2530) = node
    val v2590 = v2529.id match {
      case 480 =>
        val v2531 = v2530.asInstanceOf[SequenceNode].children.head
        val v2532 = unrollRepeat0(v2531).map { elem =>
          val BindNode(v2533, v2534) = elem
          assert(v2533.id == 483)
          val BindNode(v2535, v2536) = v2534
          assert(v2535.id == 484)
          val BindNode(v2537, v2538) = v2536
          assert(v2537.id == 485)
          val v2539 = v2538.asInstanceOf[SequenceNode].children.head
          val BindNode(v2540, v2541) = v2539
          assert(v2540.id == 486)
          matchClassModifier(v2541)
        }
        val v2542 = v2530.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2543, v2544) = v2542
        assert(v2543.id == 443)
        val v2545 = v2530.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2546, v2547) = v2545
        assert(v2546.id == 491)
        val BindNode(v2548, v2549) = v2547
        val v2558 = v2548.id match {
          case 430 =>
            None
          case 492 =>
            val BindNode(v2550, v2551) = v2549
            val v2557 = v2550.id match {
              case 493 =>
                val BindNode(v2552, v2553) = v2551
                assert(v2552.id == 494)
                val v2554 = v2553.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2555, v2556) = v2554
                assert(v2555.id == 495)
                matchTypeParameters(v2556)
            }
            Some(v2557)
        }
        val v2559 = v2530.asInstanceOf[SequenceNode].children(5)
        val BindNode(v2560, v2561) = v2559
        assert(v2560.id == 582)
        val BindNode(v2562, v2563) = v2561
        val v2572 = v2562.id match {
          case 430 =>
            None
          case 583 =>
            val BindNode(v2564, v2565) = v2563
            val v2571 = v2564.id match {
              case 584 =>
                val BindNode(v2566, v2567) = v2565
                assert(v2566.id == 585)
                val v2568 = v2567.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2569, v2570) = v2568
                assert(v2569.id == 586)
                matchClassExtends(v2570)
            }
            Some(v2571)
        }
        val v2573 = v2530.asInstanceOf[SequenceNode].children(6)
        val BindNode(v2574, v2575) = v2573
        assert(v2574.id == 589)
        val BindNode(v2576, v2577) = v2575
        val v2586 = v2576.id match {
          case 430 =>
            None
          case 590 =>
            val BindNode(v2578, v2579) = v2577
            val v2585 = v2578.id match {
              case 591 =>
                val BindNode(v2580, v2581) = v2579
                assert(v2580.id == 592)
                val v2582 = v2581.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2583, v2584) = v2582
                assert(v2583.id == 593)
                matchClassImplements(v2584)
            }
            Some(v2585)
        }
        val v2587 = v2530.asInstanceOf[SequenceNode].children(8)
        val BindNode(v2588, v2589) = v2587
        assert(v2588.id == 603)
        NormalClassDeclaration(v2532, matchTypeIdentifier(v2544), v2558, v2572, v2586, matchClassBody(v2589))(nextId(), v2530)
    }
    v2590
  }

  def matchNormalInterfaceDeclaration(node: Node): NormalInterfaceDeclaration = {
    val BindNode(v2591, v2592) = node
    val v2638 = v2591.id match {
      case 1346 =>
        val v2593 = v2592.asInstanceOf[SequenceNode].children.head
        val v2594 = unrollRepeat0(v2593).map { elem =>
          val BindNode(v2595, v2596) = elem
          assert(v2595.id == 1349)
          val BindNode(v2597, v2598) = v2596
          assert(v2597.id == 1350)
          val BindNode(v2599, v2600) = v2598
          assert(v2599.id == 1351)
          val v2601 = v2600.asInstanceOf[SequenceNode].children.head
          val BindNode(v2602, v2603) = v2601
          assert(v2602.id == 1352)
          matchInterfaceModifier(v2603)
        }
        val v2604 = v2592.asInstanceOf[SequenceNode].children(3)
        val BindNode(v2605, v2606) = v2604
        assert(v2605.id == 443)
        val v2607 = v2592.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2608, v2609) = v2607
        assert(v2608.id == 491)
        val BindNode(v2610, v2611) = v2609
        val v2620 = v2610.id match {
          case 430 =>
            None
          case 492 =>
            val BindNode(v2612, v2613) = v2611
            val v2619 = v2612.id match {
              case 493 =>
                val BindNode(v2614, v2615) = v2613
                assert(v2614.id == 494)
                val v2616 = v2615.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2617, v2618) = v2616
                assert(v2617.id == 495)
                matchTypeParameters(v2618)
            }
            Some(v2619)
        }
        val v2621 = v2592.asInstanceOf[SequenceNode].children(5)
        val BindNode(v2622, v2623) = v2621
        assert(v2622.id == 1357)
        val BindNode(v2624, v2625) = v2623
        val v2634 = v2624.id match {
          case 430 =>
            None
          case 1358 =>
            val BindNode(v2626, v2627) = v2625
            val v2633 = v2626.id match {
              case 1359 =>
                val BindNode(v2628, v2629) = v2627
                assert(v2628.id == 1360)
                val v2630 = v2629.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2631, v2632) = v2630
                assert(v2631.id == 1361)
                matchInterfaceExtends(v2632)
            }
            Some(v2633)
        }
        val v2635 = v2592.asInstanceOf[SequenceNode].children(7)
        val BindNode(v2636, v2637) = v2635
        assert(v2636.id == 1363)
        NormalInterfaceDeclaration(v2594, matchTypeIdentifier(v2606), v2620, v2634, matchInterfaceBody(v2637))(nextId(), v2592)
    }
    v2638
  }

  def matchNullLiteral(node: Node): NullLiteral = {
    val BindNode(v2639, v2640) = node
    val v2641 = v2639.id match {
      case 867 =>
        NullLiteral()(nextId(), v2640)
    }
    v2641
  }

  def matchNumericType(node: Node): NumericType = {
    val BindNode(v2642, v2643) = node
    val v2650 = v2642.id match {
      case 528 =>
        val v2644 = v2643.asInstanceOf[SequenceNode].children.head
        val BindNode(v2645, v2646) = v2644
        assert(v2645.id == 529)
        matchIntegralType(v2646)
      case 533 =>
        val v2647 = v2643.asInstanceOf[SequenceNode].children.head
        val BindNode(v2648, v2649) = v2647
        assert(v2648.id == 534)
        matchFloatingPointType(v2649)
    }
    v2650
  }

  def matchOctalDigit(node: Node): Char = {
    val BindNode(v2651, v2652) = node
    val v2656 = v2651.id match {
      case 782 =>
        val v2653 = v2652.asInstanceOf[SequenceNode].children(1)
        val BindNode(v2654, v2655) = v2653
        assert(v2654.id == 778)
        v2655.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
    }
    v2656
  }

  def matchOctalDigits(node: Node): String = {
    val BindNode(v2657, v2658) = node
    val v2666 = v2657.id match {
      case 777 =>
        val v2659 = v2658.asInstanceOf[SequenceNode].children.head
        val BindNode(v2660, v2661) = v2659
        assert(v2660.id == 778)
        val v2662 = v2658.asInstanceOf[SequenceNode].children(1)
        val v2663 = unrollRepeat0(v2662).map { elem =>
          val BindNode(v2664, v2665) = elem
          assert(v2664.id == 781)
          matchOctalDigit(v2665)
        }
        v2661.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v2663.map(x => x.toString).mkString("")
    }
    v2666
  }

  def matchOctalIntegerLiteral(node: Node): OctalIntegerLiteral = {
    val BindNode(v2667, v2668) = node
    val v2686 = v2667.id match {
      case 773 =>
        val v2669 = v2668.asInstanceOf[SequenceNode].children.head
        val BindNode(v2670, v2671) = v2669
        assert(v2670.id == 774)
        val v2672 = v2668.asInstanceOf[SequenceNode].children(1)
        val BindNode(v2673, v2674) = v2672
        assert(v2673.id == 751)
        val BindNode(v2675, v2676) = v2674
        val v2685 = v2675.id match {
          case 430 =>
            None
          case 752 =>
            val BindNode(v2677, v2678) = v2676
            val v2684 = v2677.id match {
              case 753 =>
                val BindNode(v2679, v2680) = v2678
                assert(v2679.id == 754)
                val v2681 = v2680.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2682, v2683) = v2681
                assert(v2682.id == 755)
                matchIntegerTypeSuffix(v2683)
            }
            Some(v2684)
        }
        OctalIntegerLiteral(matchOctalNumeral(v2671), v2685)(nextId(), v2668)
    }
    v2686
  }

  def matchOctalNumeral(node: Node): OctalNumeral = {
    val BindNode(v2687, v2688) = node
    val v2692 = v2687.id match {
      case 775 =>
        val v2689 = v2688.asInstanceOf[SequenceNode].children(1)
        val BindNode(v2690, v2691) = v2689
        assert(v2690.id == 776)
        OctalNumeral(matchOctalDigits(v2691))(nextId(), v2688)
    }
    v2692
  }

  def matchOrdinaryCompilationUnit(node: Node): OrdinaryCompilationUnit = {
    val BindNode(v2693, v2694) = node
    val v2733 = v2693.id match {
      case 5 =>
        val v2695 = v2694.asInstanceOf[SequenceNode].children.head
        val BindNode(v2696, v2697) = v2695
        assert(v2696.id == 6)
        val BindNode(v2698, v2699) = v2697
        val v2708 = v2698.id match {
          case 430 =>
            None
          case 7 =>
            val BindNode(v2700, v2701) = v2699
            val v2707 = v2700.id match {
              case 8 =>
                val BindNode(v2702, v2703) = v2701
                assert(v2702.id == 9)
                val v2704 = v2703.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2705, v2706) = v2704
                assert(v2705.id == 16)
                matchPackageDeclaration(v2706)
            }
            Some(v2707)
        }
        val v2709 = v2694.asInstanceOf[SequenceNode].children(1)
        val v2710 = unrollRepeat0(v2709).map { elem =>
          val BindNode(v2711, v2712) = elem
          assert(v2711.id == 433)
          val BindNode(v2713, v2714) = v2712
          val v2720 = v2713.id match {
            case 434 =>
              val BindNode(v2715, v2716) = v2714
              assert(v2715.id == 435)
              val v2717 = v2716.asInstanceOf[SequenceNode].children(1)
              val BindNode(v2718, v2719) = v2717
              assert(v2718.id == 436)
              matchImportDeclaration(v2719)
          }
          v2720
        }
        val v2721 = v2694.asInstanceOf[SequenceNode].children(2)
        val v2722 = unrollRepeat0(v2721).map { elem =>
          val BindNode(v2723, v2724) = elem
          assert(v2723.id == 472)
          val BindNode(v2725, v2726) = v2724
          val v2732 = v2725.id match {
            case 473 =>
              val BindNode(v2727, v2728) = v2726
              assert(v2727.id == 474)
              val v2729 = v2728.asInstanceOf[SequenceNode].children(1)
              val BindNode(v2730, v2731) = v2729
              assert(v2730.id == 475)
              matchTopLevelClassOrInterfaceDeclaration(v2731)
          }
          v2732
        }
        OrdinaryCompilationUnit(v2708, v2710, v2722)(nextId(), v2694)
    }
    v2733
  }

  def matchPackageDeclaration(node: Node): PackageDeclaration = {
    val BindNode(v2734, v2735) = node
    val v2751 = v2734.id match {
      case 17 =>
        val v2736 = v2735.asInstanceOf[SequenceNode].children(2)
        val BindNode(v2737, v2738) = v2736
        assert(v2737.id == 168)
        val v2739 = v2735.asInstanceOf[SequenceNode].children(3)
        val v2740 = unrollRepeat0(v2739).map { elem =>
          val BindNode(v2741, v2742) = elem
          assert(v2741.id == 426)
          val BindNode(v2743, v2744) = v2742
          val v2750 = v2743.id match {
            case 427 =>
              val BindNode(v2745, v2746) = v2744
              assert(v2745.id == 428)
              val v2747 = v2746.asInstanceOf[SequenceNode].children(3)
              val BindNode(v2748, v2749) = v2747
              assert(v2748.id == 168)
              matchIdentifier(v2749)
          }
          v2750
        }
        PackageDeclaration(List(matchIdentifier(v2738)) ++ v2740)(nextId(), v2735)
    }
    v2751
  }

  def matchPackageName(node: Node): PackageName = {
    val BindNode(v2752, v2753) = node
    val v2769 = v2752.id match {
      case 469 =>
        val v2754 = v2753.asInstanceOf[SequenceNode].children.head
        val BindNode(v2755, v2756) = v2754
        assert(v2755.id == 168)
        val v2757 = v2753.asInstanceOf[SequenceNode].children(1)
        val v2758 = unrollRepeat0(v2757).map { elem =>
          val BindNode(v2759, v2760) = elem
          assert(v2759.id == 426)
          val BindNode(v2761, v2762) = v2760
          val v2768 = v2761.id match {
            case 427 =>
              val BindNode(v2763, v2764) = v2762
              assert(v2763.id == 428)
              val v2765 = v2764.asInstanceOf[SequenceNode].children(3)
              val BindNode(v2766, v2767) = v2765
              assert(v2766.id == 168)
              matchIdentifier(v2767)
          }
          v2768
        }
        PackageName(List(matchIdentifier(v2756)) ++ v2758)(nextId(), v2753)
    }
    v2769
  }

  def matchPackageOrTypeName(node: Node): PackageOrTypeName = {
    val BindNode(v2770, v2771) = node
    val v2787 = v2770.id match {
      case 469 =>
        val v2772 = v2771.asInstanceOf[SequenceNode].children.head
        val BindNode(v2773, v2774) = v2772
        assert(v2773.id == 168)
        val v2775 = v2771.asInstanceOf[SequenceNode].children(1)
        val v2776 = unrollRepeat0(v2775).map { elem =>
          val BindNode(v2777, v2778) = elem
          assert(v2777.id == 426)
          val BindNode(v2779, v2780) = v2778
          val v2786 = v2779.id match {
            case 427 =>
              val BindNode(v2781, v2782) = v2780
              assert(v2781.id == 428)
              val v2783 = v2782.asInstanceOf[SequenceNode].children(3)
              val BindNode(v2784, v2785) = v2783
              assert(v2784.id == 168)
              matchIdentifier(v2785)
          }
          v2786
        }
        PackageOrTypeName(List(matchIdentifier(v2774)) ++ v2776)(nextId(), v2771)
    }
    v2787
  }

  def matchPostDecrementExpression(node: Node): PostDecrementExpression = {
    val BindNode(v2788, v2789) = node
    val v2793 = v2788.id match {
      case 1024 =>
        val v2790 = v2789.asInstanceOf[SequenceNode].children.head
        val BindNode(v2791, v2792) = v2790
        assert(v2791.id == 1017)
        PostDecrementExpression(matchPostfixExpression(v2792))(nextId(), v2789)
    }
    v2793
  }

  def matchPostIncrementExpression(node: Node): PostIncrementExpression = {
    val BindNode(v2794, v2795) = node
    val v2799 = v2794.id match {
      case 1021 =>
        val v2796 = v2795.asInstanceOf[SequenceNode].children.head
        val BindNode(v2797, v2798) = v2796
        assert(v2797.id == 1017)
        PostIncrementExpression(matchPostfixExpression(v2798))(nextId(), v2795)
    }
    v2799
  }

  def matchPostfixExpression(node: Node): PostfixExpression = {
    val BindNode(v2800, v2801) = node
    val v2814 = v2800.id match {
      case 1018 =>
        val v2802 = v2801.asInstanceOf[SequenceNode].children.head
        val BindNode(v2803, v2804) = v2802
        assert(v2803.id == 726)
        matchPrimary(v2804)
      case 719 =>
        val v2805 = v2801.asInstanceOf[SequenceNode].children.head
        val BindNode(v2806, v2807) = v2805
        assert(v2806.id == 720)
        matchExpressionName(v2807)
      case 1019 =>
        val v2808 = v2801.asInstanceOf[SequenceNode].children.head
        val BindNode(v2809, v2810) = v2808
        assert(v2809.id == 1020)
        matchPostIncrementExpression(v2810)
      case 1022 =>
        val v2811 = v2801.asInstanceOf[SequenceNode].children.head
        val BindNode(v2812, v2813) = v2811
        assert(v2812.id == 1023)
        matchPostDecrementExpression(v2813)
    }
    v2814
  }

  def matchPreDecrementExpression(node: Node): UnaryOp = {
    val BindNode(v2815, v2816) = node
    val v2820 = v2815.id match {
      case 1008 =>
        val v2817 = v2816.asInstanceOf[SequenceNode].children(2)
        val BindNode(v2818, v2819) = v2817
        assert(v2818.id == 1005)
        UnaryOp(UnaryOps.DECREMENT, matchUnaryExpression(v2819))(nextId(), v2816)
    }
    v2820
  }

  def matchPreIncrementExpression(node: Node): UnaryOp = {
    val BindNode(v2821, v2822) = node
    val v2826 = v2821.id match {
      case 1003 =>
        val v2823 = v2822.asInstanceOf[SequenceNode].children(2)
        val BindNode(v2824, v2825) = v2823
        assert(v2824.id == 1005)
        UnaryOp(UnaryOps.INCREMENT, matchUnaryExpression(v2825))(nextId(), v2822)
    }
    v2826
  }

  def matchPrimary(node: Node): Primary = {
    val BindNode(v2827, v2828) = node
    val v2835 = v2827.id match {
      case 727 =>
        val v2829 = v2828.asInstanceOf[SequenceNode].children.head
        val BindNode(v2830, v2831) = v2829
        assert(v2830.id == 728)
        matchPrimaryNoNewArray(v2831)
      case 967 =>
        val v2832 = v2828.asInstanceOf[SequenceNode].children.head
        val BindNode(v2833, v2834) = v2832
        assert(v2833.id == 968)
        matchArrayCreationExpression(v2834)
    }
    v2835
  }

  def matchPrimaryNoNewArray(node: Node): PrimaryNoNewArray = {
    val BindNode(v2836, v2837) = node
    val v2865 = v2836.id match {
      case 942 =>
        val v2838 = v2837.asInstanceOf[SequenceNode].children.head
        val BindNode(v2839, v2840) = v2838
        assert(v2839.id == 943)
        matchMethodInvocation(v2840)
      case 869 =>
        val v2841 = v2837.asInstanceOf[SequenceNode].children.head
        val BindNode(v2842, v2843) = v2841
        assert(v2842.id == 870)
        matchClassLiteral(v2843)
      case 729 =>
        val v2844 = v2837.asInstanceOf[SequenceNode].children.head
        val BindNode(v2845, v2846) = v2844
        assert(v2845.id == 730)
        matchLiteral(v2846)
      case 916 =>
        val v2847 = v2837.asInstanceOf[SequenceNode].children.head
        val BindNode(v2848, v2849) = v2847
        assert(v2848.id == 917)
        matchArrayAccess(v2849)
      case 879 =>
        val v2850 = v2837.asInstanceOf[SequenceNode].children.head
        val BindNode(v2851, v2852) = v2850
        assert(v2851.id == 441)
        This(Some(matchTypeName(v2852)))(nextId(), v2837)
      case 881 =>
        val v2853 = v2837.asInstanceOf[SequenceNode].children.head
        val BindNode(v2854, v2855) = v2853
        assert(v2854.id == 882)
        matchClassInstanceCreationExpression(v2855)
      case 957 =>
        val v2856 = v2837.asInstanceOf[SequenceNode].children.head
        val BindNode(v2857, v2858) = v2856
        assert(v2857.id == 958)
        matchMethodReference(v2858)
      case 880 =>
        val v2859 = v2837.asInstanceOf[SequenceNode].children(2)
        val BindNode(v2860, v2861) = v2859
        assert(v2860.id == 644)
        Paren(matchExpression(v2861))(nextId(), v2837)
      case 877 =>
        This(None)(nextId(), v2837)
      case 723 =>
        val v2862 = v2837.asInstanceOf[SequenceNode].children.head
        val BindNode(v2863, v2864) = v2862
        assert(v2863.id == 724)
        matchFieldAccess(v2864)
    }
    v2865
  }

  def matchPrimitiveType(node: Node): PrimitiveType = {
    val BindNode(v2866, v2867) = node
    val v2871 = v2866.id match {
      case 526 =>
        val v2868 = v2867.asInstanceOf[SequenceNode].children.head
        val BindNode(v2869, v2870) = v2868
        assert(v2869.id == 527)
        matchNumericType(v2870)
      case 245 =>
        BooleanType()(nextId(), v2867)
    }
    v2871
  }

  def matchReferenceType(node: Node): ReferenceType = {
    val BindNode(v2872, v2873) = node
    val v2880 = v2872.id match {
      case 521 =>
        val v2874 = v2873.asInstanceOf[SequenceNode].children.head
        val BindNode(v2875, v2876) = v2874
        assert(v2875.id == 508)
        matchClassOrInterfaceType(v2876)
      case 522 =>
        val v2877 = v2873.asInstanceOf[SequenceNode].children.head
        val BindNode(v2878, v2879) = v2877
        assert(v2878.id == 523)
        matchArrayType(v2879)
    }
    v2880
  }

  def matchRelationalExpression(node: Node): RelationalExpression = {
    val BindNode(v2881, v2882) = node
    val v2913 = v2881.id match {
      case 1198 =>
        val v2883 = v2882.asInstanceOf[SequenceNode].children.head
        val BindNode(v2884, v2885) = v2883
        assert(v2884.id == 1170)
        val v2886 = v2882.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2887, v2888) = v2886
        assert(v2887.id == 1172)
        BinOp(BinOps.LE, matchRelationalExpression(v2885), matchShiftExpression(v2888))(nextId(), v2882)
      case 1192 =>
        val v2889 = v2882.asInstanceOf[SequenceNode].children.head
        val BindNode(v2890, v2891) = v2889
        assert(v2890.id == 1170)
        val v2892 = v2882.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2893, v2894) = v2892
        assert(v2893.id == 1172)
        BinOp(BinOps.GT, matchRelationalExpression(v2891), matchShiftExpression(v2894))(nextId(), v2882)
      case 1194 =>
        val v2895 = v2882.asInstanceOf[SequenceNode].children.head
        val BindNode(v2896, v2897) = v2895
        assert(v2896.id == 1170)
        val v2898 = v2882.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2899, v2900) = v2898
        assert(v2899.id == 1172)
        BinOp(BinOps.LT, matchRelationalExpression(v2897), matchShiftExpression(v2900))(nextId(), v2882)
      case 1200 =>
        val v2901 = v2882.asInstanceOf[SequenceNode].children.head
        val BindNode(v2902, v2903) = v2901
        assert(v2902.id == 1201)
        matchInstanceofExpression(v2903)
      case 1196 =>
        val v2904 = v2882.asInstanceOf[SequenceNode].children.head
        val BindNode(v2905, v2906) = v2904
        assert(v2905.id == 1170)
        val v2907 = v2882.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2908, v2909) = v2907
        assert(v2908.id == 1172)
        BinOp(BinOps.GE, matchRelationalExpression(v2906), matchShiftExpression(v2909))(nextId(), v2882)
      case 1171 =>
        val v2910 = v2882.asInstanceOf[SequenceNode].children.head
        val BindNode(v2911, v2912) = v2910
        assert(v2911.id == 1172)
        matchShiftExpression(v2912)
    }
    v2913
  }

  def matchResult(node: Node): Result = {
    val BindNode(v2914, v2915) = node
    val v2919 = v2914.id match {
      case 669 =>
        val v2916 = v2915.asInstanceOf[SequenceNode].children.head
        val BindNode(v2917, v2918) = v2916
        assert(v2917.id == 626)
        matchType(v2918)
      case 1245 =>
        VoidResult()(nextId(), v2915)
    }
    v2919
  }

  def matchReturnStatement(node: Node): ReturnStatement = {
    val BindNode(v2920, v2921) = node
    val v2936 = v2920.id match {
      case 1052 =>
        val v2922 = v2921.asInstanceOf[SequenceNode].children(1)
        val BindNode(v2923, v2924) = v2922
        assert(v2923.id == 1054)
        val BindNode(v2925, v2926) = v2924
        val v2935 = v2925.id match {
          case 430 =>
            None
          case 1055 =>
            val BindNode(v2927, v2928) = v2926
            val v2934 = v2927.id match {
              case 1056 =>
                val BindNode(v2929, v2930) = v2928
                assert(v2929.id == 1057)
                val v2931 = v2930.asInstanceOf[SequenceNode].children(1)
                val BindNode(v2932, v2933) = v2931
                assert(v2932.id == 644)
                matchExpression(v2933)
            }
            Some(v2934)
        }
        ReturnStatement(v2935)(nextId(), v2921)
    }
    v2936
  }

  def matchShiftExpression(node: Node): ShiftExpression = {
    val BindNode(v2937, v2938) = node
    val v2960 = v2937.id match {
      case 1173 =>
        val v2939 = v2938.asInstanceOf[SequenceNode].children.head
        val BindNode(v2940, v2941) = v2939
        assert(v2940.id == 1174)
        matchAdditiveExpression(v2941)
      case 1186 =>
        val v2942 = v2938.asInstanceOf[SequenceNode].children.head
        val BindNode(v2943, v2944) = v2942
        assert(v2943.id == 1172)
        val v2945 = v2938.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2946, v2947) = v2945
        assert(v2946.id == 1174)
        BinOp(BinOps.SHL, matchShiftExpression(v2944), matchAdditiveExpression(v2947))(nextId(), v2938)
      case 1188 =>
        val v2948 = v2938.asInstanceOf[SequenceNode].children.head
        val BindNode(v2949, v2950) = v2948
        assert(v2949.id == 1172)
        val v2951 = v2938.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2952, v2953) = v2951
        assert(v2952.id == 1174)
        BinOp(BinOps.SHR, matchShiftExpression(v2950), matchAdditiveExpression(v2953))(nextId(), v2938)
      case 1190 =>
        val v2954 = v2938.asInstanceOf[SequenceNode].children.head
        val BindNode(v2955, v2956) = v2954
        assert(v2955.id == 1172)
        val v2957 = v2938.asInstanceOf[SequenceNode].children(4)
        val BindNode(v2958, v2959) = v2957
        assert(v2958.id == 1174)
        BinOp(BinOps.SHRZ, matchShiftExpression(v2956), matchAdditiveExpression(v2959))(nextId(), v2938)
    }
    v2960
  }

  def matchSignedInteger(node: Node): SignedInteger = {
    val BindNode(v2961, v2962) = node
    val v2985 = v2961.id match {
      case 806 =>
        val v2963 = v2962.asInstanceOf[SequenceNode].children.head
        val BindNode(v2964, v2965) = v2963
        assert(v2964.id == 807)
        val BindNode(v2966, v2967) = v2965
        val v2981 = v2966.id match {
          case 430 =>
            None
          case 808 =>
            val BindNode(v2968, v2969) = v2967
            val v2980 = v2968.id match {
              case 72 =>
                val BindNode(v2970, v2971) = v2969
                assert(v2970.id == 73)
                val v2972 = v2971.asInstanceOf[SequenceNode].children.head
                val BindNode(v2973, v2974) = v2972
                assert(v2973.id == 74)
                v2974.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
              case 82 =>
                val BindNode(v2975, v2976) = v2969
                assert(v2975.id == 83)
                val v2977 = v2976.asInstanceOf[SequenceNode].children.head
                val BindNode(v2978, v2979) = v2977
                assert(v2978.id == 84)
                v2979.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v2980)
        }
        val v2982 = v2962.asInstanceOf[SequenceNode].children(1)
        val BindNode(v2983, v2984) = v2982
        assert(v2983.id == 742)
        SignedInteger(v2981, matchDigits(v2984))(nextId(), v2962)
    }
    v2985
  }

  def matchSimpleTypeName(node: Node): TypeIdentifier = {
    val BindNode(v2986, v2987) = node
    val v2991 = v2986.id match {
      case 442 =>
        val v2988 = v2987.asInstanceOf[SequenceNode].children.head
        val BindNode(v2989, v2990) = v2988
        assert(v2989.id == 443)
        matchTypeIdentifier(v2990)
    }
    v2991
  }

  def matchSingleCharacter(node: Node): SingleCharacter = {
    val BindNode(v2992, v2993) = node
    val v2999 = v2992.id match {
      case 829 =>
        val v2994 = v2993.asInstanceOf[SequenceNode].children.head
        val BindNode(v2995, v2996) = v2994
        assert(v2995.id == 830)
        val BindNode(v2997, v2998) = v2996
        assert(v2997.id == 831)
        SingleCharacter(matchInputCharacter(v2998))(nextId(), v2993)
    }
    v2999
  }

  def matchSingleTypeImportDeclaration(node: Node): SingleTypeImportDeclaration = {
    val BindNode(v3000, v3001) = node
    val v3005 = v3000.id match {
      case 439 =>
        val v3002 = v3001.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3003, v3004) = v3002
        assert(v3003.id == 441)
        SingleTypeImportDeclaration(matchTypeName(v3004))(nextId(), v3001)
    }
    v3005
  }

  def matchStatement(node: Node): Statement = {
    val BindNode(v3006, v3007) = node
    val v3026 = v3006.id match {
      case 1143 =>
        val v3008 = v3007.asInstanceOf[SequenceNode].children.head
        val BindNode(v3009, v3010) = v3008
        assert(v3009.id == 1144)
        matchForStatement(v3010)
      case 1096 =>
        val v3011 = v3007.asInstanceOf[SequenceNode].children.head
        val BindNode(v3012, v3013) = v3011
        assert(v3012.id == 1097)
        matchIfThenStatement(v3013)
      case 1140 =>
        val v3014 = v3007.asInstanceOf[SequenceNode].children.head
        val BindNode(v3015, v3016) = v3014
        assert(v3015.id == 1141)
        matchWhileStatement(v3016)
      case 1093 =>
        val v3017 = v3007.asInstanceOf[SequenceNode].children.head
        val BindNode(v3018, v3019) = v3017
        assert(v3018.id == 1094)
        matchLabeledStatement(v3019)
      case 1100 =>
        val v3020 = v3007.asInstanceOf[SequenceNode].children.head
        val BindNode(v3021, v3022) = v3020
        assert(v3021.id == 1101)
        matchIfThenElseStatement(v3022)
      case 706 =>
        val v3023 = v3007.asInstanceOf[SequenceNode].children.head
        val BindNode(v3024, v3025) = v3023
        assert(v3024.id == 707)
        matchStatementWithoutTrailingSubstatement(v3025)
    }
    v3026
  }

  def matchStatementExpression(node: Node): StatementExpression = {
    val BindNode(v3027, v3028) = node
    val v3050 = v3027.id match {
      case 715 =>
        val v3029 = v3028.asInstanceOf[SequenceNode].children.head
        val BindNode(v3030, v3031) = v3029
        assert(v3030.id == 716)
        matchAssignment(v3031)
      case 942 =>
        val v3032 = v3028.asInstanceOf[SequenceNode].children.head
        val BindNode(v3033, v3034) = v3032
        assert(v3033.id == 943)
        matchMethodInvocation(v3034)
      case 1001 =>
        val v3035 = v3028.asInstanceOf[SequenceNode].children.head
        val BindNode(v3036, v3037) = v3035
        assert(v3036.id == 1002)
        matchPreIncrementExpression(v3037)
      case 881 =>
        val v3038 = v3028.asInstanceOf[SequenceNode].children.head
        val BindNode(v3039, v3040) = v3038
        assert(v3039.id == 882)
        matchClassInstanceCreationExpression(v3040)
      case 1022 =>
        val v3041 = v3028.asInstanceOf[SequenceNode].children.head
        val BindNode(v3042, v3043) = v3041
        assert(v3042.id == 1023)
        matchPostDecrementExpression(v3043)
      case 1006 =>
        val v3044 = v3028.asInstanceOf[SequenceNode].children.head
        val BindNode(v3045, v3046) = v3044
        assert(v3045.id == 1007)
        matchPreDecrementExpression(v3046)
      case 1019 =>
        val v3047 = v3028.asInstanceOf[SequenceNode].children.head
        val BindNode(v3048, v3049) = v3047
        assert(v3048.id == 1020)
        matchPostIncrementExpression(v3049)
    }
    v3050
  }

  def matchStatementExpressionList(node: Node): StatementExpressionList = {
    val BindNode(v3051, v3052) = node
    val v3068 = v3051.id match {
      case 1128 =>
        val v3053 = v3052.asInstanceOf[SequenceNode].children.head
        val BindNode(v3054, v3055) = v3053
        assert(v3054.id == 714)
        val v3056 = v3052.asInstanceOf[SequenceNode].children(1)
        val v3057 = unrollRepeat0(v3056).map { elem =>
          val BindNode(v3058, v3059) = elem
          assert(v3058.id == 1131)
          val BindNode(v3060, v3061) = v3059
          val v3067 = v3060.id match {
            case 1132 =>
              val BindNode(v3062, v3063) = v3061
              assert(v3062.id == 1133)
              val v3064 = v3063.asInstanceOf[SequenceNode].children(3)
              val BindNode(v3065, v3066) = v3064
              assert(v3065.id == 714)
              matchStatementExpression(v3066)
          }
          v3067
        }
        StatementExpressionList(List(matchStatementExpression(v3055)) ++ v3057)(nextId(), v3052)
    }
    v3068
  }

  def matchStatementNoShortIf(node: Node): StatementNoShortIf = {
    val BindNode(v3069, v3070) = node
    val v3086 = v3069.id match {
      case 1115 =>
        val v3071 = v3070.asInstanceOf[SequenceNode].children.head
        val BindNode(v3072, v3073) = v3071
        assert(v3072.id == 1116)
        matchForStatementNoShortIf(v3073)
      case 1104 =>
        val v3074 = v3070.asInstanceOf[SequenceNode].children.head
        val BindNode(v3075, v3076) = v3074
        assert(v3075.id == 1105)
        matchLabeledStatementNoShortIf(v3076)
      case 1107 =>
        val v3077 = v3070.asInstanceOf[SequenceNode].children.head
        val BindNode(v3078, v3079) = v3077
        assert(v3078.id == 1108)
        matchIfThenElseStatementNoShortIf(v3079)
      case 1111 =>
        val v3080 = v3070.asInstanceOf[SequenceNode].children.head
        val BindNode(v3081, v3082) = v3080
        assert(v3081.id == 1112)
        matchWhileStatementNoShortIf(v3082)
      case 706 =>
        val v3083 = v3070.asInstanceOf[SequenceNode].children.head
        val BindNode(v3084, v3085) = v3083
        assert(v3084.id == 707)
        matchStatementWithoutTrailingSubstatement(v3085)
    }
    v3086
  }

  def matchStatementWithoutTrailingSubstatement(node: Node): StatementWithoutTrailingSubstatement = {
    val BindNode(v3087, v3088) = node
    val v3116 = v3087.id match {
      case 708 =>
        val v3089 = v3088.asInstanceOf[SequenceNode].children.head
        val BindNode(v3090, v3091) = v3089
        assert(v3090.id == 709)
        matchEmptyStatement(v3091)
      case 1062 =>
        val v3092 = v3088.asInstanceOf[SequenceNode].children.head
        val BindNode(v3093, v3094) = v3092
        assert(v3093.id == 1063)
        matchTryStatement(v3094)
      case 711 =>
        val v3095 = v3088.asInstanceOf[SequenceNode].children.head
        val BindNode(v3096, v3097) = v3095
        assert(v3096.id == 712)
        matchExpressionStatement(v3097)
      case 1034 =>
        val v3098 = v3088.asInstanceOf[SequenceNode].children.head
        val BindNode(v3099, v3100) = v3098
        assert(v3099.id == 1035)
        matchDoStatement(v3100)
      case 1058 =>
        val v3101 = v3088.asInstanceOf[SequenceNode].children.head
        val BindNode(v3102, v3103) = v3101
        assert(v3102.id == 1059)
        matchThrowStatement(v3103)
      case 1050 =>
        val v3104 = v3088.asInstanceOf[SequenceNode].children.head
        val BindNode(v3105, v3106) = v3104
        assert(v3105.id == 1051)
        matchReturnStatement(v3106)
      case 1038 =>
        val v3107 = v3088.asInstanceOf[SequenceNode].children.head
        val BindNode(v3108, v3109) = v3107
        assert(v3108.id == 1039)
        matchBreakStatement(v3109)
      case 1046 =>
        val v3110 = v3088.asInstanceOf[SequenceNode].children.head
        val BindNode(v3111, v3112) = v3110
        assert(v3111.id == 1047)
        matchContinueStatement(v3112)
      case 688 =>
        val v3113 = v3088.asInstanceOf[SequenceNode].children.head
        val BindNode(v3114, v3115) = v3113
        assert(v3114.id == 689)
        matchBlock(v3115)
    }
    v3116
  }

  def matchStaticInitializer(node: Node): StaticInitializer = {
    val BindNode(v3117, v3118) = node
    val v3122 = v3117.id match {
      case 1281 =>
        val v3119 = v3118.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3120, v3121) = v3119
        assert(v3120.id == 689)
        StaticInitializer(matchBlock(v3121))(nextId(), v3118)
    }
    v3122
  }

  def matchStringCharacter(node: Node): StringCharacter = {
    val BindNode(v3123, v3124) = node
    val v3131 = v3123.id match {
      case 859 =>
        val v3125 = v3124.asInstanceOf[SequenceNode].children.head
        val BindNode(v3126, v3127) = v3125
        assert(v3126.id == 860)
        matchStringSingleCharacter(v3127)
      case 864 =>
        val v3128 = v3124.asInstanceOf[SequenceNode].children.head
        val BindNode(v3129, v3130) = v3128
        assert(v3129.id == 838)
        matchEscapeSequence(v3130)
    }
    v3131
  }

  def matchStringLiteral(node: Node): StringLiteral = {
    val BindNode(v3132, v3133) = node
    val v3138 = v3132.id match {
      case 854 =>
        val v3134 = v3133.asInstanceOf[SequenceNode].children(1)
        val v3135 = unrollRepeat0(v3134).map { elem =>
          val BindNode(v3136, v3137) = elem
          assert(v3136.id == 858)
          matchStringCharacter(v3137)
        }
        StringLiteral(v3135)(nextId(), v3133)
    }
    v3138
  }

  def matchStringSingleCharacter(node: Node): StringSingleCharacter = {
    val BindNode(v3139, v3140) = node
    val v3146 = v3139.id match {
      case 861 =>
        val v3141 = v3140.asInstanceOf[SequenceNode].children.head
        val BindNode(v3142, v3143) = v3141
        assert(v3142.id == 862)
        val BindNode(v3144, v3145) = v3143
        assert(v3144.id == 831)
        StringSingleCharacter(matchInputCharacter(v3145))(nextId(), v3140)
    }
    v3146
  }

  def matchThrowStatement(node: Node): ThrowStatement = {
    val BindNode(v3147, v3148) = node
    val v3152 = v3147.id match {
      case 1060 =>
        val v3149 = v3148.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3150, v3151) = v3149
        assert(v3150.id == 644)
        ThrowStatement(matchExpression(v3151))(nextId(), v3148)
    }
    v3152
  }

  def matchThrows(node: Node): Throws = {
    val BindNode(v3153, v3154) = node
    val v3158 = v3153.id match {
      case 1266 =>
        val v3155 = v3154.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3156, v3157) = v3155
        assert(v3156.id == 1268)
        Throws(matchExceptionTypeList(v3157))(nextId(), v3154)
    }
    v3158
  }

  def matchTopLevelClassOrInterfaceDeclaration(node: Node): TopLevelClassOrInterfaceDeclaration = {
    val BindNode(v3159, v3160) = node
    val v3167 = v3159.id match {
      case 476 =>
        val v3161 = v3160.asInstanceOf[SequenceNode].children.head
        val BindNode(v3162, v3163) = v3161
        assert(v3162.id == 477)
        matchClassDeclaration(v3163)
      case 1342 =>
        val v3164 = v3160.asInstanceOf[SequenceNode].children.head
        val BindNode(v3165, v3166) = v3164
        assert(v3165.id == 1343)
        matchInterfaceDeclaration(v3166)
    }
    v3167
  }

  def matchTryStatement(node: Node): TryStatement = {
    val BindNode(v3168, v3169) = node
    val v3197 = v3168.id match {
      case 1064 =>
        val v3170 = v3169.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3171, v3172) = v3170
        assert(v3171.id == 689)
        val v3173 = v3169.asInstanceOf[SequenceNode].children(4)
        val BindNode(v3174, v3175) = v3173
        assert(v3174.id == 1066)
        TryStatement(matchBlock(v3172), matchCatches(v3175), None)(nextId(), v3169)
      case 1085 =>
        val v3176 = v3169.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3177, v3178) = v3176
        assert(v3177.id == 689)
        val v3180 = v3169.asInstanceOf[SequenceNode].children(3)
        val BindNode(v3181, v3182) = v3180
        assert(v3181.id == 1086)
        val BindNode(v3183, v3184) = v3182
        val v3193 = v3183.id match {
          case 430 =>
            None
          case 1087 =>
            val BindNode(v3185, v3186) = v3184
            val v3192 = v3185.id match {
              case 1088 =>
                val BindNode(v3187, v3188) = v3186
                assert(v3187.id == 1089)
                val v3189 = v3188.asInstanceOf[SequenceNode].children(1)
                val BindNode(v3190, v3191) = v3189
                assert(v3190.id == 1066)
                matchCatches(v3191)
            }
            Some(v3192)
        }
        val v3179 = v3193
        val v3194 = v3169.asInstanceOf[SequenceNode].children(5)
        val BindNode(v3195, v3196) = v3194
        assert(v3195.id == 1090)
        TryStatement(matchBlock(v3178), if (v3179.isDefined) v3179.get else List(), Some(matchFinally(v3196)))(nextId(), v3169)
    }
    v3197
  }

  def matchType(node: Node): Type = {
    val BindNode(v3198, v3199) = node
    val v3206 = v3198.id match {
      case 627 =>
        val v3200 = v3199.asInstanceOf[SequenceNode].children.head
        val BindNode(v3201, v3202) = v3200
        assert(v3201.id == 525)
        matchPrimitiveType(v3202)
      case 519 =>
        val v3203 = v3199.asInstanceOf[SequenceNode].children.head
        val BindNode(v3204, v3205) = v3203
        assert(v3204.id == 520)
        matchReferenceType(v3205)
    }
    v3206
  }

  def matchTypeArgument(node: Node): TypeArgument = {
    val BindNode(v3207, v3208) = node
    val v3215 = v3207.id match {
      case 519 =>
        val v3209 = v3208.asInstanceOf[SequenceNode].children.head
        val BindNode(v3210, v3211) = v3209
        assert(v3210.id == 520)
        matchReferenceType(v3211)
      case 548 =>
        val v3212 = v3208.asInstanceOf[SequenceNode].children.head
        val BindNode(v3213, v3214) = v3212
        assert(v3213.id == 549)
        matchWildcard(v3214)
    }
    v3215
  }

  def matchTypeArgumentList(node: Node): List[TypeArgument] = {
    val BindNode(v3216, v3217) = node
    val v3233 = v3216.id match {
      case 517 =>
        val v3218 = v3217.asInstanceOf[SequenceNode].children.head
        val BindNode(v3219, v3220) = v3218
        assert(v3219.id == 518)
        val v3221 = v3217.asInstanceOf[SequenceNode].children(1)
        val v3222 = unrollRepeat0(v3221).map { elem =>
          val BindNode(v3223, v3224) = elem
          assert(v3223.id == 562)
          val BindNode(v3225, v3226) = v3224
          val v3232 = v3225.id match {
            case 563 =>
              val BindNode(v3227, v3228) = v3226
              assert(v3227.id == 564)
              val v3229 = v3228.asInstanceOf[SequenceNode].children(3)
              val BindNode(v3230, v3231) = v3229
              assert(v3230.id == 518)
              matchTypeArgument(v3231)
          }
          v3232
        }
        List(matchTypeArgument(v3220)) ++ v3222
    }
    v3233
  }

  def matchTypeArguments(node: Node): TypeArguments = {
    val BindNode(v3234, v3235) = node
    val v3239 = v3234.id match {
      case 515 =>
        val v3236 = v3235.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3237, v3238) = v3236
        assert(v3237.id == 516)
        TypeArguments(matchTypeArgumentList(v3238))(nextId(), v3235)
    }
    v3239
  }

  def matchTypeArgumentsOrDiamond(node: Node): TypeArgumentsOrDiamond = {
    val BindNode(v3240, v3241) = node
    val v3245 = v3240.id match {
      case 894 =>
        val v3242 = v3241.asInstanceOf[SequenceNode].children.head
        val BindNode(v3243, v3244) = v3242
        assert(v3243.id == 514)
        matchTypeArguments(v3244)
      case 895 =>
        DiamondTypeArguments()(nextId(), v3241)
    }
    v3245
  }

  def matchTypeBound(node: Node): TypeBound = {
    val BindNode(v3246, v3247) = node
    val v3263 = v3246.id match {
      case 506 =>
        val v3248 = v3247.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3249, v3250) = v3248
        assert(v3249.id == 508)
        val v3251 = v3247.asInstanceOf[SequenceNode].children(3)
        val v3252 = unrollRepeat0(v3251).map { elem =>
          val BindNode(v3253, v3254) = elem
          assert(v3253.id == 571)
          val BindNode(v3255, v3256) = v3254
          val v3262 = v3255.id match {
            case 572 =>
              val BindNode(v3257, v3258) = v3256
              assert(v3257.id == 573)
              val v3259 = v3258.asInstanceOf[SequenceNode].children(1)
              val BindNode(v3260, v3261) = v3259
              assert(v3260.id == 574)
              matchAdditionalBound(v3261)
          }
          v3262
        }
        ExtendingConcrete(matchClassOrInterfaceType(v3250), v3252)(nextId(), v3247)
    }
    v3263
  }

  def matchTypeIdentifier(node: Node): TypeIdentifier = {
    val BindNode(v3264, v3265) = node
    val v3271 = v3264.id match {
      case 444 =>
        val v3266 = v3265.asInstanceOf[SequenceNode].children.head
        val BindNode(v3267, v3268) = v3266
        assert(v3267.id == 445)
        val BindNode(v3269, v3270) = v3268
        assert(v3269.id == 168)
        TypeIdentifier(matchIdentifier(v3270))(nextId(), v3265)
    }
    v3271
  }

  def matchTypeName(node: Node): TypeName = {
    val BindNode(v3272, v3273) = node
    val v3283 = v3272.id match {
      case 442 =>
        val v3274 = v3273.asInstanceOf[SequenceNode].children.head
        val BindNode(v3275, v3276) = v3274
        assert(v3275.id == 443)
        TypeName(None, matchTypeIdentifier(v3276))(nextId(), v3273)
      case 467 =>
        val v3277 = v3273.asInstanceOf[SequenceNode].children.head
        val BindNode(v3278, v3279) = v3277
        assert(v3278.id == 468)
        val v3280 = v3273.asInstanceOf[SequenceNode].children(4)
        val BindNode(v3281, v3282) = v3280
        assert(v3281.id == 443)
        TypeName(Some(matchPackageOrTypeName(v3279)), matchTypeIdentifier(v3282))(nextId(), v3273)
    }
    v3283
  }

  def matchTypeOrExpressionName(node: Node): ExpressionName = {
    val BindNode(v3284, v3285) = node
    val v3289 = v3284.id match {
      case 719 =>
        val v3286 = v3285.asInstanceOf[SequenceNode].children.head
        val BindNode(v3287, v3288) = v3286
        assert(v3287.id == 720)
        matchExpressionName(v3288)
    }
    v3289
  }

  def matchTypeParameter(node: Node): TypeParameter = {
    val BindNode(v3290, v3291) = node
    val v3309 = v3290.id match {
      case 500 =>
        val v3292 = v3291.asInstanceOf[SequenceNode].children.head
        val BindNode(v3293, v3294) = v3292
        assert(v3293.id == 443)
        val v3295 = v3291.asInstanceOf[SequenceNode].children(1)
        val BindNode(v3296, v3297) = v3295
        assert(v3296.id == 501)
        val BindNode(v3298, v3299) = v3297
        val v3308 = v3298.id match {
          case 430 =>
            None
          case 502 =>
            val BindNode(v3300, v3301) = v3299
            val v3307 = v3300.id match {
              case 503 =>
                val BindNode(v3302, v3303) = v3301
                assert(v3302.id == 504)
                val v3304 = v3303.asInstanceOf[SequenceNode].children(1)
                val BindNode(v3305, v3306) = v3304
                assert(v3305.id == 505)
                matchTypeBound(v3306)
            }
            Some(v3307)
        }
        TypeParameter(matchTypeIdentifier(v3294), v3308)(nextId(), v3291)
    }
    v3309
  }

  def matchTypeParameterList(node: Node): List[TypeParameter] = {
    val BindNode(v3310, v3311) = node
    val v3327 = v3310.id match {
      case 498 =>
        val v3312 = v3311.asInstanceOf[SequenceNode].children.head
        val BindNode(v3313, v3314) = v3312
        assert(v3313.id == 499)
        val v3315 = v3311.asInstanceOf[SequenceNode].children(1)
        val v3316 = unrollRepeat0(v3315).map { elem =>
          val BindNode(v3317, v3318) = elem
          assert(v3317.id == 579)
          val BindNode(v3319, v3320) = v3318
          val v3326 = v3319.id match {
            case 580 =>
              val BindNode(v3321, v3322) = v3320
              assert(v3321.id == 581)
              val v3323 = v3322.asInstanceOf[SequenceNode].children(3)
              val BindNode(v3324, v3325) = v3323
              assert(v3324.id == 499)
              matchTypeParameter(v3325)
          }
          v3326
        }
        List(matchTypeParameter(v3314)) ++ v3316
    }
    v3327
  }

  def matchTypeParameters(node: Node): TypeParameters = {
    val BindNode(v3328, v3329) = node
    val v3333 = v3328.id match {
      case 496 =>
        val v3330 = v3329.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3331, v3332) = v3330
        assert(v3331.id == 497)
        TypeParameters(matchTypeParameterList(v3332))(nextId(), v3329)
    }
    v3333
  }

  def matchUnaryExpression(node: Node): UnaryExpression = {
    val BindNode(v3334, v3335) = node
    val v3351 = v3334.id match {
      case 1012 =>
        val v3336 = v3335.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3337, v3338) = v3336
        assert(v3337.id == 1005)
        UnaryOp(UnaryOps.MINUS, matchUnaryExpression(v3338))(nextId(), v3335)
      case 1014 =>
        val v3339 = v3335.asInstanceOf[SequenceNode].children.head
        val BindNode(v3340, v3341) = v3339
        assert(v3340.id == 1015)
        matchUnaryExpressionNotPlusMinus(v3341)
      case 1006 =>
        val v3342 = v3335.asInstanceOf[SequenceNode].children.head
        val BindNode(v3343, v3344) = v3342
        assert(v3343.id == 1007)
        matchPreDecrementExpression(v3344)
      case 1001 =>
        val v3345 = v3335.asInstanceOf[SequenceNode].children.head
        val BindNode(v3346, v3347) = v3345
        assert(v3346.id == 1002)
        matchPreIncrementExpression(v3347)
      case 1010 =>
        val v3348 = v3335.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3349, v3350) = v3348
        assert(v3349.id == 1005)
        UnaryOp(UnaryOps.PLUS, matchUnaryExpression(v3350))(nextId(), v3335)
    }
    v3351
  }

  def matchUnaryExpressionNotPlusMinus(node: Node): UnaryExpressionNotPlusMinus = {
    val BindNode(v3352, v3353) = node
    val v3366 = v3352.id match {
      case 1016 =>
        val v3354 = v3353.asInstanceOf[SequenceNode].children.head
        val BindNode(v3355, v3356) = v3354
        assert(v3355.id == 1017)
        matchPostfixExpression(v3356)
      case 1025 =>
        val v3357 = v3353.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3358, v3359) = v3357
        assert(v3358.id == 1005)
        UnaryOp(UnaryOps.TILDE, matchUnaryExpression(v3359))(nextId(), v3353)
      case 1027 =>
        val v3360 = v3353.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3361, v3362) = v3360
        assert(v3361.id == 1005)
        UnaryOp(UnaryOps.NEG, matchUnaryExpression(v3362))(nextId(), v3353)
      case 1029 =>
        val v3363 = v3353.asInstanceOf[SequenceNode].children.head
        val BindNode(v3364, v3365) = v3363
        assert(v3364.id == 1030)
        matchCastExpression(v3365)
    }
    v3366
  }

  def matchUnqualifiedClassInstanceCreationExpression(node: Node): UnqualifiedClassInstanceCreationExpression = {
    val BindNode(v3367, v3368) = node
    val v3415 = v3367.id match {
      case 885 =>
        val v3369 = v3368.asInstanceOf[SequenceNode].children(1)
        val BindNode(v3370, v3371) = v3369
        assert(v3370.id == 510)
        val BindNode(v3372, v3373) = v3371
        val v3382 = v3372.id match {
          case 430 =>
            None
          case 511 =>
            val BindNode(v3374, v3375) = v3373
            val v3381 = v3374.id match {
              case 512 =>
                val BindNode(v3376, v3377) = v3375
                assert(v3376.id == 513)
                val v3378 = v3377.asInstanceOf[SequenceNode].children(1)
                val BindNode(v3379, v3380) = v3378
                assert(v3379.id == 514)
                matchTypeArguments(v3380)
            }
            Some(v3381)
        }
        val v3383 = v3368.asInstanceOf[SequenceNode].children(3)
        val BindNode(v3384, v3385) = v3383
        assert(v3384.id == 887)
        val v3387 = v3368.asInstanceOf[SequenceNode].children(6)
        val BindNode(v3388, v3389) = v3387
        assert(v3388.id == 896)
        val BindNode(v3390, v3391) = v3389
        val v3400 = v3390.id match {
          case 430 =>
            None
          case 897 =>
            val BindNode(v3392, v3393) = v3391
            val v3399 = v3392.id match {
              case 898 =>
                val BindNode(v3394, v3395) = v3393
                assert(v3394.id == 899)
                val v3396 = v3395.asInstanceOf[SequenceNode].children(1)
                val BindNode(v3397, v3398) = v3396
                assert(v3397.id == 900)
                matchArgumentList(v3398)
            }
            Some(v3399)
        }
        val v3386 = v3400
        val v3401 = v3368.asInstanceOf[SequenceNode].children(9)
        val BindNode(v3402, v3403) = v3401
        assert(v3402.id == 907)
        val BindNode(v3404, v3405) = v3403
        val v3414 = v3404.id match {
          case 430 =>
            None
          case 908 =>
            val BindNode(v3406, v3407) = v3405
            val v3413 = v3406.id match {
              case 909 =>
                val BindNode(v3408, v3409) = v3407
                assert(v3408.id == 910)
                val v3410 = v3409.asInstanceOf[SequenceNode].children(1)
                val BindNode(v3411, v3412) = v3410
                assert(v3411.id == 603)
                matchClassBody(v3412)
            }
            Some(v3413)
        }
        UnqualifiedClassInstanceCreationExpression(v3382, matchClassOrInterfaceTypeToInstantiate(v3385), if (v3386.isDefined) v3386.get else List(), v3414)(nextId(), v3368)
    }
    v3415
  }

  def matchUnqualifiedMethodIdentifier(node: Node): Identifier = {
    val BindNode(v3416, v3417) = node
    val v3423 = v3416.id match {
      case 948 =>
        val v3418 = v3417.asInstanceOf[SequenceNode].children.head
        val BindNode(v3419, v3420) = v3418
        assert(v3419.id == 949)
        val BindNode(v3421, v3422) = v3420
        assert(v3421.id == 168)
        matchIdentifier(v3422)
    }
    v3423
  }

  def matchVariableArityParameter(node: Node): VariableArityParameter = {
    val BindNode(v3424, v3425) = node
    val v3443 = v3424.id match {
      case 672 =>
        val v3426 = v3425.asInstanceOf[SequenceNode].children.head
        val v3427 = unrollRepeat0(v3426).map { elem =>
          val BindNode(v3428, v3429) = elem
          assert(v3428.id == 662)
          val BindNode(v3430, v3431) = v3429
          assert(v3430.id == 663)
          val BindNode(v3432, v3433) = v3431
          assert(v3432.id == 664)
          val v3434 = v3433.asInstanceOf[SequenceNode].children.head
          val BindNode(v3435, v3436) = v3434
          assert(v3435.id == 665)
          matchVariableModifier(v3436)
        }
        val v3437 = v3425.asInstanceOf[SequenceNode].children(1)
        val BindNode(v3438, v3439) = v3437
        assert(v3438.id == 626)
        val v3440 = v3425.asInstanceOf[SequenceNode].children(5)
        val BindNode(v3441, v3442) = v3440
        assert(v3441.id == 168)
        VariableArityParameter(v3427, matchType(v3439), matchIdentifier(v3442))(nextId(), v3425)
    }
    v3443
  }

  def matchVariableDeclarator(node: Node): VariableDeclarator = {
    val BindNode(v3444, v3445) = node
    val v3463 = v3444.id match {
      case 631 =>
        val v3446 = v3445.asInstanceOf[SequenceNode].children.head
        val BindNode(v3447, v3448) = v3446
        assert(v3447.id == 632)
        val v3449 = v3445.asInstanceOf[SequenceNode].children(1)
        val BindNode(v3450, v3451) = v3449
        assert(v3450.id == 638)
        val BindNode(v3452, v3453) = v3451
        val v3462 = v3452.id match {
          case 430 =>
            None
          case 639 =>
            val BindNode(v3454, v3455) = v3453
            val v3461 = v3454.id match {
              case 640 =>
                val BindNode(v3456, v3457) = v3455
                assert(v3456.id == 641)
                val v3458 = v3457.asInstanceOf[SequenceNode].children(3)
                val BindNode(v3459, v3460) = v3458
                assert(v3459.id == 642)
                matchVariableInitializer(v3460)
            }
            Some(v3461)
        }
        VariableDeclarator(matchVariableDeclaratorId(v3448), v3462)(nextId(), v3445)
    }
    v3463
  }

  def matchVariableDeclaratorId(node: Node): VariableDeclaratorId = {
    val BindNode(v3464, v3465) = node
    val v3483 = v3464.id match {
      case 633 =>
        val v3466 = v3465.asInstanceOf[SequenceNode].children.head
        val BindNode(v3467, v3468) = v3466
        assert(v3467.id == 168)
        val v3469 = v3465.asInstanceOf[SequenceNode].children(1)
        val BindNode(v3470, v3471) = v3469
        assert(v3470.id == 634)
        val BindNode(v3472, v3473) = v3471
        val v3482 = v3472.id match {
          case 430 =>
            None
          case 635 =>
            val BindNode(v3474, v3475) = v3473
            val v3481 = v3474.id match {
              case 636 =>
                val BindNode(v3476, v3477) = v3475
                assert(v3476.id == 637)
                val v3478 = v3477.asInstanceOf[SequenceNode].children(1)
                val BindNode(v3479, v3480) = v3478
                assert(v3479.id == 538)
                matchDims(v3480)
            }
            Some(v3481)
        }
        VariableDeclaratorId(matchIdentifier(v3468), v3482)(nextId(), v3465)
    }
    v3483
  }

  def matchVariableDeclaratorList(node: Node): List[VariableDeclarator] = {
    val BindNode(v3484, v3485) = node
    val v3501 = v3484.id match {
      case 629 =>
        val v3486 = v3485.asInstanceOf[SequenceNode].children.head
        val BindNode(v3487, v3488) = v3486
        assert(v3487.id == 630)
        val v3489 = v3485.asInstanceOf[SequenceNode].children(1)
        val v3490 = unrollRepeat0(v3489).map { elem =>
          val BindNode(v3491, v3492) = elem
          assert(v3491.id == 1223)
          val BindNode(v3493, v3494) = v3492
          val v3500 = v3493.id match {
            case 1224 =>
              val BindNode(v3495, v3496) = v3494
              assert(v3495.id == 1225)
              val v3497 = v3496.asInstanceOf[SequenceNode].children(3)
              val BindNode(v3498, v3499) = v3497
              assert(v3498.id == 630)
              matchVariableDeclarator(v3499)
          }
          v3500
        }
        List(matchVariableDeclarator(v3488)) ++ v3490
    }
    v3501
  }

  def matchVariableInitializer(node: Node): VariableInitializer = {
    val BindNode(v3502, v3503) = node
    val v3510 = v3502.id match {
      case 643 =>
        val v3504 = v3503.asInstanceOf[SequenceNode].children.head
        val BindNode(v3505, v3506) = v3504
        assert(v3505.id == 644)
        matchExpression(v3506)
      case 1220 =>
        val v3507 = v3503.asInstanceOf[SequenceNode].children.head
        val BindNode(v3508, v3509) = v3507
        assert(v3508.id == 923)
        matchArrayInitializer(v3509)
    }
    v3510
  }

  def matchVariableInitializerList(node: Node): List[VariableInitializer] = {
    val BindNode(v3511, v3512) = node
    val v3528 = v3511.id match {
      case 930 =>
        val v3513 = v3512.asInstanceOf[SequenceNode].children.head
        val BindNode(v3514, v3515) = v3513
        assert(v3514.id == 642)
        val v3516 = v3512.asInstanceOf[SequenceNode].children(1)
        val v3517 = unrollRepeat0(v3516).map { elem =>
          val BindNode(v3518, v3519) = elem
          assert(v3518.id == 933)
          val BindNode(v3520, v3521) = v3519
          val v3527 = v3520.id match {
            case 934 =>
              val BindNode(v3522, v3523) = v3521
              assert(v3522.id == 935)
              val v3524 = v3523.asInstanceOf[SequenceNode].children(3)
              val BindNode(v3525, v3526) = v3524
              assert(v3525.id == 642)
              matchVariableInitializer(v3526)
          }
          v3527
        }
        List(matchVariableInitializer(v3515)) ++ v3517
    }
    v3528
  }

  def matchVariableModifier(node: Node): VariableModifier.Value = {
    val BindNode(v3529, v3530) = node
    val v3531 = v3529.id match {
      case 666 =>
        VariableModifier.FINAL
    }
    v3531
  }

  def matchWhileStatement(node: Node): WhileStatement = {
    val BindNode(v3532, v3533) = node
    val v3540 = v3532.id match {
      case 1142 =>
        val v3534 = v3533.asInstanceOf[SequenceNode].children(4)
        val BindNode(v3535, v3536) = v3534
        assert(v3535.id == 644)
        val v3537 = v3533.asInstanceOf[SequenceNode].children(8)
        val BindNode(v3538, v3539) = v3537
        assert(v3538.id == 705)
        WhileStatement(matchExpression(v3536), matchStatement(v3539))(nextId(), v3533)
    }
    v3540
  }

  def matchWhileStatementNoShortIf(node: Node): WhileStatement = {
    val BindNode(v3541, v3542) = node
    val v3549 = v3541.id match {
      case 1113 =>
        val v3543 = v3542.asInstanceOf[SequenceNode].children(4)
        val BindNode(v3544, v3545) = v3543
        assert(v3544.id == 644)
        val v3546 = v3542.asInstanceOf[SequenceNode].children(8)
        val BindNode(v3547, v3548) = v3546
        assert(v3547.id == 1103)
        WhileStatement(matchExpression(v3545), matchStatementNoShortIf(v3548))(nextId(), v3542)
    }
    v3549
  }

  def matchWildcard(node: Node): Wildcard = {
    val BindNode(v3550, v3551) = node
    val v3566 = v3550.id match {
      case 550 =>
        val v3552 = v3551.asInstanceOf[SequenceNode].children(1)
        val BindNode(v3553, v3554) = v3552
        assert(v3553.id == 552)
        val BindNode(v3555, v3556) = v3554
        val v3565 = v3555.id match {
          case 430 =>
            None
          case 553 =>
            val BindNode(v3557, v3558) = v3556
            val v3564 = v3557.id match {
              case 554 =>
                val BindNode(v3559, v3560) = v3558
                assert(v3559.id == 555)
                val v3561 = v3560.asInstanceOf[SequenceNode].children(1)
                val BindNode(v3562, v3563) = v3561
                assert(v3562.id == 556)
                matchWildcardBounds(v3563)
            }
            Some(v3564)
        }
        Wildcard(v3565)(nextId(), v3551)
    }
    v3566
  }

  def matchWildcardBounds(node: Node): WildcardBounds = {
    val BindNode(v3567, v3568) = node
    val v3575 = v3567.id match {
      case 557 =>
        val v3569 = v3568.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3570, v3571) = v3569
        assert(v3570.id == 520)
        ExtendsBound(matchReferenceType(v3571))(nextId(), v3568)
      case 558 =>
        val v3572 = v3568.asInstanceOf[SequenceNode].children(2)
        val BindNode(v3573, v3574) = v3572
        assert(v3573.id == 520)
        SuperBound(matchReferenceType(v3574))(nextId(), v3568)
    }
    v3575
  }
}
