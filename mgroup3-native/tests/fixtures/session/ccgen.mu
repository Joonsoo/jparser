namespace mulang.cpp

// Annotation Processor for Mulang → C++ compilation
//
// 이 파일은 annotation processor의 전체적인 형태를 보여주는 설계 예시입니다.
// annotation processor는 Mu 언어로 작성되고, Mu 인터프리터에서 실행되며,
// 최종적으로 CppAst 데이터 구조를 생성합니다.

import mulang.ast.{MulangAst, AstNode, Expr}
import mulang.cpp_ast.CppAst.{
  CppFile, CppInclude, CppTopLevelDecl, CppStruct, CppClass, CppFunction,
  CppType, NamedType, PointerType, ReferenceType, VerbatimType,
  CppMember, FieldMember, MethodMember, ConstructorMember, DestructorMember,
  AccessSpecMember, NestedTypeDecl, AccessLevel, CppParam, CppBlock,
  CppStmt, VarDeclStmt, ReturnStmt, ExprStmt, IfStmt,
  CppExpr, VarRef, CallExpr, MemberAccess, BinaryOp, InitExpr,
  CppFuncQualifiers, CppInitializer, CppBase, CppTemplateParam,
  CppTemplateParamKind, TypeParamConstraint,
  StructDecl, ClassDecl, FunctionDecl, TypeAliasDecl, EnumDecl,
  TemplateDecl, NamespaceDecl, CppNamespace
}


// ============================================================
// 1. Compilation Context — annotation processor들이 공유하는 상태
// ============================================================

class CompilationContext {
  // 타입 이름 → 이미 생성된 C++ 선언 매핑
  typeRegistry: map<string, CppTopLevelDecl>
  // 현재 파일에 필요한 #include 목록
  includes: mut&list<CppInclude>
  // 현재 namespace 경로
  currentNamespace: list<string>
  // 사용자가 설정한 프로젝트 전역 옵션들
  options: CompilerOptions

  mut def requireInclude(path: string, isSystem: bool) {
    if !(includes.any { _.path == path }) {
      includes.append(CppInclude(path=path, isSystem=isSystem))
    }
  }

  def resolveType(name: string): optional<CppTopLevelDecl> =
    typeRegistry.get(name)
}

class CompilerOptions {
  // 기본 error handling 전략
  errorStrategy: ErrorStrategy
  // 기본 ownership 매핑
  defaultOwnership: OwnershipMapping
  // 생성할 C++ standard version
  cppStandard: CppStandard
}

enum ErrorStrategy {
  EXCEPTIONS          // C++ exceptions
  RESULT_TYPE         // std::expected<T, E> or custom Result
  STATUS_OR           // Google-style StatusOr<T>
}

enum OwnershipMapping {
  SMART_POINTERS      // unique_ptr / shared_ptr
  RAW_POINTERS        // manual memory management
  ARENA               // arena allocator
}

enum CppStandard {
  CPP17
  CPP20
  CPP23
}


// ============================================================
// 2. 기본 타입 매핑 (RefKind → C++ Type)
// ============================================================

// @cpp.TypeMapper — Mu 타입을 C++ 타입으로 변환하는 핵심 annotation processor
//
// 이 annotation은 모듈 레벨에서 사용되어 타입 매핑 규칙을 정의합니다.
// 기본 매핑이 있지만, 사용자가 override 할 수 있습니다.

