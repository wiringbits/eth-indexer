package io.stakenet.eth.explorer.services

import akka.pattern.ask
import akka.util.Timeout
import io.stakenet.eth.explorer.executors.BlockingExecutionContext
import io.stakenet.eth.explorer.models.{MarketStatistics, SynchronizationProgress}
import io.stakenet.eth.explorer.repository.blocks.BlocksRepository
import io.stakenet.eth.explorer.tasks.currencySynchronizer.CurrencySynchronizerActor
import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration._

class StatisticsService @Inject()(
    blocksRepository: BlocksRepository.FutureImpl,
    ethService: ETHService,
    ethCurrencySynchronizerActor: CurrencySynchronizerActor.EthRef,
    wethCurrencySynchronizerActor: CurrencySynchronizerActor.WethRef,
    usdtCurrencySynchronizerActor: CurrencySynchronizerActor.UsdtRef
)(implicit ec: BlockingExecutionContext) {

  def getEthPrices(): Future[MarketStatistics] = {
    implicit val timeout: Timeout = 10.seconds

    ethCurrencySynchronizerActor.ref
      .ask(CurrencySynchronizerActor.Command.GetMarketStatistics)
      .mapTo[MarketStatistics]
  }

  def getWethPrices(): Future[MarketStatistics] = {
    implicit val timeout: Timeout = 10.seconds

    wethCurrencySynchronizerActor.ref
      .ask(CurrencySynchronizerActor.Command.GetMarketStatistics)
      .mapTo[MarketStatistics]
  }

  def getUsdtPrices(): Future[MarketStatistics] = {
    implicit val timeout: Timeout = 10.seconds

    usdtCurrencySynchronizerActor.ref
      .ask(CurrencySynchronizerActor.Command.GetMarketStatistics)
      .mapTo[MarketStatistics]
  }

  def getSynchronizationProgress(): Future[SynchronizationProgress] = {
    for {
      latestBlockNumber <- ethService.getLatestBlockNumber()
      latestSynchronizedBlock <- blocksRepository.getLatestBlock()
      latestSynchronizedBlockNumber = latestSynchronizedBlock.map(_.number).getOrElse(BigInt(0))
    } yield SynchronizationProgress(total = latestBlockNumber, synced = latestSynchronizedBlockNumber)
  }
}
