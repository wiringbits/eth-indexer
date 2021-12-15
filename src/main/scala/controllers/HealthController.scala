package controllers

import io.stakenet.eth.explorer.services.StatisticsService
import javax.inject.Inject
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext
import scala.util.Success

class HealthController @Inject()(statisticsService: StatisticsService, cc: ControllerComponents)(
    implicit ec: ExecutionContext
) extends AbstractController(cc) {

  def check() = Action.async { _ =>
    statisticsService
      .getSynchronizationProgress()
      .transform {
        case Success(progress) if progress.missing < 300 => Success(Ok(""))
        case Success(progress) => Success(InternalServerError(s"There are ${progress.missing} missing blocks"))
        case _ => Success(InternalServerError("Failed to check sync progress"))
      }
  }
}