def mapType(muType: MulangAst.Type, ctx: &CompilationContext): CppType = match muType {
  // own&T → std::unique_ptr<T>
  case is MulangAst.RefTypeWithAnnots {.kind .OWN, .typ as inner} ->
    ctx.requireInclude("memory", isSystem=true)
    NamedType(name="std::unique_ptr", templateArgs=[mapType(inner, ctx)])

  // arc&T → std::shared_ptr<T>
  case is MulangAst.RefTypeWithAnnots {.kind .ARC, .typ as inner} ->
    ctx.requireInclude("memory", isSystem=true)
    NamedType(name="std::shared_ptr", templateArgs=[mapType(inner, ctx)])

  // rc&T → 비-atomic shared_ptr (프로젝트별 구현)
  case is MulangAst.RefTypeWithAnnots {.kind .RC, .typ as inner} ->
    NamedType(name="rc_ptr", templateArgs=[mapType(inner, ctx)])

  // weak&T → std::weak_ptr<T>
  case is MulangAst.RefTypeWithAnnots {.kind .WEAK, .typ as inner} ->
    ctx.requireInclude("memory", isSystem=true)
    NamedType(name="std::weak_ptr", templateArgs=[mapType(inner, ctx)])

  // mut&T → T& (mutable reference)
  case is MulangAst.RefTypeWithAnnots {.kind .MUT, .typ as inner} ->
    ReferenceType(inner=mapType(inner, ctx), isConst=false)

  // &T → const T& (immutable reference, kind가 없는 경우)
  case is MulangAst.RefTypeWithAnnots {.kind .none, .typ as inner} ->
    ReferenceType(inner=mapType(inner, ctx), isConst=true)

  // *T → 스택 할당 T
  case is MulangAst.StackType {.typ as inner} ->
    mapType(inner, ctx)

  // (A, B, C) → std::tuple<A, B, C>
  case is MulangAst.TupleType {.elems as elems} ->
    ctx.requireInclude("tuple", isSystem=true)
    NamedType(
      name="std::tuple",
      templateArgs=elems.map { mapType(_, ctx) }
    )

  // (A) -> B → std::function<B(A)>
  case is MulangAst.FunctionType {.params as params, .retType as ret} ->
    ctx.requireInclude("functional", isSystem=true)
    NamedType(
      name="std::function",
      templateArgs=[FunctionType(
        params=params.map { mapType(_, ctx) },
        ret=mapType(ret, ctx)
      )]
    )

  // 기본 타입 이름 매핑
  case is MulangAst.TypeSecondaryWithAnnots {.typ is MulangAst.TypeNameTok {.name as name}} ->
    mapPrimitiveType(name, ctx)

  // 제네릭 타입: vector<T> → std::vector<T>
  case is MulangAst.TypeSecondaryWithAnnots {.typ is MulangAst.TypeWithArgs {.base as base, .args as args}} ->
    let mappedArgs = args.map { arg -> match arg {
      case is MulangAst.Type as t -> mapType(t, ctx)
      else -> VerbatimType(code="/* unknown type arg */")
    }}
    mapGenericType(base.name, mappedArgs, ctx)

  else ->
    VerbatimType(code="/* unmapped type */")
}

def mapPrimitiveType(name: string, ctx: &CompilationContext): CppType = match name {
  case "i8"     -> NamedType(name="int8_t", templateArgs=[])
  case "i16"    -> NamedType(name="int16_t", templateArgs=[])
  case "i32"    -> NamedType(name="int32_t", templateArgs=[])
  case "i64"    -> NamedType(name="int64_t", templateArgs=[])
  case "u8"     -> NamedType(name="uint8_t", templateArgs=[])
  case "u16"    -> NamedType(name="uint16_t", templateArgs=[])
  case "u32"    -> NamedType(name="uint32_t", templateArgs=[])
  case "u64"    -> NamedType(name="uint64_t", templateArgs=[])
  case "f32"    -> NamedType(name="float", templateArgs=[])
  case "f64"    -> NamedType(name="double", templateArgs=[])
  case "bool"   -> NamedType(name="bool", templateArgs=[])
  case "string" ->
    ctx.requireInclude("string", isSystem=true)
    NamedType(name="std::string", templateArgs=[])
  else ->
    NamedType(name=name, templateArgs=[])
}

def mapGenericType(name: string, args: list<CppType>, ctx: &CompilationContext): CppType = match name {
  case "list" ->
    ctx.requireInclude("vector", isSystem=true)
    NamedType(name="std::vector", templateArgs=args)
  case "map" ->
    ctx.requireInclude("unordered_map", isSystem=true)
    NamedType(name="std::unordered_map", templateArgs=args)
  case "set" ->
    ctx.requireInclude("unordered_set", isSystem=true)
    NamedType(name="std::unordered_set", templateArgs=args)
  case "optional" ->
    ctx.requireInclude("optional", isSystem=true)
    NamedType(name="std::optional", templateArgs=args)
  case "span" ->
    ctx.requireInclude("span", isSystem=true)
    NamedType(name="std::span", templateArgs=args)
  else ->
    NamedType(name=name, templateArgs=args)
}


// ============================================================
// 3. Class → C++ Struct/Class 변환
// ============================================================

def processClassDef(
  classDef: MulangAst.ClassDef,
  annots: optional<MulangAst.Annots>,
  ctx: mut&CompilationContext
): CppTopLevelDecl {
  let cppName = classDef.name.name
  let templateParams = mapTypeParams(classDef.typeParams, ctx)
  let bases = mapBases(classDef.extends, ctx)
  let members = list<CppMember>()

  // 필드 처리
  match classDef.body {
    case .some(body) ->
      for member in body.members {
        match member {
          case is MulangAst.FieldDef as field ->
            members.append(processField(field, ctx))
          case is MulangAst.FuncDef as func ->
            members.append(processMethod(func, ctx))
          case is MulangAst.PropFuncDef as prop ->
            members.append(processProperty(prop, ctx))
          case is MulangAst.ClassCtorDef as ctor ->
            members.append(processConstructor(ctor, classDef, ctx))
          else -> {}
        }
      }
    case .none -> {}
  }

  // value class → struct (public by default, no virtual)
  let decl = if classDef.isValue then {
    StructDecl(body=CppStruct(
      name=cppName,
      templateParams=templateParams,
      bases=bases,
      members=members
    ))
  } else {
    // 일반 class → C++ class + virtual destructor
    let allMembers = list<CppMember>()
    allMembers.append(AccessSpecMember(level=.PUBLIC))
    allMembers.appendAll(members)

    // virtual destructor 추가 (상속 가능한 클래스)
    if bases.isEmpty {
      allMembers.append(DestructorMember(
        access=.PUBLIC,
        isVirtual=true,
        body=.none  // = default
      ))
    }

    ClassDecl(body=CppClass(
      name=cppName,
      templateParams=templateParams,
      bases=bases,
      members=allMembers
    ))
  }

  // 템플릿이 있으면 TemplateDecl로 감싸기
  if templateParams.isEmpty then {
    decl
  } else {
    TemplateDecl(params=templateParams, inner=decl)
  }
}

