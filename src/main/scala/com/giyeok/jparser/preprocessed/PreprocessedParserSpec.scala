package com.giyeok.jparser.preprocessed

import com.giyeok.jparser.Kernels.Kernel
import PreprocessedParserSpec._
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParsingErrors
import com.giyeok.jparser.Inputs
import scala.Right
import com.giyeok.jparser.Inputs.TermGroupDesc
import com.giyeok.jparser.Symbols.Terminal

case class PreprocessedParserSpec(startingKernel: Kernel, kernelSetActions: Map[KernelSet, Map[TermGroupDesc, KernelSetAction]]) {
    assert(kernelSetActions forall { ksa =>
        val (ks, actionsMap) = ksa
        actionsMap forall { kv =>
            val (term, actions) = kv
            ks.kernels == actions.actions.keySet
        }
    })
}

object PreprocessedParserSpec {
    // KernelSet과 KernelSetAction은 혹시나 나중에 최적화할 게 있을까 싶어서 클래스로 묶어둠
    case class KernelSet(kernels: Set[Kernel])

    case class KernelSetAction(actions: Map[Kernel, OriginExpand])

    sealed trait ProgNodeSpec
    // cycles는 해당 노드가 lift되면 그 지점에서부터 cycles subgraph를 다시 expand한 뒤 마지막 지점마다 해당 노드 lift해서 나온 parse tree를 덧붙여서 lift를 다시 진행 
    case class OriginExpand(children: Seq[ProgNodeSpec], cycles: Seq[Seq[ProgNodeSpec]])
    case class SymbolNode(kernel: Kernel, children: Seq[ProgNodeSpec], cycles: Seq[Seq[ProgNodeSpec]]) extends ProgNodeSpec
    case class JoinNode(kernel: Kernel, child: ProgNodeSpec, join: ProgNodeSpec, endJoinSwitched: Boolean) extends ProgNodeSpec
    case class TermNode(terminal: Terminal) extends ProgNodeSpec
    case object EmptyNode extends ProgNodeSpec
}

trait PreprocessedParserSpecSerializer {
    def serialize(spec: PreprocessedParserSpec): String
}

object PreprocessedParserSpecSerializer {
    object ToScala extends PreprocessedParserSpecSerializer {
        def serialize(spec: PreprocessedParserSpec): String = {
            ???
        }
    }
}
