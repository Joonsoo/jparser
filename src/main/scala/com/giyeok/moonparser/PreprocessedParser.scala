package com.giyeok.moonparser

import com.giyeok.moonparser.Inputs.TermGroupDesc
import Kernels.Kernel
import PreprocessedParserSpec._
import com.giyeok.moonparser.ParsingErrors.ParsingError

// KernelSet이 state의 의미라고 보면 됨

class PreprocessedParser(spec: PreprocessedParserSpec) {
    import Inputs.{ ConcreteInput => Input }

    // TODO Context에 Reverter 정보 넣기
    case class Context(kernelSet: KernelSet, ipnPaths: Map[Kernel, Seq[IPNPath]]) {
        assert(spec.kernelSetActions contains kernelSet)

        def proceed(input: Input): Either[Context, ParsingError] = {
            val actionsMap = spec.kernelSetActions(kernelSet)
            val appliables = actionsMap filter { _._1 contains input }
            assert(appliables.size <= 1)
            if (appliables.isEmpty) {
                Right(ParsingErrors.UnexpectedInput(input))
            } else {
                // 1. 현재 KernelSet/State에서 이 입력이 들어왔을 때 기존 ipn path들을 어떻게 바꾸어야 되는지 정의된대로 바꾸고
                val actions = appliables.head._2.actions
                assert(ipnPaths.keySet == actions.keySet)
                val newPaths = ipnPaths flatMap { ipnPaths =>
                    val (kernel, paths) = ipnPaths
                    actions(kernel) flatMap { action => paths map { _.applyAction(action, input) } }
                }
                // 2. 얻어진 newPaths를 뒤에서부터 스택이 비거나/더이상 finishable 하지 않은 것이 나올 때까지 lift해서
                //    (이 때 join은 어떻게 할 지 고민해야함)
                // 2-1. 스택이 비어서 starting symbol이 lift된 것들은 result candidate이 되고
                // 2-2. 그 과정에서 derivable한 노드가 마지막에 위치했던 모든 path를 기록한 다음,
                //      그 path의 마지막에 있는 커널들만 모으면 다음 kernelSet이 되고, 이 때 ipnPaths는 그 path들을 groupBy _.kernel 해서 구하면 됨
                // - 이 때 reverter는 어떻게 될 지 고민해야함
            }
            ???
        }
    }

    // TODO IPNPath - join 어떻게 할 지 고민
    case class IPNPath() {
        def applyAction(action: KernelAction, terminal: Input): IPNPath = ???
    }

    val startingContext = Context(
        KernelSet(Set(spec.startingKernel)),
        Map(spec.startingKernel -> Seq(IPNPath( /* startingKernel만 포함된 빈 노드 */ ))))
}

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