def processField(
  field: MulangAst.FieldDef,
  ctx: &CompilationContext
): CppMember {
  let access = mapAccessModifier(field.accessMods)
  let cppType = mapType(field.typ, ctx)
  let defaultVal = field.defaultValue.map { processExpr(_, ctx) }

  FieldMember(
    name=field.name.name,
    type=cppType,
    defaultValue=defaultVal,
    access=access,
    isStatic=false,
    isMutable=false
  )
}

def processMethod(
  funcDef: MulangAst.FuncDef,
  ctx: &CompilationContext
): CppMember {
  let sig = funcDef.sig
  let cppFunc = processFuncSig(sig, funcDef.body, ctx)

  MethodMember(
    func=cppFunc,
    access=mapAccessModifier(sig.accessMods),
    isVirtual=false,
    isPureVirtual=match funcDef.body {
      // body가 없으면 pure virtual
      case .none -> true
      case .some -> false
    },
    isOverride=sig.mods.isOverride,
    isFinal=false,
    isStatic=false
  )
}


// ============================================================
// 4. oneof → C++ Sum Type 변환
//    annotation에 따라 std::variant / 상속 / tagged union 중 선택
// ============================================================

// @cpp.Layout("variant") — std::variant 기반
// @cpp.Layout("virtual") — 가상 상속 기반
// @cpp.Layout("tagged_union") — C-style tagged union

def processOneofDef(
  oneofDef: MulangAst.OneofDef,
  annots: optional<MulangAst.Annots>,
  ctx: mut&CompilationContext
): list<CppTopLevelDecl> {
  let layout = findAnnotValue(annots, "cpp", "Layout").getOrElse("variant")

  match layout {
    case "variant"      -> processOneofAsVariant(oneofDef, ctx)
    case "virtual"      -> processOneofAsVirtual(oneofDef, ctx)
    case "tagged_union" -> processOneofAsTaggedUnion(oneofDef, ctx)
    else -> processOneofAsVariant(oneofDef, ctx)
  }
}

// --- variant 방식 ---
// oneof Message { quit; write: string; move(i32, i32) }
// →
// struct Message_Quit {};
// struct Message_Write { std::string value; };
// struct Message_Move { int32_t _0; int32_t _1; };
// using Message = std::variant<Message_Quit, Message_Write, Message_Move>;

def processOneofAsVariant(
  oneofDef: MulangAst.OneofDef,
  ctx: mut&CompilationContext
): list<CppTopLevelDecl> {
  ctx.requireInclude("variant", isSystem=true)

  let baseName = oneofDef.name.name
  let decls = list<CppTopLevelDecl>()
  let variantTypes = list<CppType>()

  for member in oneofDef.body.members {
    match member {
      case is MulangAst.OneofFieldDef {.body as body} -> match body {
        // quit → struct Message_Quit {}
        case is MulangAst.OneofNameOnlyField {.name as name} ->
          let structName = "${baseName}_${capitalize(name.name)}"
          decls.append(StructDecl(body=CppStruct(
            name=structName,
            templateParams=[],
            bases=[],
            members=[]
          )))
          variantTypes.append(NamedType(name=structName, templateArgs=[]))

        // write: string → struct Message_Write { std::string value; }
        case is MulangAst.OneofFullField {.name as name, .typ as typ} ->
          let structName = "${baseName}_${capitalize(name.name)}"
          decls.append(StructDecl(body=CppStruct(
            name=structName,
            templateParams=[],
            bases=[],
            members=[FieldMember(
              name="value",
              type=mapType(typ, ctx),
              defaultValue=.none,
              access=.PUBLIC,
              isStatic=false,
              isMutable=false
            )]
          )))
          variantTypes.append(NamedType(name=structName, templateArgs=[]))

        // move(i32, i32) → struct Message_Move { int32_t _0; int32_t _1; }
        case is MulangAst.OneofTupleField {.name as name, .typ as tupleType} ->
          let structName = "${baseName}_${capitalize(name.name)}"
          let fields = tupleType.elems.mapWithIndex { idx, elem ->
            FieldMember(
              name="_$idx",
              type=mapType(elem, ctx),
              defaultValue=.none,
              access=.PUBLIC,
              isStatic=false,
              isMutable=false
            )
          }
          decls.append(StructDecl(body=CppStruct(
            name=structName,
            templateParams=[],
            bases=[],
            members=fields
          )))
          variantTypes.append(NamedType(name=structName, templateArgs=[]))
        else -> {}
      }
      else -> {}
    }
  }

  // using Message = std::variant<...>
  decls.append(TypeAliasDecl(
    name=baseName,
    type=NamedType(name="std::variant", templateArgs=variantTypes)
  ))

  decls
}

