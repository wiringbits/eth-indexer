package controllers

import controllers.Writers.transactionWrites
import io.stakenet.eth.explorer.services.TransactionsService
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

class TransactionsController @Inject() (
    cc: ControllerComponents,
    transactionsService: TransactionsService
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def findByAddress(address: String, limit: Int, scrollId: Option[String]) = Action.async { _ =>
    transactionsService.getPaginatedTransactions(address, scrollId, limit).map { transactions =>
      val result = Json.obj(
        "data" -> transactions,
        "scrollId" -> transactions.lastOption.map(_.hash)
      )

      Ok(result)
    }
  }
}
