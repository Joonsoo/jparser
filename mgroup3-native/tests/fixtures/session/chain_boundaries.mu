// CallChain 경계 사례 코퍼스 — <CallChain_+> longest 제거(1a)가 언어를 바꾸지 않음을
// 차등 테스트로 검증하기 위한 코퍼스. 여기의 모든 형태는 *현행* 문법에서 유효하게
// 파싱되어야 한다. 각 케이스에 의도를 한 줄 주석으로 남긴다.

// ── 1. 같은 줄 다단 체인 (()·[]·트레일링 람다 조합) ──
def sameLineChains() {
  f(a)(b)              // 연속 () 호출 두 단
  f(a) (b)             // 사이에 공백 (WS_NO_NL) — 같은 줄이므로 여전히 한 체인
  f(a)[i](b)           // () → [] 첨자 → () 혼합
  f[i][j](a)           // 첨자 두 단 뒤 호출
  f(a)[i]              // 호출 뒤 첨자로 끝
  f[i]                 // BaseCallee(f) + 첨자만 (SubscribeAccess 경로)
  f(a).g(b)            // () 뒤 .member 뒤 ()
  f(a).g[i](b)         // .member 뒤 첨자 뒤 ()
  f(a).g.h(b)          // .member 두 단 뒤 ()
  obj.method(x)(y)     // BaseCallee(obj) + .method 첨자 + () + ()
}

// ── 2. 트레일링 람다가 낀 체인 (람다 뒤 같은 줄 체인 연장) ──
def lambdaInChains() {
  f { _ }              // 트레일링 람다만
  f(a) { _ }           // () + 트레일링 람다
  f { _ } (b)          // 람다 뒤 같은 줄 () 연장
  f { _ }.g(b)         // 람다 뒤 .member 뒤 () 연장
  f(a) { _ }.g(b)      // () + 람다 뒤 .member 뒤 ()
  f { _ }[i](b)        // 람다 뒤 첨자 뒤 () 연장
  f(a) { _ } (b) { _ } // () + 람다, 그 뒤 () + 람다 (2단 모두 람다)
  f { _ }.g { _ }      // 람다 → .member → 람다
}

// ── 3. 체인 직후 개행 + 새 문장이 연장으로 오해될 수 있는 형태 ──
def newlineBoundary() {
  f(a)                 // 이 체인은 여기서 끝
  (b)                  // 다음 줄 괄호식 문장 (체인 연장 아님)

  g(x)                 // 체인 끝
  [x]                  // 다음 줄 리스트 리터럴 문장 (첨자 연장 아님 — WS_NO_NL 위반)

  h(a)
  - b                  // 주의: AddExpr(<longest>) 가 `h(a) - b` (뺄셈)로 묶음.
                       // 개행이 체인은 끊지만 이항 연산자는 WS(개행 허용)라 여기 붙는다.

  k { _ }              // 트레일링 람다로 끝난 체인
  (c)                  // 다음 줄 괄호식 문장
}

// ── 4. 중첩: 람다 본문 / match case 본문 / if 조건(ExprNoLambda) 안의 체인 ──
def nestedChains() {
  xs.map {
    _.f(a).g(b)        // 람다 본문 안의 체인
  }
  xs.filter {
    _.f(a)             // 람다 본문 체인, 여기서 끝
    (b)               // 같은 람다 본문 다음 줄 괄호식 문장
  }

  match v {
    case .some(x) -> f(x).g(x)   // case 본문 체인 + 다음 case 경계
    case .none -> h()            // 다음 case 의 체인
  }

  match w {
    case .a -> f(a)              // case 본문 체인
      .g(b)                     // .member 는 WS(개행) 허용 → 같은 체인 연장
    case .b -> k()              // 다음 case
  }

  if f(a).g(b) {                // if 조건(ExprNoLambda) 안의 체인
    doThing()
  }
  while p(x).q(y) {             // while 조건 안의 체인
    step()
  }
}

// ── 5. let 바인딩 rhs 의 체인 + 다음 줄 문장 ──
def letBindingChains() {
  let a = f(x).g(y)             // let rhs 체인
  let b = h(z)                  // 다음 줄 let, 이전 체인 연장 아님
  let c = f(x)[i](y)            // rhs 에 첨자 낀 체인
  let d = f { _ }.g(y)          // rhs 에 람다 낀 체인
  let e = obj.m(x).n(y).o(z)    // rhs 긴 .member 체인
  process(a, b, c, d, e)
}

// ── 6. 개행을 사이에 둔 .member 연장 (WS 는 개행 허용) ──
def dottedNewlineContinuation() {
  builder(x)
    .add(a)                    // 다음 줄 .member — 같은 체인 (Subscribe_ 의 WS 는 개행 포함)
    .add(b)
    .build()
  chain(x)
    .map { _ }                 // 개행 뒤 .member + 트레일링 람다
    .filter { _ }
}