// --- virtual 방식 ---
// oneof Message { quit; write: string; move(i32, i32) }
// →
// class Message { public: virtual ~Message() = default; };
// class Message_Quit : public Message {};
// class Message_Write : public Message { public: std::string value; };
// class Message_Move : public Message { public: int32_t _0; int32_t _1; };

def processOneofAsVirtual(
  oneofDef: MulangAst.OneofDef,
  ctx: mut&CompilationContext
): list<CppTopLevelDecl> {
  let baseName = oneofDef.name.name
  let decls = list<CppTopLevelDecl>()

  // Base class
  decls.append(ClassDecl(body=CppClass(
    name=baseName,
    templateParams=[],
    bases=[],
    members=[
      AccessSpecMember(level=.PUBLIC),
      DestructorMember(access=.PUBLIC, isVirtual=true, body=.none)
    ]
  )))

  // 각 variant를 subclass로
  let baseType = NamedType(name=baseName, templateArgs=[])
  for member in oneofDef.body.members {
    match member {
      case is MulangAst.OneofFieldDef {.body as body} -> match body {
        case is MulangAst.OneofNameOnlyField {.name as name} ->
          decls.append(ClassDecl(body=CppClass(
            name="${baseName}_${capitalize(name.name)}",
            templateParams=[],
            bases=[CppBase(type=baseType, access=.PUBLIC, isVirtual=false)],
            members=[]
          )))

        case is MulangAst.OneofFullField {.name as name, .typ as typ} ->
          decls.append(ClassDecl(body=CppClass(
            name="${baseName}_${capitalize(name.name)}",
            templateParams=[],
            bases=[CppBase(type=baseType, access=.PUBLIC, isVirtual=false)],
            members=[
              AccessSpecMember(level=.PUBLIC),
              FieldMember(
                name="value",
                type=mapType(typ, ctx),
                defaultValue=.none,
                access=.PUBLIC,
                isStatic=false,
                isMutable=false
              )
            ]
          )))

        case is MulangAst.OneofTupleField {.name as name, .typ as tupleType} ->
          let fields = tupleType.elems.mapWithIndex { idx, elem ->
            FieldMember(
              name="_$idx",
              type=mapType(elem, ctx),
              defaultValue=.none,
              access=.PUBLIC,
              isStatic=false,
              isMutable=false
            )
          }
          decls.append(ClassDecl(body=CppClass(
            name="${baseName}_${capitalize(name.name)}",
            templateParams=[],
            bases=[CppBase(type=baseType, access=.PUBLIC, isVirtual=false)],
            members=[AccessSpecMember(level=.PUBLIC)] ++ fields
          )))
        else -> {}
      }
      else -> {}
    }
  }

  decls
}


// ============================================================
// 5. enum → C++ enum class 변환
// ============================================================

def processEnumDef(
  enumDef: MulangAst.EnumDef,
  ctx: &CompilationContext
): CppTopLevelDecl {
  let valueType = enumDef.valueType.map { mapPrimitiveType(_.name, ctx) }
  let members = enumDef.body.map { m ->
    CppEnumMember(
      name=m.name.name,
      value=m.intValue.map { IntLit(value=parseInt(_.digits), suffix="") }
    )
  }

  EnumDecl(
    name=enumDef.name.name,
    isClass=true,
    valueType=valueType,
    members=members
  )
}


// ============================================================
// 6. sealed trait → C++ abstract base class 변환
// ============================================================

def processSealedTrait(
  traitDef: MulangAst.TraitDef,
  ctx: mut&CompilationContext
): CppTopLevelDecl {
  let baseName = traitDef.name.name
  let templateParams = mapTypeParams(traitDef.typeParams, ctx)
  let members = list<CppMember>()

  members.append(AccessSpecMember(level=.PUBLIC))

  // virtual destructor
  members.append(DestructorMember(
    access=.PUBLIC,
    isVirtual=true,
    body=.none
  ))

  // trait의 abstract 메서드들 → pure virtual
  match traitDef.body {
    case .some(body) ->
      for member in body.members {
        match member {
          case is MulangAst.FuncDef as func ->
            members.append(MethodMember(
              func=processFuncSig(func.sig, func.body, ctx),
              access=.PUBLIC,
              isVirtual=true,
              isPureVirtual=match func.body {
                case .none -> true
                case .some -> false
              },
              isOverride=false,
              isFinal=false,
              isStatic=false
            ))
          case is MulangAst.PropFuncDef as prop ->
            members.append(processProperty(prop, ctx))
          else -> {}
        }
      }
    case .none -> {}
  }

  let decl = ClassDecl(body=CppClass(
    name=baseName,
    templateParams=templateParams,
    bases=mapBases(traitDef.extends, ctx),
    members=members
  ))

  if templateParams.isEmpty {
    decl
  } else {
    TemplateDecl(params=templateParams, inner=decl)
  }
}


