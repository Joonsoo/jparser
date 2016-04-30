package com.giyeok.jparser.preprocessed

import com.giyeok.jparser.Kernels.Kernel
import PreprocessedParserSpec._
import com.giyeok.jparser.ParsingErrors.ParsingError
import com.giyeok.jparser.ParsingErrors
import com.giyeok.jparser.Inputs
import scala.Right
import com.giyeok.jparser.Inputs.TermGroupDesc

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

    case class KernelSetAction(actions: Map[Kernel, Seq[KernelAction]])

    // TODO KernelAction에서 reverter, join 어떻게 할 지 고민
    case class KernelAction(pathAppends: Seq[Kernel], nodeAppendToLast: ParseNodeCreationSpec)

    // TODO ParseNodeCreationSpec
    case class ParseNodeCreationSpec()
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
