package io.stakenet.eth.explorer.services

import java.net.ConnectException

import akka.actor.Scheduler
import enumeratum.{Enum, EnumEntry}
import io.stakenet.eth.explorer.config.CoinMarketCapConfig.CoinID
import io.stakenet.eth.explorer.config.{CoinMarketCapConfig, RetryConfig}
import io.stakenet.eth.explorer.executors.BlockingExecutionContext
import io.stakenet.eth.explorer.models.MarketInformation
import io.stakenet.eth.explorer.services.CurrencyService.CoinMarketCapError
import io.stakenet.eth.explorer.services.CurrencyService.CoinMarketCapError.{
  CoinMarketCapRequestFailedError,
  CoinMarketCapUnexpectedResponseError
}
import io.stakenet.eth.explorer.util.RetryableFuture
import javax.inject.Inject
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

sealed abstract class Currency(override val entryName: String) extends EnumEntry

object Currency extends Enum[Currency] {
  final case object USD extends Currency("USD")
  final case object BTC extends Currency("BTC")
  final case object EUR extends Currency("EUR")
  final case object GBP extends Currency("GBP")
  final case object JPY extends Currency("JPY")
  final case object MXN extends Currency("MXN")
  final case object NZD extends Currency("NZD")
  final case object TRY extends Currency("TRY")
  final case object UAH extends Currency("UAH")

  val values = findValues
}

trait CurrencyService {
  def getPrice(coinID: CoinID, currency: Currency): Future[Either[CoinMarketCapError, BigDecimal]]
  def getMarketInformation(coinID: CoinID): Future[Either[CoinMarketCapError, MarketInformation]]
}

class CurrencyServiceCoinMarketCapImpl @Inject()(
    ws: WSClient,
    coinMarketCapConfig: CoinMarketCapConfig,
    retryConfig: RetryConfig
)(
    implicit ec: BlockingExecutionContext,
    scheduler: Scheduler
) extends CurrencyService {

  private def requestFor(url: String) = {
    ws.url(s"${coinMarketCapConfig.host.string}/$url")
      .withHttpHeaders(
        "X-CMC_PRO_API_KEY" -> coinMarketCapConfig.key.string,
        "Accept" -> "application/json",
        "Accept-Encoding" -> "deflate, gzip"
      )
  }

  private def retrying[A](f: => Future[Either[CoinMarketCapError, A]]): Future[Either[CoinMarketCapError, A]] = {
    val retry = RetryableFuture.withExponentialBackoff[Either[CoinMarketCapError, A]](
      retryConfig.initialDelay,
      retryConfig.maxDelay
    )

    val shouldRetry: Try[Either[CoinMarketCapError, A]] => Boolean = {
      case Success(Left(CoinMarketCapRequestFailedError(500))) => true
      case Success(Left(CoinMarketCapRequestFailedError(502))) => true
      case Success(Left(CoinMarketCapRequestFailedError(503))) => true
      case Success(Left(CoinMarketCapRequestFailedError(504))) => true
      case Failure(_: ConnectException) => true
      case _ => false
    }

    retry(shouldRetry) {
      f
    }
  }

  override def getPrice(coinId: CoinID, currency: Currency): Future[Either[CoinMarketCapError, BigDecimal]] = {
    retrying {
      val url = s"v1/tools/price-conversion?id=${coinId.string}&amount=1&convert=${currency.entryName}"
      requestFor(url).get().map { response =>
        (response.status, response) match {
          case (200, r) =>
            Try(r.json).toOption
              .map { json =>
                (json \ "data" \ "quote" \ currency.entryName \ "price")
                  .asOpt[BigDecimal]
                  .map(Right(_))
                  .getOrElse(Left(CoinMarketCapUnexpectedResponseError(json.toString)))
              }
              .getOrElse(Left(CoinMarketCapUnexpectedResponseError(response.body)))
          case (code, _) =>
            Left(CoinMarketCapRequestFailedError(code))
        }
      }
    }
  }

  override def getMarketInformation(coinId: CoinID): Future[Either[CoinMarketCapError, MarketInformation]] = {
    retrying {
      val url = s"v1/cryptocurrency/quotes/latest?id=${coinId.string}"
      requestFor(url).get().map { response =>
        (response.status, response) match {
          case (200, r) =>
            Try(r.json).toOption
              .map { json =>
                for {
                  volume <- (json \ "data" \ coinId.string \ "quote" \ "USD" \ "volume_24h")
                    .asOpt[BigDecimal]
                    .map(Right(_))
                    .getOrElse(Left(CoinMarketCapUnexpectedResponseError(json.toString)))

                  marketcap <- (json \ "data" \ coinId.string \ "quote" \ "USD" \ "market_cap")
                    .asOpt[BigDecimal]
                    .map(Right(_))
                    .getOrElse(Left(CoinMarketCapUnexpectedResponseError(json.toString)))
                } yield MarketInformation(volume, marketcap)
              }
              .getOrElse(Left(CoinMarketCapUnexpectedResponseError(r.body)))
          case (code, _) =>
            Left(CoinMarketCapRequestFailedError(code))
        }
      }
    }
  }
}

object CurrencyService {
  sealed trait CoinMarketCapError

  object CoinMarketCapError {
    case class CoinMarketCapUnexpectedResponseError(response: String) extends CoinMarketCapError
    case class CoinMarketCapRequestFailedError(status: Int) extends CoinMarketCapError
  }
}
