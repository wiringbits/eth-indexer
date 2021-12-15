package io.stakenet.eth.explorer.tasks

import akka.actor.ActorSystem
import io.stakenet.eth.explorer.services.{ETHService, SynchronizerService}
import javax.inject.Inject
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class PollerSynchronizerTask @Inject()(
    ethService: ETHService,
    synchronizerService: SynchronizerService
)(
    implicit ec: ExecutionContext,
    actorSystem: ActorSystem
) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  start()

  def start() = {
    logger.info("Starting the poller synchronizer task")
    actorSystem.scheduler.scheduleOnce(30.seconds) {
      run()
    }
  }

  private def run(): Unit = {
    val result = for {
      latestBlockNumber <- ethService.getLatestBlockNumber()
      result <- synchronizerService.synchronize(latestBlockNumber)
    } yield result

    result.onComplete {
      case Success(_) => ()
      case Failure(error) => logger.error("Error syncing", error)
    }

    result.onComplete { _ =>
      actorSystem.scheduler.scheduleOnce(20.seconds) { run() }
    }
  }
}
