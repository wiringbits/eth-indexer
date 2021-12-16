package io.stakenet.eth.explorer.tasks.currencySynchronizer

import akka.actor.{ActorRef, ActorSystem}
import io.stakenet.eth.explorer.config.CoinMarketCapConfig.CoinID
import io.stakenet.eth.explorer.config.{CoinMarketCapConfig, CurrencySynchronizerConfig}
import io.stakenet.eth.explorer.services.{Currency, CurrencyService}
import javax.inject.Inject
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class CurrencySynchronizerTask @Inject() (
    config: CurrencySynchronizerConfig,
    actorSystem: ActorSystem,
    currencyService: CurrencyService,
    coinMarketCapConfig: CoinMarketCapConfig,
    ethCurrencySynchronizerActor: CurrencySynchronizerActor.EthRef,
    wethCurrencySynchronizerActor: CurrencySynchronizerActor.WethRef,
    usdtCurrencySynchronizerActor: CurrencySynchronizerActor.UsdtRef
)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  start()

  def start(): Unit = {
    if (config.enabled) {

      logger.info("Starting currency synchronizer task")

      sync(ethCurrencySynchronizerActor.ref, coinMarketCapConfig.ethCoinId)
      sync(wethCurrencySynchronizerActor.ref, coinMarketCapConfig.wethCoinId)
      sync(usdtCurrencySynchronizerActor.ref, coinMarketCapConfig.usdtCoinId)
    } else {
      logger.info("Currency synchronizer task is disabled")
    }
  }

  private def sync(currencySynchronizerActor: ActorRef, coinId: CoinID): Unit = {
    val highPriorityCurrencies = List(Currency.BTC, Currency.USD)
    val lowPriorityCurrencies = Currency.values.filterNot(highPriorityCurrencies.contains)

    actorSystem.scheduler.scheduleAtFixedRate(config.initialDelay, config.highPriorityInterval) { () =>
      highPriorityCurrencies.foreach(currency => syncCurrencyPrice(coinId, currency, currencySynchronizerActor))

      currencyService.getMarketInformation(coinId).onComplete {
        case Success(Right(marketInformation)) =>
          logger.info("Market information synced")
          currencySynchronizerActor ! CurrencySynchronizerActor.Command.UpdateMarketInformation(marketInformation)
        case Success(Left(error)) =>
          logger.info(s"market information synchronization failed due to $error")
        case Failure(exception) =>
          logger.info(s"market information synchronization failed due to ${exception.getLocalizedMessage}", exception)
      }
    }

    actorSystem.scheduler.scheduleAtFixedRate(config.initialDelay, config.lowPriorityInterval) { () =>
      lowPriorityCurrencies.foreach(currency => syncCurrencyPrice(coinId, currency, currencySynchronizerActor))
    }

    ()
  }

  private def syncCurrencyPrice(coinId: CoinID, currency: Currency, currencySynchronizerActor: ActorRef): Unit = {
    currencyService.getPrice(coinId, currency).onComplete {
      case Success(Right(price)) =>
        logger.info(s"${currency.entryName} price synced")
        currencySynchronizerActor ! CurrencySynchronizerActor.Command.UpdatePrice(currency, price)
      case Success(Left(error)) =>
        logger.info(s"${currency.entryName} price synchronization failed due to $error")
      case Failure(exception) =>
        logger.info(
          s"${currency.entryName} price synchronization failed due to ${exception.getLocalizedMessage}",
          exception
        )
    }
  }
}
