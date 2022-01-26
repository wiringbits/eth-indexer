package controllers

import io.stakenet.eth.explorer.services.BlocksService
import controllers.Writers.blockWrites
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class BlocksController @Inject() (
    cc: ControllerComponents,
    blocksService: BlocksService
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def getLatest() = Action.async { _ =>
    blocksService.getLatest().map {
      case Some(block) => Ok(Json.toJson(block))
      case None => NotFound("{}")
    }
  }
}
