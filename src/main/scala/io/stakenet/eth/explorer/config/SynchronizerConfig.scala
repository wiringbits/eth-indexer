package io.stakenet.eth.explorer.config

import play.api.Configuration

case class SynchronizerConfig(syncTransactionsFromBlock: BigInt)

object SynchronizerConfig {

  def apply(config: Configuration): SynchronizerConfig = {
    val syncTransactionsFromBlock = BigInt(config.get[String]("syncTransactionsFromBlock"))
    SynchronizerConfig(syncTransactionsFromBlock)
  }
}
