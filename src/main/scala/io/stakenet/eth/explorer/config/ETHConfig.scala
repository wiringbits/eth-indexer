package io.stakenet.eth.explorer.config

import play.api.Configuration

case class ETHConfig(url: String)

object ETHConfig {

  def apply(config: Configuration): ETHConfig = {
    val url = config.get[String]("url")
    ETHConfig(url)
  }
}
