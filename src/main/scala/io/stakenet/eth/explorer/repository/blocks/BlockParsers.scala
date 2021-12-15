package io.stakenet.eth.explorer.repository.blocks

import anorm.{Macro, RowParser}
import io.stakenet.eth.explorer.models.Block

object BlockParsers {

  val blockWithoutTransactionsParser: RowParser[Block.WithoutTransactions] = Macro.parser[Block.WithoutTransactions](
    "number",
    "block_hash",
    "parent_block_hash",
    "nonce",
    "sha3_uncles",
    "transactions_root",
    "state_root",
    "receipt_root",
    "author",
    "miner",
    "mix_hash",
    "difficulty",
    "total_difficulty",
    "extra_data",
    "size",
    "gas_limit",
    "gas_used",
    "time"
  )
}
