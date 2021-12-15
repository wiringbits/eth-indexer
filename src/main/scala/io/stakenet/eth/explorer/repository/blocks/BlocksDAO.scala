package io.stakenet.eth.explorer.repository.blocks

import java.sql.Connection

import anorm._
import io.stakenet.eth.explorer.models.Block
import org.postgresql.util.{PSQLException, PSQLState}

object BlocksDAO {

  object Constraints {
    val blocksBlockHashPK = "blocks_block_hash_pk"
    val blocksNumbertUnique = "blocks_number_unique"
    val blocksParentBlockHashUnique = "blocks_parent_block_hash_unique"
  }

  def create(block: Block)(implicit conn: Connection): Unit = {
    try {
      SQL"""
         INSERT INTO blocks(
          number,
          block_hash,
          parent_block_hash,
          nonce,
          sha3_uncles,
          transactions_root,
          state_root,
          receipt_root,
          author,
          miner,
          mix_hash,
          difficulty,
          total_difficulty,
          extra_data,
          size,
          gas_limit,
          gas_used,
          time
         ) VALUES (
          ${block.number},
          ${block.hash},
          ${block.parentHash},
          ${block.nonce},
          ${block.sha3Uncles},
          ${block.transactionsRoot},
          ${block.stateRoot},
          ${block.receiptRoot},
          ${block.author},
          ${block.miner},
          ${block.mixHash},
          ${block.difficulty},
          ${block.totalDifficulty},
          ${block.extraData},
          ${block.size},
          ${block.gasLimit},
          ${block.gasUsed},
          ${block.timestamp}
         )
       """.execute()

      ()
    } catch {
      case e: PSQLException if violatesConstraint(e, Constraints.blocksBlockHashPK) =>
        throw new PSQLException(s"block ${block.hash} already exist", PSQLState.DATA_ERROR)
      case e: PSQLException if violatesConstraint(e, Constraints.blocksNumbertUnique) =>
        throw new PSQLException(s"block ${block.number} already exist", PSQLState.DATA_ERROR)
      case e: PSQLException if violatesConstraint(e, Constraints.blocksParentBlockHashUnique) =>
        throw new PSQLException(s"block with parent ${block.parentHash} already exist", PSQLState.DATA_ERROR)
    }
  }

  def getLatestBlock()(implicit conn: Connection): Option[Block.WithoutTransactions] = {
    SQL"""
         SELECT number, block_hash, parent_block_hash, nonce, sha3_uncles, transactions_root, state_root,
           receipt_root, author, miner, mix_hash, difficulty, total_difficulty, extra_data, size, gas_limit,
           gas_used, time
         FROM blocks
         ORDER BY number DESC
         LIMIT 1
       """.as(BlockParsers.blockWithoutTransactionsParser.singleOpt)
  }

  def findByHash(hash: String)(implicit conn: Connection): Option[Block.WithoutTransactions] = {
    SQL"""
         SELECT number, block_hash, parent_block_hash, nonce, sha3_uncles, transactions_root, state_root,
           receipt_root, author, miner, mix_hash, difficulty, total_difficulty, extra_data, size, gas_limit,
           gas_used, time
         FROM blocks
         WHERE block_hash = $hash
       """.as(BlockParsers.blockWithoutTransactionsParser.singleOpt)
  }

  def deleteByHash(hash: String)(implicit conn: Connection): Option[Block.WithoutTransactions] = {
    SQL"""
         DELETE FROM blocks
         WHERE block_hash = $hash
         RETURNING number, block_hash, parent_block_hash, nonce, sha3_uncles, transactions_root, state_root,
           receipt_root, author, miner, mix_hash, difficulty, total_difficulty, extra_data, size, gas_limit,
           gas_used, time
       """.as(BlockParsers.blockWithoutTransactionsParser.singleOpt)
  }

  private def violatesConstraint(error: PSQLException, constraint: String): Boolean = {
    error.getServerErrorMessage.getConstraint == constraint
  }
}
