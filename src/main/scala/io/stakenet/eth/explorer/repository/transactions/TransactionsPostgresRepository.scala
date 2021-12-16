package io.stakenet.eth.explorer.repository.transactions

import io.stakenet.eth.explorer.models.Transaction
import io.stakenet.eth.explorer.repository.transactions.TransactionsRepository.Id
import javax.inject.Inject
import play.api.db.Database

class TransactionsPostgresRepository @Inject() (database: Database) extends TransactionsRepository.Blocking {

  override def findByAddress(address: String, limit: Int): Id[List[Transaction]] = {
    database.withConnection { implicit connection =>
      TransactionsDAO.findByAddress(address, limit)
    }
  }

  override def findByAddress(address: String, limit: Int, startAfter: String): Id[List[Transaction]] = {
    database.withConnection { implicit connection =>
      TransactionsDAO.findByAddress(address, limit, startAfter)
    }
  }
}
