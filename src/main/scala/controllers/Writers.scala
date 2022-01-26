package controllers

import io.stakenet.eth.explorer.models.{Block, Transaction, TransactionStatus}
import play.api.libs.json.{Json, Writes}

object Writers {

  implicit val transactionStatusWrites: Writes[TransactionStatus] = (status: TransactionStatus) => {
    Json.toJson(status.entryName)
  }

  implicit val transactionWrites: Writes[Transaction] = Json.writes[Transaction]

  implicit val blockWithoutTransactionsWriter: Writes[Block.WithoutTransactions] =
    Json.writes[Block.WithoutTransactions]
  implicit val blockWithTransactionsWriter: Writes[Block.WithTransactions] = Json.writes[Block.WithTransactions]
  implicit val blockWrites: Writes[Block] = {
    case block: Block.WithoutTransactions => Json.toJson(block)
    case block: Block.WithTransactions => Json.toJson(block)
  }
}
