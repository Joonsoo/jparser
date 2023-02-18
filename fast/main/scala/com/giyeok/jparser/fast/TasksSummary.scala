package com.giyeok.jparser.fast

import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.nparser.ParsingContext.Node

case class TasksSummary(progressedKernels: List[(Node, AcceptCondition)], finishedKernels: List[Node])
