package controllers

import io.stakenet.eth.explorer.models.{Transaction, TransactionStatus}
import io.stakenet.eth.explorer.modules.{
  CurrencySynchronizerModule,
  PollerSynchronizerModule,
  TransactionStatusSynchronizerModule
}
import io.stakenet.eth.explorer.repository.blocks.BlocksPostgresRepository
import io.stakenet.eth.explorer.repository.transactions.{TransactionsPostgresRepository, TransactionsRepository}
import io.stakenet.eth.explorer.{DatabaseSpec, Helpers}
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsSuccess, JsValue, Json, Reads}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, route, status, _}
import play.api.{Application, Configuration, Environment, Mode}

import scala.concurrent.Future

class TransactionsControllerSpec extends PlaySpec with DatabaseSpec {

  implicit val transactionStatusReads: Reads[TransactionStatus] = (status: JsValue) => {
    JsSuccess(TransactionStatus.withNameInsensitive(status.as[String]))
  }

  implicit val transactionReads: Reads[Transaction] = Json.reads[Transaction]

  "GET /addresses/:address/transactions" should {
    "return first 1000 transactions by default" in {
      val transactionsRepository = new TransactionsPostgresRepository(database)
      val application = getApplication(transactionsRepository)
      val address = Helpers.randomHash()
      val transactions = createTransactionsForAddress(address, numberOfTransactions = 250)

      val result = GET(application, s"/addresses/$address/transactions")
      val expected = transactions.slice(0, 100)

      status(result) mustBe OK
      (contentAsJson(result) \ "data").as[List[Transaction]] mustBe expected
    }

    "return transactions after the specified one" in {
      val transactionsRepository = new TransactionsPostgresRepository(database)
      val application = getApplication(transactionsRepository)
      val address = Helpers.randomHash()
      val transactions = createTransactionsForAddress(address, numberOfTransactions = 250)
      val startAfter = transactions(33).hash

      val result = GET(application, s"/addresses/$address/transactions?scrollId=$startAfter")
      val expected = transactions.slice(34, 134)

      status(result) mustBe OK
      (contentAsJson(result) \ "data").as[List[Transaction]] mustBe expected
    }

    "return the specified number of transactions" in {
      val transactionsRepository = new TransactionsPostgresRepository(database)
      val application = getApplication(transactionsRepository)
      val address = Helpers.randomHash()
      val transactions = createTransactionsForAddress(address, numberOfTransactions = 250)
      val limit = 13

      val result = GET(application, s"/addresses/$address/transactions?limit=$limit")
      val expected = transactions.slice(0, 13)

      status(result) mustBe OK
      (contentAsJson(result) \ "data").as[List[Transaction]] mustBe expected
    }

    "return the last transaction's hash as scrollId" in {
      val transactionsRepository = new TransactionsPostgresRepository(database)
      val application = getApplication(transactionsRepository)
      val address = Helpers.randomHash()
      val transactions = createTransactionsForAddress(address, numberOfTransactions = 250)
      val limit = 13

      val result = GET(application, s"/addresses/$address/transactions?limit=$limit")
      val expected = transactions(12).hash

      status(result) mustBe OK
      (contentAsJson(result) \ "scrollId").as[String] mustBe expected
    }

    "return None for scrollId when result is empty" in {
      val transactionsRepository = new TransactionsPostgresRepository(database)
      val application = getApplication(transactionsRepository)
      val address = Helpers.randomHash()

      val result = GET(application, s"/addresses/$address/transactions")

      status(result) mustBe OK
      (contentAsJson(result) \ "scrollId").asOpt[String] mustBe empty
    }
  }

  private def GET(application: Application, url: String, extraHeaders: (String, String)*): Future[Result] = {
    val headers = (CONTENT_TYPE -> "application/json") :: extraHeaders.toList
    val request = FakeRequest("GET", url).withHeaders(headers: _*)

    route(application, request).get
  }

  private def loadConfigWithoutEvolutions(env: Environment): Configuration = {
    val map = Map("play.evolutions.db.default.enabled" -> false)

    Configuration.from(map).withFallback(Configuration.load(env))
  }

  private def getApplication(transactionsRepository: TransactionsRepository.Blocking): Application = {
    GuiceApplicationBuilder(loadConfiguration = loadConfigWithoutEvolutions)
      .in(Mode.Test)
      .disable(classOf[PollerSynchronizerModule])
      .disable(classOf[CurrencySynchronizerModule])
      .disable(classOf[TransactionStatusSynchronizerModule])
      .overrides(bind(classOf[TransactionsRepository.Blocking]).to(transactionsRepository))
      .build()
  }

  private def createTransactionsForAddress(address: String, numberOfTransactions: Int): List[Transaction] = {
    val blocksRepository = new BlocksPostgresRepository(database)
    val block = Helpers.randomBlock()
    val transactions = List.fill(numberOfTransactions) {
      Helpers
        .randomTransaction()
        .copy(
          blockHash = block.hash,
          blockNumber = block.number,
          from = address,
          timestamp = block.timestamp
        )
    }

    blocksRepository.create(block.withTransactions(transactions))

    transactions.sortBy(_.hash)
  }
}
