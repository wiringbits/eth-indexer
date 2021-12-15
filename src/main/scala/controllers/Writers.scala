package controllers

import io.stakenet.eth.explorer.models.{Transaction, TransactionStatus}
import play.api.libs.json.{Json, Writes}

object Writers {

  implicit val transactionStatusWrites: Writes[TransactionStatus] = (status: TransactionStatus) => {
    Json.toJson(status.entryName)
  }

  implicit val transactionWrites: Writes[Transaction] = Json.writes[Transaction]
}
