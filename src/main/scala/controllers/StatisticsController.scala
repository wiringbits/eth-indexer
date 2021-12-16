package controllers

import io.stakenet.eth.explorer.services.StatisticsService
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

class StatisticsController @Inject() (statisticsService: StatisticsService, cc: ControllerComponents)(implicit
    ec: ExecutionContext
) extends AbstractController(cc) {

  def getEthCurrency(currency: Option[String]) = Action.async { _ =>
    val prices = statisticsService.getEthPrices().map(r => Json.toJson(r))

    currency.map(_.toLowerCase) match {
      case Some(currency) =>
        prices.map { prices =>
          (prices \ currency)
            .asOpt[BigDecimal]
            .map(price => Ok(Json.obj(currency -> price)))
            .getOrElse(NotFound)
        }
      case None => prices.map(Ok(_))
    }
  }

  def getWethCurrency(currency: Option[String]) = Action.async { _ =>
    val prices = statisticsService.getWethPrices().map(r => Json.toJson(r))

    currency.map(_.toLowerCase) match {
      case Some(currency) =>
        prices.map { prices =>
          (prices \ currency)
            .asOpt[BigDecimal]
            .map(price => Ok(Json.obj(currency -> price)))
            .getOrElse(NotFound)
        }
      case None => prices.map(Ok(_))
    }
  }

  def getUsdtCurrency(currency: Option[String]) = Action.async { _ =>
    val prices = statisticsService.getUsdtPrices().map(r => Json.toJson(r))

    currency.map(_.toLowerCase) match {
      case Some(currency) =>
        prices.map { prices =>
          (prices \ currency)
            .asOpt[BigDecimal]
            .map(price => Ok(Json.obj(currency -> price)))
            .getOrElse(NotFound)
        }
      case None => prices.map(Ok(_))
    }
  }
}
