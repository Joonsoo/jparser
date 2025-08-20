# Milestone group parser 3

* mgroup3 는 코틀린으로 새로 작성하는 mgroup 파서
* 기존 mgroup2와의 차이점:
  * 파서 제너레이터와 파서를 모두 코틀린으로 새로 작성
  * 기존에는 NaiveParser의 코드를 활용하고 있었던 부분도 새로 작성
  * accept condition을 처리하는 방식을 단순화
    * start symbol로 시작하는 main path와 동일한 방식으로 accept condition symbol들을 처리
    * 그걸 기본으로 두고, longest symbol처럼 이미 컨텍스트에 있는 path의 진행경과로 대체할 수 있는 부분은 대체
    * 이렇게 하면 기존 방식에 비해 추적하는 경로의 수가 늘어나서 좀 느려질 수 있을 수도 있을텐데 훨씬 복잡도를 줄일 수 있을 것으로 기대.
      * 그리고 어차피 lookahead symbol은 잘 쓰지도 않음
* 개발하면서 문서화 및 rust나 C++ 파서 구현도 함께 진행할 예정
  * rust보단 C++이 나을듯


* 기존의 milestone parser, mgroup1/2 parser들은 NaiveParser의 그래프를 갖고 동작하게 만드는데 집중하면서,
  (특히 conditional symbol들을 처리할 때) 생긴 지나친 복잡성들이 있었음
  * 예를 들어 longest 심볼의 경우, NaiveParser는 derive 시에 longest 조건을 처리하기 위한 별도의 노드를 추가하거나
    새로운 accept condition을 만들지 않고, progress 시 accept condition을 추가하는 방식이었다.
  * 그리고 m* parser들도 같은 방식으로 동작하게 되면서 이러한 조건 비교가 무척 복잡해졌음.
  * 하지만 mgroup3 파서에서는 main paths 외에 cond(itional) paths 를 별도로 두고 해당 심볼을 추적하도록 해서 구조를 단순화하려고 한다.
  * 단, longest의 경우 기존 경로가 cond path와 같은 역할을 할 수 있어서, 최적화를 위한 장치를 추후에 추가할 수는 있음.
  * 하지만 기본 기조는 구조를 단순화하기 위해 longest symbol 조건 확인을 위한 별도의 경로를 유지하는 것.
  * 이렇게 하면 파싱 과정에서 관리해야하는 경로의 갯수가 많아질 수 있지만,
    대신 기존에는 조건 확인을 위해 GenActions를 검토하는 과정이 복잡했기 때문에 성능상 유불리는 비교가 필요.
