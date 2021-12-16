package io.stakenet.eth.explorer.repository.blocks

import io.stakenet.eth.explorer.models.Block
import io.stakenet.eth.explorer.repository.blocks.BlocksRepository.Id
import io.stakenet.eth.explorer.repository.transactions.TransactionsDAO
import javax.inject.Inject
import play.api.db.Database

class BlocksPostgresRepository @Inject() (database: Database) extends BlocksRepository.Blocking {

  def create(block: Block): Unit = {
    database.withTransaction { implicit conn =>
      BlocksDAO.create(block)

      block match {
        case withTransactions: Block.WithTransactions =>
          withTransactions.transactions.foreach { transaction =>
            TransactionsDAO.create(transaction)
          }

        case _ =>
          ()
      }
    }
  }

  def getLatestBlock(): Option[Block] = {
    database.withConnection { implicit conn =>
      BlocksDAO.getLatestBlock()
    }
  }

  override def removeBlock(hash: String): Id[Option[Block]] = {
    database.withTransaction { implicit conn =>
      TransactionsDAO.deleteBlockTransactions(hash)
      BlocksDAO.deleteByHash(hash)
    }
  }

  override def findByHash(hash: String): Id[Option[Block]] = {
    database.withConnection { implicit conn =>
      BlocksDAO.findByHash(hash)
    }
  }
}
