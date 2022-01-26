package io.stakenet.eth.explorer.repository.transactions

import java.sql.Connection

import anorm._
import io.stakenet.eth.explorer.models.Transaction
import org.postgresql.util.{PSQLException, PSQLState}

object TransactionsDAO {

  object Constraints {
    val transactionsHashPK = "transactions_hash_pk"
    val transactionsBlocksNumberFK = "transactions_blocks_number_fk"
    val transactionsBlocksBlockHashFK = "transactions_blocks_block_hash_fk"
  }

  def create(transaction: Transaction)(implicit conn: Connection): Unit = {
    try {
      SQL"""
         INSERT INTO transactions(
           hash,
           nonce,
           block_hash,
           block_number,
           transaction_index,
           from_address,
           to_address,
           value,
           gas_price,
           gas,
           input,
           creates,
           public_key,
           raw,
           timestamp,
           token_transfer_recipient,
           status
         ) VALUES (
          ${transaction.hash},
          ${transaction.nonce},
          ${transaction.blockHash},
          ${transaction.blockNumber},
          ${transaction.transactionIndex},
          ${transaction.from},
          ${transaction.to},
          ${transaction.value},
          ${transaction.gasPrice},
          ${transaction.gas},
          ${transaction.input},
          ${transaction.creates},
          ${transaction.publicKey},
          ${transaction.raw},
          ${transaction.timestamp},
          ${transaction.tokenTransferRecipient},
          ${transaction.status.map(_.entryName)}::TRANSACTION_STATUS
         )
       """.execute()

      ()
    } catch {
      case e: PSQLException if violatesConstraint(e, Constraints.transactionsHashPK) =>
        throw new PSQLException(s"transaction ${transaction.hash} already exist", PSQLState.DATA_ERROR)
      case e: PSQLException if violatesConstraint(e, Constraints.transactionsBlocksNumberFK) =>
        throw new PSQLException(s"block ${transaction.blockNumber} does not exist", PSQLState.DATA_ERROR)
      case e: PSQLException if violatesConstraint(e, Constraints.transactionsBlocksBlockHashFK) =>
        throw new PSQLException(s"block ${transaction.blockHash} does not exist", PSQLState.DATA_ERROR)
    }
  }

  def get(hash: String)(implicit conn: Connection): Option[Transaction] = {
    val result = SQL"""
         SELECT t.hash, t.nonce, t.block_hash, t.block_number, t.transaction_index, t.from_address, t.to_address,
           t.value, t.gas_price, t.gas, t.input, t.creates, t.public_key, t.raw, b.time AS timestamp, t.status
         FROM transactions t
         INNER JOIN blocks b USING(block_hash)
         WHERE t.hash = $hash
       """.as(TransactionParsers.transactionParser.singleOpt)

    result
  }

  def findByAddress(address: String, limit: Int)(implicit conn: Connection): List[Transaction] = {
    val result = SQL"""
         SELECT t.hash, t.nonce, t.block_hash, t.block_number, t.transaction_index, t.from_address, t.to_address,
           t.value, t.gas_price, t.gas, t.input, t.creates, t.public_key, t.raw, b.time AS timestamp, t.status
         FROM transactions t
         INNER JOIN blocks b USING(block_hash)
         WHERE t.from_address = $address OR t.to_address = $address OR t.token_transfer_recipient = $address
         ORDER BY b.time DESC, t.hash ASC
         LIMIT $limit
       """.as(TransactionParsers.transactionParser.*)

    updateTransactionsTimestamp(result)

    result
  }

  def findByAddress(address: String, limit: Int, startAfter: String)(implicit conn: Connection): List[Transaction] = {
    val result = SQL"""
        WITH ranked AS (
           SELECT ROW_NUMBER() OVER(ORDER BY b.time DESC, t.hash ASC) AS rank, t.hash, t.nonce, t.block_hash,
             t.block_number, t.transaction_index, t.from_address, t.to_address, t.value, t.gas_price, t.gas, t.input,
             t.creates, t.public_key, t.raw, b.time AS timestamp, t.status
           FROM transactions t
           INNER JOIN blocks b USING(block_hash)
           WHERE t.from_address = $address OR t.to_address = $address OR t.token_transfer_recipient = $address
           ORDER BY t.timestamp DESC, t.hash ASC
        )
        
        SELECT *
        FROM ranked
        WHERE rank > (SELECT rank FROM ranked WHERE hash = $startAfter)
        LIMIT $limit
       """.as(TransactionParsers.transactionParser.*)

    updateTransactionsTimestamp(result)

    result
  }

  def deleteBlockTransactions(blockHash: String)(implicit conn: Connection): List[Transaction] = {
    SQL"""
         DELETE FROM transactions
         USING (SELECT time FROM blocks WHERE block_hash = $blockHash) b
         WHERE block_hash = $blockHash
         RETURNING hash, nonce, block_hash, block_number, transaction_index, from_address, to_address, value, gas_price,
           gas, input, creates, public_key, raw, b.time AS timestamp, status
       """.as(TransactionParsers.transactionParser.*)
  }

  private def updateTransactionsTimestamp(transactions: List[Transaction])(implicit conn: Connection): Unit = {
    if (transactions.nonEmpty) {
      SQL"""
         UPDATE transactions t
         SET timestamp = b.time
         FROM blocks b
         WHERE b.block_hash = t.block_hash AND
          t.timestamp IS NULL AND
          t.hash IN(${transactions.map(_.hash)})
       """.executeUpdate()
    }

    ()
  }

  private def violatesConstraint(error: PSQLException, constraint: String): Boolean = {
    error.getServerErrorMessage.getConstraint == constraint
  }
}