// ============================================================
// 7. 함수 / 표현식 변환
// ============================================================

def processFuncSig(
  sig: MulangAst.FuncSig,
  body: optional<MulangAst.FuncBody>,
  ctx: &CompilationContext
): CppFunction {
  let params = sig.params.params.map { p ->
    CppParam(
      name=p.name.name,
      type=mapType(p.typ, ctx),
      defaultValue=p.defaultValue.map { processExpr(_, ctx) }
    )
  }

  let retType = sig.retType.map { mapType(_, ctx) }.getOrElse(
    VerbatimType(code="void")
  )

  CppFunction(
    name=sig.name.name,
    templateParams=mapTypeParams(sig.typeParams, ctx),
    params=params,
    retType=retType,
    qualifiers=CppFuncQualifiers(
      isConst=!sig.mods.isMut,  // Mu: mut → C++: non-const; 기본 = const
      isNoexcept=false,
      isInline=sig.mods.isInline,
      isStatic=false,
      isExplicit=false,
      isConstexpr=false,
      isNodiscard=false
    ),
    body=body.map { b -> match b {
      case is MulangAst.Block as block -> processBlock(block, ctx)
      // 식 본문: = expr → { return expr; }
      case is MulangAst.Expr as expr ->
        CppBlock(stmts=[ReturnStmt(value=processExpr(expr, ctx))])
    }}
  )
}

def processBlock(block: MulangAst.Block, ctx: &CompilationContext): CppBlock {
  let stmts = list<CppStmt>()

  for stmt in block.stmts {
    stmts.appendAll(processStmt(stmt, ctx))
  }

  CppBlock(stmts=stmts)
}

def processStmt(stmt: MulangAst.Stmt, ctx: &CompilationContext): list<CppStmt> =
  match stmt {
    case is MulangAst.LetStmtWithValue {.name as name, .typ as typ, .value as value} ->
      [VarDeclStmt(
        name=name.name,
        type=typ.map { mapType(_, ctx) },
        init=processExpr(value, ctx),
        isConst=true,     // let → const
        isConstexpr=false,
        isRef=false
      )]

    case is MulangAst.Return {.value as value} ->
      [ReturnStmt(value=value.map { processExpr(_, ctx) })]

    case is MulangAst.IfExpr as ifExpr ->
      [processIfExpr(ifExpr, ctx)]

    case is MulangAst.ForStmt {.iter as iter, .coll as coll, .body as body} ->
      [RangeForStmt(
        var_=VarDeclStmt(
          name=match iter {
            case is MulangAst.GenericLambdaParam {.name as n} -> n.name
            else -> "_"
          },
          type=.none,  // auto
          init=.none,
          isConst=true,
          isConstexpr=false,
          isRef=true  // const auto&
        ),
        range=processExpr(coll, ctx),
        body=processBlock(body, ctx)
      )]

    case is MulangAst.WhileStmt {.cond as cond, .body as body} ->
      [WhileStmt(
        cond=processExpr(cond, ctx),
        body=processBlock(body, ctx)
      )]

    case is MulangAst.Expr as expr ->
      [ExprStmt(expr=processExpr(expr, ctx))]

    else ->
      [VerbatimStmt(code="/* unprocessed stmt */")]
  }

def processExpr(expr: MulangAst.Expr, ctx: &CompilationContext): CppExpr =
  match expr {
    case is MulangAst.IntLiteral {.digits as digits} ->
      IntLit(value=parseInt(digits), suffix="")

    case is MulangAst.BoolLiteral {.value as v} ->
      BoolLit(value=v)

    case is MulangAst.StringExpr {.elems as elems} ->
      processStringExpr(elems, ctx)

    case is MulangAst.ExprNameTok {.name as name} ->
      VarRef(name=name)

    case is MulangAst.BinaryOp {.op as op, .lhs as lhs, .rhs as rhs} ->
      BinaryOp(op=op, lhs=processExpr(lhs, ctx), rhs=processExpr(rhs, ctx))

    case is MulangAst.CallChainExpr {.callee as callee, .chain as chain} ->
      processCallChain(callee, chain, ctx)

    case is MulangAst.MoveExpr {.expr as inner} ->
      // ^x → std::move(x)
      ctx.requireInclude("utility", isSystem=true)
      CallExpr(
        callee=VarRef(name="std::move"),
        args=[processExpr(inner, ctx)],
        templateArgs=[]
      )

    case is MulangAst.BorrowExpr {.expr as inner} ->
      // &x → 그대로 (C++에서는 reference passing이 자동)
      processExpr(inner, ctx)

    case is MulangAst.MatchValueExpr as matchExpr ->
      processMatchExpr(matchExpr, ctx)

    case is MulangAst.IfExpr as ifExpr ->
      // if expression → ternary (단순 경우) 또는 즉시 실행 lambda
      processIfAsExpr(ifExpr, ctx)

    case is MulangAst.ListExpr {.elems as elems} ->
      // [a, b, c] → std::vector<T>{a, b, c}  (타입 추론 필요)
      ctx.requireInclude("vector", isSystem=true)
      InitExpr(
        type=NamedType(name="std::vector", templateArgs=[VerbatimType(code="auto")]),
        args=elems.map { processExpr(_, ctx) },
        isBrace=true
      )

    case is MulangAst.TupleExpr {.elems as elems} ->
      // (a, b) → std::make_tuple(a, b)
      ctx.requireInclude("tuple", isSystem=true)
      CallExpr(
        callee=VarRef(name="std::make_tuple"),
        args=elems.map { processExpr(_, ctx) },
        templateArgs=[]
      )

    else ->
      VerbatimExpr(code="/* unprocessed expr */")
  }


