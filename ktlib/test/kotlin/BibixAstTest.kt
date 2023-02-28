import com.giyeok.jparser.Inputs
import com.giyeok.jparser.ktlib.KernelSet
import com.giyeok.jparser.milestone2.MilestoneParser
import com.giyeok.jparser.proto.MilestoneParser2ProtobufConverter
import com.giyeok.jparser.proto.MilestoneParserDataProto
import com.giyeok.jparser.test.BibixAst

object BibixAstTest {
  fun <T> scala.collection.immutable.List<T>.toKtList(): List<T> =
    List<T>(this.size()) { idx -> this.apply(idx) }

  fun <T> scala.collection.immutable.Seq<T>.toKtList(): List<T> =
    List<T>(this.size()) { idx -> this.apply(idx) }

  fun <T> scala.collection.immutable.Set<T>.toKtSet(): Set<T> =
    this.toList().toKtList().toSet()

  @JvmStatic
  fun main(args: Array<String>) {
    val parserDataProto = this::class.java.getResourceAsStream("/bibix2-parserdata.pb").use {
      MilestoneParserDataProto.Milestone2ParserData.parseFrom(it)
    }
    val parserData = MilestoneParser2ProtobufConverter.fromProto(parserDataProto)

    val parser = MilestoneParser(parserData)
    val inputs = Inputs.fromString("a = \"\$def\"")
    val parseResult = parser.parseOrThrow(inputs)
    val history = parser.kernelsHistory(parseResult).toKtList().map {
      KernelSet(it.toKtSet())
    }
    val buildScript = BibixAst(inputs.toKtList(), history).matchStart()
    println(buildScript)
  }
}
