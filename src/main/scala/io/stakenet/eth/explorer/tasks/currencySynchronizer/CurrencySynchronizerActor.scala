package io.stakenet.eth.explorer.tasks.currencySynchronizer

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import io.stakenet.eth.explorer.models.{MarketInformation, MarketStatistics}
import io.stakenet.eth.explorer.services.Currency

class CurrencySynchronizerActor extends Actor {
  import context._

  def receive: Receive = {
    val initialPrices = Currency.values.map(currency => currency -> BigDecimal(0)).toMap
    val initialMarketInformation = MarketInformation(0, 0)

    behavior(MarketStatistics(initialPrices, initialMarketInformation))
  }

  private def behavior(marketStatistics: MarketStatistics): Receive = {
    case CurrencySynchronizerActor.Command.UpdatePrice(currency, price) =>
      val updatedStatistics = marketStatistics.copy(prices = marketStatistics.prices + (currency -> price))
      become(behavior(updatedStatistics))
    case CurrencySynchronizerActor.Command.UpdateMarketInformation(marketInformation) =>
      val updatedStatistics = marketStatistics.copy(marketInformation = marketInformation)
      become(behavior(updatedStatistics))
    case CurrencySynchronizerActor.Command.GetMarketStatistics =>
      sender() ! marketStatistics
  }
}

object CurrencySynchronizerActor {

  def props(): Props = {
    Props(new CurrencySynchronizerActor())
  }

  final class EthRef private (val ref: ActorRef)

  object EthRef {

    def apply()(implicit system: ActorSystem): EthRef = {
      val actor = system.actorOf(props(), "eth-currency-synchronizer")
      new EthRef(actor)
    }
  }

  final class WethRef private (val ref: ActorRef)

  object WethRef {

    def apply()(implicit system: ActorSystem): WethRef = {
      val actor = system.actorOf(props(), "weth-currency-synchronizer")
      new WethRef(actor)
    }
  }

  final class UsdtRef private (val ref: ActorRef)

  object UsdtRef {

    def apply()(implicit system: ActorSystem): UsdtRef = {
      val actor = system.actorOf(props(), "usdt-currency-synchronizer")
      new UsdtRef(actor)
    }
  }

  sealed trait Command extends Product with Serializable

  object Command {
    final case class UpdatePrice(currency: Currency, price: BigDecimal)
    final case class UpdateMarketInformation(marketInformation: MarketInformation)
    final case class GetMarketStatistics()
  }
}