// ============================================================
// 8. Match → std::visit / if-else chain 변환
// ============================================================

// oneof에 대한 match → std::visit + overloaded lambda pattern
//
// match msg {
//   case .quit -> handleQuit()
//   case .write as w -> println(w)
//   case .move(x, y) -> moveTo(x, y)
// }
// →
// std::visit(overloaded{
//   [](const Message_Quit&) { handleQuit(); },
//   [](const Message_Write& w) { println(w.value); },
//   [](const Message_Move& m) { moveTo(m._0, m._1); },
// }, msg);

def processMatchExpr(
  matchExpr: MulangAst.MatchValueExpr,
  ctx: &CompilationContext
): CppExpr {
  let scrutinee = processExpr(matchExpr.value, ctx)

  // TODO: 타입 정보를 기반으로 variant match인지 일반 값 match인지 판별
  // 여기서는 일반 값 match → if-else chain으로 변환하는 예시

  // 즉시 실행 lambda로 감싸기: [&]() { if (...) ... }()
  let stmts = list<CppStmt>()
  let tempVar = "_match_val"
  stmts.append(VarDeclStmt(
    name=tempVar,
    type=.none,
    init=.some(scrutinee),
    isConst=true,
    isConstexpr=false,
    isRef=true
  ))

  // 각 case를 if-else chain으로
  let cases = matchExpr.body.cases
  for i in 0..<cases.size {
    let c = cases[i]
    // 간소화: pattern을 조건식으로 변환
    let cond = patternToCondition(c.pattern, VarRef(name=tempVar), ctx)
    let body = processMatchHandler(c.handler, ctx)

    if i == 0 {
      stmts.append(IfStmt(init=.none, cond=cond, thn=body, els=.none))
    } else {
      // else if chain은 중첩 IfStmt로
      stmts.append(IfStmt(init=.none, cond=cond, thn=body, els=.none))
    }
  }

  // else case
  match matchExpr.body.elseCase {
    case .some(elseCase) ->
      stmts.append(BlockStmt(block=processMatchHandler(elseCase, ctx)))
    case .none -> {}
  }

  // [&]() { ... }()
  CallExpr(
    callee=LambdaExpr(
      captures=[CppCapture(kind=.defaultByRef, name=.none)],
      params=[],
      retType=.none,
      body=CppBlock(stmts=stmts),
      isMutable=false
    ),
    args=[],
    templateArgs=[]
  )
}


// ============================================================
// 9. Error Handling 변환 — annotation에 따라 전략 선택
// ============================================================

// @cpp.ErrorHandling("exceptions") — 기본, try/catch 그대로
// @cpp.ErrorHandling("result")     — std::expected<T, E> 반환
// @cpp.ErrorHandling("status_or")  — StatusOr<T> 반환

def processTryCatch(
  tryExpr: MulangAst.TryCatchExpr,
  strategy: ErrorStrategy,
  ctx: &CompilationContext
): list<CppStmt> = match strategy {

  case .EXCEPTIONS ->
    // try { ... } catch { case .eofError -> ... }
    // → try { ... } catch (const EofError& e) { ... }
    let tryBlock = processBlock(tryExpr.tryBlock, ctx)
    let handlers = tryExpr.catchBody.map { catchBody ->
      catchBody.cases.map { c ->
        CppCatchHandler(
          param=patternToCatchParam(c.pattern, ctx),
          body=processMatchHandler(c.handler, ctx)
        )
      }
    }.getOrElse([])

    let finallyBlock = tryExpr.finallyBlock.map { processBlock(_, ctx) }

    // C++에는 finally가 없으므로 RAII guard로 변환
    match finallyBlock {
      case .some(fb) ->
        let guardStmts = list<CppStmt>()
        guardStmts.append(VerbatimStmt(
          code="auto _finally_guard = scope_exit([&]() ${finallyToString(fb)});"
        ))
        guardStmts.append(TryCatchStmt(body=tryBlock, handlers=handlers, finally_=.none))
        guardStmts
      case .none ->
        [TryCatchStmt(body=tryBlock, handlers=handlers, finally_=.none)]
    }

  case .RESULT_TYPE ->
    // try { val } catch { case .err -> fallback }
    // → auto result = someFunc(); if (!result) { /* handle error */ }
    // 이 변환은 호출하는 함수의 시그니처도 바꿔야 하므로 더 복잡
    [VerbatimStmt(code="/* result-type error handling - requires whole-function transform */")]

  case .STATUS_OR ->
    [VerbatimStmt(code="/* status-or error handling */")]
}


