package io.stakenet.eth.explorer.models

import enumeratum.EnumEntry.Uppercase
import enumeratum.{Enum, EnumEntry}

sealed trait TransactionStatus extends EnumEntry with Uppercase

object TransactionStatus extends Enum[TransactionStatus] {
  final case object Success extends TransactionStatus
  final case object Fail extends TransactionStatus

  val values = findValues
}
