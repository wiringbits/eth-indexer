package controllers

import io.stakenet.eth.explorer.Helpers
import io.stakenet.eth.explorer.modules.{
  CurrencySynchronizerModule,
  PollerSynchronizerModule,
  TransactionStatusSynchronizerModule
}
import io.stakenet.eth.explorer.repository.blocks.BlocksRepository
import io.stakenet.eth.explorer.services.ETHService
import org.mockito.MockitoSugar.{mock, when}
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, route, _}
import play.api.{Application, Configuration, Environment, Mode}

import scala.concurrent.Future

class HealthControllerSpec extends PlaySpec {
  "GET /health" should {
    "return OK when latest synced block is close to the latest block" in {
      val ethService = mock[ETHService]
      val blocksRepository = mock[BlocksRepository.Blocking]
      val application = getApplication(ethService, blocksRepository)
      val latestBlockNumber = BigInt(100)
      val latestSyncedBlock = Helpers.randomBlock().copy(number = BigInt(98))

      when(ethService.getLatestBlockNumber()).thenReturn(Future.successful(latestBlockNumber))
      when(blocksRepository.getLatestBlock()).thenReturn(Some(latestSyncedBlock))

      val result = GET(application, "/health")

      status(result) mustBe OK
      contentAsString(result) mustBe ""
    }

    "return InternalServerError when latest synced block and the latest block are too far apart" in {
      val ethService = mock[ETHService]
      val blocksRepository = mock[BlocksRepository.Blocking]
      val application = getApplication(ethService, blocksRepository)
      val latestBlockNumber = BigInt(1000)
      val latestSyncedBlock = Helpers.randomBlock().copy(number = BigInt(50))

      when(ethService.getLatestBlockNumber()).thenReturn(Future.successful(latestBlockNumber))
      when(blocksRepository.getLatestBlock()).thenReturn(Some(latestSyncedBlock))

      val result = GET(application, "/health")

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe "There are 950 missing blocks"
    }

    "return InternalServerError when latest block could not be obtained" in {
      val ethService = mock[ETHService]
      val blocksRepository = mock[BlocksRepository.Blocking]
      val application = getApplication(ethService, blocksRepository)
      val latestSyncedBlock = Helpers.randomBlock().copy(number = BigInt(50))

      when(ethService.getLatestBlockNumber()).thenReturn(Future.failed(new RuntimeException("Error")))
      when(blocksRepository.getLatestBlock()).thenReturn(Some(latestSyncedBlock))

      val result = GET(application, "/health")

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe "Failed to check sync progress"
    }

    "return InternalServerError when latest synced block could not be obtained" in {
      val ethService = mock[ETHService]
      val blocksRepository = mock[BlocksRepository.Blocking]
      val application = getApplication(ethService, blocksRepository)
      val latestBlockNumber = BigInt(100)

      when(ethService.getLatestBlockNumber()).thenReturn(Future.successful(latestBlockNumber))
      when(blocksRepository.getLatestBlock()).thenThrow(new RuntimeException("Error"))

      val result = GET(application, "/health")

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe "Failed to check sync progress"
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

  private def getApplication(ethService: ETHService, blocksRepository: BlocksRepository.Blocking): Application = {
    GuiceApplicationBuilder(loadConfiguration = loadConfigWithoutEvolutions)
      .in(Mode.Test)
      .disable(classOf[PollerSynchronizerModule])
      .disable(classOf[CurrencySynchronizerModule])
      .disable(classOf[TransactionStatusSynchronizerModule])
      .overrides(bind(classOf[ETHService]).to(ethService))
      .overrides(bind(classOf[BlocksRepository.Blocking]).to(blocksRepository))
      .build()
  }
}
