package io.stakenet.eth.explorer.modules

import com.google.inject.{AbstractModule, Provides}
import io.stakenet.eth.explorer.config.{CoinMarketCapConfig, CurrencySynchronizerConfig, ETHConfig, SynchronizerConfig}
import org.slf4j.LoggerFactory
import play.api.Configuration

class ConfigurationModule extends AbstractModule {

  private val logger = LoggerFactory.getLogger(this.getClass)

  @Provides
  def ethConfig(appConfig: Configuration): ETHConfig = {
    val config = ETHConfig(appConfig.get[Configuration]("eth"))

    logger.info(s"Loading ETHConfig $config")

    config
  }

  @Provides
  def synchronizerConfig(appConfig: Configuration): SynchronizerConfig = {
    val config = SynchronizerConfig(appConfig.get[Configuration]("synchronizer"))

    logger.info(s"Loading SynchronizerConfig $config")

    config
  }

  @Provides
  def coinMarketCapConfig(appConfig: Configuration): CoinMarketCapConfig = {
    val config = CoinMarketCapConfig(appConfig.get[Configuration]("coinMarketCap"))

    val dummyKey = s"${config.key.toString.take(2)}...${config.key.toString.takeRight(2)}"
    logger.info(s"Loading CoinMarketCapConfig ${config.copy(key = CoinMarketCapConfig.Key(dummyKey))}")

    config
  }

  @Provides
  def currencuSynchronizerConfig(appConfig: Configuration): CurrencySynchronizerConfig = {
    val config = CurrencySynchronizerConfig(appConfig.get[Configuration]("currencySynchronizer"))

    logger.info(s"Loading CurrencySynchronizerConfig $config")

    config
  }
}
