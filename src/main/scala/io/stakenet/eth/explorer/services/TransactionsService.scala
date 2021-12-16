package io.stakenet.eth.explorer.services

import com.google.inject.Inject
import io.stakenet.eth.explorer.models.Transaction
import io.stakenet.eth.explorer.repository.transactions.TransactionsRepository

import scala.concurrent.Future

class TransactionsService @Inject() (transactionsRepository: TransactionsRepository.FutureImpl) {

  def getPaginatedTransactions(address: String, startAfter: Option[String], limit: Int): Future[List[Transaction]] = {
    startAfter match {
      case Some(startAfter) => transactionsRepository.findByAddress(address, limit, startAfter)
      case None => transactionsRepository.findByAddress(address, limit)
    }
  }
}
