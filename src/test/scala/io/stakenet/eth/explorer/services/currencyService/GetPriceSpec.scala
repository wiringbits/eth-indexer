package io.stakenet.eth.explorer.services.currencyService

import akka.actor.ActorSystem
import io.stakenet.eth.explorer.Helpers.Executors
import io.stakenet.eth.explorer.config.CoinMarketCapConfig.{CoinID, Host, Key}
import io.stakenet.eth.explorer.config.{CoinMarketCapConfig, RetryConfig}
import io.stakenet.eth.explorer.services.CurrencyService.CoinMarketCapError.{
  CoinMarketCapRequestFailedError,
  CoinMarketCapUnexpectedResponseError
}
import io.stakenet.eth.explorer.services.{Currency, CurrencyServiceCoinMarketCapImpl}
import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.annotation.nowarn
import scala.concurrent.Future
import scala.concurrent.duration._

@nowarn
class GetPriceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    actorSystem.terminate()
    ()
  }

  val ws = mock[WSClient]
  val ec = Executors.blockingEC
  val actorSystem = ActorSystem()
  val scheduler = actorSystem.scheduler

  val coinMarketCapConfig = CoinMarketCapConfig(Host("host"), Key("key"), CoinID("id"), CoinID("id"), CoinID("id"))

  val retryConfig = RetryConfig(1.millisecond, 2.milliseconds)

  val request = mock[WSRequest]
  val response = mock[WSResponse]
  when(ws.url(anyString)).thenReturn(request)
  when(request.withHttpHeaders(any())).thenReturn(request)

  val service = new CurrencyServiceCoinMarketCapImpl(ws, coinMarketCapConfig, retryConfig)(ec, scheduler)

  def createSuccessfullResponse(currency: Currency, value: BigDecimal): String = {
    s"""
       |{
       |  "status": {
       |    "timestamp": "2019-12-16T01:58:23.659Z",
       |    "error_code": 0,
       |    "error_message": null,
       |    "elapsed": 6,
       |    "credit_count": 1
       |  },
       |  "data": {
       |    "id": 2633,
       |    "symbol": "XSN",
       |    "name": "Stakenet",
       |    "amount": 1,
       |    "last_updated": "2019-08-30T18:51:11.000Z",
       |    "quote": {
       |      "${currency.entryName}": {
       |        "price": $value,
       |        "last_updated": "2019-08-30T18:51:11.000Z"
       |      }
       |    }
       |  }
       |}
     """.stripMargin
  }

  "getPrice" should {
    "get coin price" in {
      val responseBody = createSuccessfullResponse(Currency.USD, 0.0734864351)
      val json = Json.parse(responseBody)

      mockRequest(request, response)(200, json)

      whenReady(service.getPrice(CoinID("id"), Currency.USD)) { result =>
        result.isRight mustBe true

        val usd = result.right.get
        usd mustBe 0.0734864351
      }
    }

    "fail when status is not 200" in {
      val responseBody = createSuccessfullResponse(Currency.USD, 0.0734864351)
      val json = Json.parse(responseBody)

      mockRequest(request, response)(502, json)

      whenReady(service.getPrice(CoinID("id"), Currency.USD)) { result =>
        result mustBe Left(CoinMarketCapRequestFailedError(502))
      }
    }

    "fail on unexpected response" in {
      val responseBody = createSuccessfullResponse(Currency.USD, 0.0734864351)
      val json = Json.parse(responseBody)

      mockRequest(request, response)(200, json)

      whenReady(service.getPrice(CoinID("id"), Currency.BTC)) { result =>
        result mustBe Left(CoinMarketCapUnexpectedResponseError(json.toString))
      }
    }
  }

  private def mockRequest(request: WSRequest, response: WSResponse)(status: Int, body: JsValue) = {
    when(response.status).thenReturn(status)
    when(response.json).thenReturn(body)
    when(response.body).thenReturn(body.toString())
    when(request.get).thenReturn(Future.successful(response))
  }
}
