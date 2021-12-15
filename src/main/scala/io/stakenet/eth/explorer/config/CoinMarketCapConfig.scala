package io.stakenet.eth.explorer.config

import io.stakenet.eth.explorer.config.CoinMarketCapConfig._
import play.api.Configuration

case class CoinMarketCapConfig(host: Host, key: Key, ethCoinId: CoinID, wethCoinId: CoinID, usdtCoinId: CoinID)

object CoinMarketCapConfig {

  case class Host(string: String) extends AnyVal
  case class Key(string: String) extends AnyVal
  case class CoinID(string: String) extends AnyVal

  def apply(config: Configuration): CoinMarketCapConfig = {
    val host = Host(config.get[String]("host"))
    val key = Key(config.get[String]("key"))
    val ethCoinID = CoinID(config.get[String]("ethCoinId"))
    val wethCoinID = CoinID(config.get[String]("wethCoinId"))
    val usdtCoinID = CoinID(config.get[String]("usdtCoinId"))

    CoinMarketCapConfig(
      host = host,
      key = key,
      ethCoinId = ethCoinID,
      wethCoinId = wethCoinID,
      usdtCoinId = usdtCoinID
    )
  }
}
