package io.stakenet.eth.explorer.repository.transactions

import anorm.{Column, Macro, RowParser}
import io.stakenet.eth.explorer.models.{Transaction, TransactionStatus}

private[transactions] object TransactionParsers {

  implicit val transactionStatusColumn: Column[TransactionStatus] = Column.columnToString.map(
    TransactionStatus.withNameInsensitive
  )

  val transactionParser: RowParser[Transaction] = Macro.parser[Transaction](
    "hash",
    "nonce",
    "block_hash",
    "block_number",
    "transaction_index",
    "from_address",
    "to_address",
    "value",
    "gas_price",
    "gas",
    "input",
    "creates",
    "public_key",
    "raw",
    "timestamp",
    "status",
    "confirmations"
  )
}
