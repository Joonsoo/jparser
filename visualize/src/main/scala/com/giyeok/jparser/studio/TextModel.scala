package com.giyeok.jparser.studio

trait TextModel {
    def modify(location: Int, length: Int, inserted: String): TextModel

    val text: String
    // diff의 결과는 반드시 location이 작은 것부터
    def diff(textModel: TextModel): Seq[TextModel.TextChange]

    def normalize: TextModel = TextModel(this.text)
}

case class WholeText(text: String) extends TextModel {
    def modify(location: Int, length: Int, inserted: String): TextModel = DiffText(this, TextModel.TextChange(location, length, inserted), 1)

    def diff(textModel: TextModel): Seq[TextModel.TextChange] = ???
}

case class DiffText(base: TextModel, change: TextModel.TextChange, changes: Int) extends TextModel {
    def modify(location: Int, length: Int, inserted: String): TextModel = {
        // TODO 이 이벤트를 change에 합칠 수 있으면 changes를 늘리지 말고 합쳐서 반환
        DiffText(this, TextModel.TextChange(location, length, inserted), changes + 1)
    }

    lazy val text: String = {
        val baseText = base.text
        val result = baseText.substring(0, change.location) + change.inserted + baseText.substring(change.location + change.length)
        println(s"Text: $result")
        result
    }
    def diff(textModel: TextModel): Seq[TextModel.TextChange] = ???
}

object TextModel {
    def apply(text: String): TextModel = WholeText(text)

    // `length`가 0이면 `location`의 위치에 `inserted`가 삽입된 것이고
    // `inserted`가 비어있으면 `location`의 위치부터 `length`만큼 삭제된 것이고
    // 둘 다 아닌 경우엔 문자열이 대치된 것임
    case class TextChange(location: Int, length: Int, inserted: String)
}