// ============================================================
// 10. Top-level Driver — CompileUnit → CppFile 변환
// ============================================================

def compile(source: string, ctx: mut&CompilationContext): CppFile {
  let ast = parseMulang(source)  // MulangAst parser 호출

  let topLevelDecls = list<CppTopLevelDecl>()

  for elem in ast.elems {
    let decls = processTopLevelDef(elem, ctx)
    topLevelDecls.appendAll(decls)
  }

  // namespace 감싸기
  let finalDecls = match ast.namespace {
    case .some(ns) ->
      let nsPath = ns.namespace.map { _.name }
      wrapInNamespaces(nsPath, topLevelDecls)
    case .none ->
      topLevelDecls
  }

  CppFile(
    includes=ctx.includes.toList(),
    topLevel=finalDecls
  )
}

def processTopLevelDef(
  elem: MulangAst.TopLevelDef,
  ctx: mut&CompilationContext
): list<CppTopLevelDecl> = match elem {

  case is MulangAst.ClassDefWithAnnots {.annots as annots, .classDef as classDef} ->
    [processClassDef(classDef, annots, ctx)]

  case is MulangAst.TraitDefWithAnnots {.annots as annots, .traitDef as traitDef} ->
    if traitDef.isSealed {
      [processSealedTrait(traitDef, ctx)]
    } else {
      [processTrait(traitDef, ctx)]
    }

  case is MulangAst.OneofDefWithAnnots {.annots as annots, .oneofDef as oneofDef} ->
    processOneofDef(oneofDef, annots, ctx)

  case is MulangAst.EnumDefWithAnnots {.annots as annots, .enumDef as enumDef} ->
    [processEnumDef(enumDef, ctx)]

  case is MulangAst.FuncDef as funcDef ->
    [FunctionDecl(func=processFuncSig(funcDef.sig, funcDef.body, ctx))]

  case is MulangAst.ExtendDefWithAnnots {.annots as annots, .extendDef as extendDef} ->
    processExtendDef(extendDef, annots, ctx)

  case is MulangAst.NamespaceDef {.namespace as ns, .elems as elems} ->
    let innerDecls = elems.flatMap { processTopLevelDef(_, ctx) }
    [NamespaceDecl(ns=CppNamespace(
      name=ns.map { _.name }.join("::"),
      members=innerDecls
    ))]

  case is MulangAst.TypeAliasDef {.name as name, .typ as typ} ->
    [TypeAliasDecl(name=name.name, type=mapType(typ, ctx))]

  else -> []
}


// ============================================================
// 11. extend → C++ method 주입
//     Mu의 extend는 별도의 블록이지만 C++에서는 클래스 안의 메서드가 됨
// ============================================================

def processExtendDef(
  extendDef: MulangAst.ExtendDef,
  annots: optional<MulangAst.Annots>,
  ctx: mut&CompilationContext
): list<CppTopLevelDecl> {
  // extend<T> World<T> for Hello<T> → Hello<T> 클래스에 메서드 추가
  // C++에서는 class 밖에서 정의하는 형태로 생성

  let targetName = extendDef.targetClass.clsName.name
  let methods = list<CppTopLevelDecl>()

  match extendDef.body {
    case .some(body) ->
      for member in body.members {
        match member {
          case is MulangAst.FuncDef as func ->
            let cppFunc = processFuncSig(func.sig, func.body, ctx)
            // className::methodName 형태
            let qualifiedFunc = CppFunction(
              name="${targetName}::${cppFunc.name}",
              templateParams=cppFunc.templateParams,
              params=cppFunc.params,
              retType=cppFunc.retType,
              qualifiers=cppFunc.qualifiers,
              body=cppFunc.body
            )
            methods.append(FunctionDecl(func=qualifiedFunc))
          else -> {}
        }
      }
    case .none -> {}
  }

  methods
}


// ============================================================
// 12. C++ 코드 출력 (CppAst → string)
// ============================================================

def emitCppFile(file: CppFile): string {
  let out = StringBuilder()

  // #pragma once
  out.appendLine("#pragma once")
  out.appendLine()

  // #include
  for inc in file.includes {
    if inc.isSystem {
      out.appendLine("#include <${inc.path}>")
    } else {
      out.appendLine("#include \"${inc.path}\"")
    }
  }
  out.appendLine()

  // top-level declarations
  for decl in file.topLevel {
    emitTopLevelDecl(decl, out, indent=0)
    out.appendLine()
  }

  out.toString()
}

