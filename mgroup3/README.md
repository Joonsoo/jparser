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
