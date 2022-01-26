package controllers

import io.stakenet.eth.explorer.models.{Block, TransactionStatus}
import io.stakenet.eth.explorer.modules.{
  CurrencySynchronizerModule,
  PollerSynchronizerModule,
  TransactionStatusSynchronizerModule
}
import io.stakenet.eth.explorer.repository.blocks
import io.stakenet.eth.explorer.repository.blocks.{BlocksPostgresRepository, BlocksRepository}
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

class BlocksControllerSpec extends PlaySpec with DatabaseSpec {
  implicit val transactionStatusReads: Reads[TransactionStatus] = (status: JsValue) => {
    JsSuccess(TransactionStatus.withNameInsensitive(status.as[String]))
  }

  implicit val blockReads: Reads[Block.WithoutTransactions] = Json.reads[Block.WithoutTransactions]

  "GET /blocks/latests" should {
    "return latest block" in {
      val blocksRepository = new BlocksPostgresRepository(database)
      val application = getApplication(blocksRepository)

      createBlock(number = 0)
      createBlock(number = 1)
      val latest = createBlock(number = 2)

      val result = GET(application, s"/blocks/latest")

      status(result) mustBe OK
      contentAsJson(result).as[Block.WithoutTransactions] mustBe latest
    }

    "return not found when there are no blocks" in {
      val blocksRepository = new BlocksPostgresRepository(database)
      val application = getApplication(blocksRepository)

      val result = GET(application, s"/blocks/latest")

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "{}"
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

  private def getApplication(blocksRepository: BlocksRepository.Blocking): Application = {
    GuiceApplicationBuilder(loadConfiguration = loadConfigWithoutEvolutions)
      .in(Mode.Test)
      .disable(classOf[PollerSynchronizerModule])
      .disable(classOf[CurrencySynchronizerModule])
      .disable(classOf[TransactionStatusSynchronizerModule])
      .overrides(bind(classOf[blocks.BlocksRepository.Blocking]).to(blocksRepository))
      .build()
  }

  private def createBlock(number: BigInt): Block = {
    val blocksRepository = new BlocksPostgresRepository(database)
    val block = Helpers.randomBlock().copy(number = number)

    blocksRepository.create(block)

    block
  }
}
