package com.giyeok.jparser.nparser2

import com.giyeok.jparser.nparser.AcceptCondition.AcceptCondition
import com.giyeok.jparser.nparser.Kernel

sealed trait ParsingTask {
  val kernel: Kernel
}

case class DeriveTask(kernel: Kernel) extends ParsingTask

case class FinishTask(kernel: Kernel) extends ParsingTask

case class ProgressTask(kernel: Kernel, condition: AcceptCondition) extends ParsingTask