def emitTopLevelDecl(decl: CppTopLevelDecl, out: mut&StringBuilder, indent: i32) {
  let pad = " ".repeat(indent)
  match decl {
    case is NamespaceDecl {.ns as ns} ->
      out.appendLine("${pad}namespace ${ns.name} {")
      for member in ns.members {
        emitTopLevelDecl(member, out, indent=indent + 2)
      }
      out.appendLine("${pad}} // namespace ${ns.name}")

    case is StructDecl {.body as body} ->
      out.appendLine("${pad}struct ${body.name} {")
      emitMembers(body.members, out, indent=indent + 2)
      out.appendLine("${pad}};")

    case is ClassDecl {.body as body} ->
      out.append("${pad}class ${body.name}")
      if !body.bases.isEmpty {
        let basesStr = body.bases.map { b ->
          let access = match b.access {
            case .PUBLIC -> "public"
            case .PROTECTED -> "protected"
            case .PRIVATE -> "private"
          }
          "$access ${emitType(b.type)}"
        }.join(", ")
        out.append(" : $basesStr")
      }
      out.appendLine(" {")
      emitMembers(body.members, out, indent=indent + 2)
      out.appendLine("${pad}};")

    case is FunctionDecl {.func as func} ->
      emitFunction(func, out, indent)

    case is TypeAliasDecl {.name as name, .type as typ} ->
      out.appendLine("${pad}using $name = ${emitType(typ)};")

    case is EnumDecl as e ->
      let classStr = if e.isClass then " class" else ""
      let typeStr = e.valueType.map { " : ${emitType(_)}" }.getOrElse("")
      out.appendLine("${pad}enum${classStr} ${e.name}${typeStr} {")
      for m in e.members {
        let valStr = m.value.map { " = ${emitExpr(_)}" }.getOrElse("")
        out.appendLine("${pad}  ${m.name}${valStr},")
      }
      out.appendLine("${pad}};")

    case is TemplateDecl {.params as params, .inner as inner} ->
      let paramsStr = params.map { p -> match p.kind {
        case .typeParam -> "typename ${p.name}"
        case .nonTypeParam as t -> "${emitType(t)} ${p.name}"
      }}.join(", ")
      out.appendLine("${pad}template<$paramsStr>")
      emitTopLevelDecl(inner, out, indent)

    case is VerbatimDecl {.code as code} ->
      out.appendLine("${pad}$code")

    else -> {}
  }
}


// ============================================================
// Helper Functions
// ============================================================

def mapAccessModifier(mod: optional<MulangAst.AccessModifier>): AccessLevel =
  match mod {
    case .none            -> .PUBLIC
    case .some(.PRIVATE)  -> .PRIVATE
    else                  -> .PUBLIC
  }

def mapTypeParams(
  typeParams: optional<MulangAst.TypeParams>,
  ctx: &CompilationContext
): list<CppTemplateParam> {
  let typeParams = typeParams.getOr! { return [] }
  return typeParams.params.map { tp ->
    CppTemplateParam(
      name=tp.name.name,
      kind=.typeParam(TypeParamConstraint(concept_=.none))
    )
  }
}

def mapBases(
  extends: optional<list<MulangAst.ClassType>>,
  ctx: &CompilationContext
): list<CppBase> {
  let extends = extends.getOr! { return [] }
  return extends.map { base ->
    let baseType = NamedType(
      name=base.clsName.name,
      templateArgs=base.typeArgs.map { args ->
        args.map { arg -> match arg {
          case is MulangAst.Type as t -> mapType(t, ctx)
          else -> VerbatimType(code="auto")
        }}
      }.getOrElse([])
    )
    CppBase(type=baseType, access=.PUBLIC, isVirtual=false)
  }
}

def wrapInNamespaces(
  path: list<string>,
  decls: list<CppTopLevelDecl>
): list<CppTopLevelDecl> {
  if path.isEmpty { return decls }
  // mulang.foo.bar → namespace mulang::foo::bar { ... }
  return [NamespaceDecl(ns=CppNamespace(
    name=path.join("::"),
    members=decls
  ))]
}

def findAnnotValue(
  annots: optional<MulangAst.Annots>,
  namespace: string,
  name: string
): optional<string> {
  let annots = annots.getOr! { return .none }
  for annot in annots.annots {
    if annot.name.name == name {
      // 첫 번째 positional arg를 string으로 해석
      match annot.args {
        case .some(args) if !args.args.isEmpty ->
          match args.args[0] {
            case is MulangAst.StringExpr {.elems [is MulangAst.CharsChunk {.value as v}]} ->
              return .some(v)
            else -> {}
          }
        else -> {}
      }
    }
  }
  .none
}

def capitalize(s: string): string =
  if s.isEmpty then s
  else s[0].toUpper() + s.substring(1)
