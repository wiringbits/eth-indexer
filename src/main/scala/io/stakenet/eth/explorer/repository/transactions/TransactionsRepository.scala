package io.stakenet.eth.explorer.repository.transactions

import io.stakenet.eth.explorer.executors.DatabaseExecutionContext
import io.stakenet.eth.explorer.models.Transaction
import javax.inject.Inject

import scala.concurrent.Future

trait TransactionsRepository[F[_]] {
  def get(hash: String): F[Option[Transaction]]
  def findByAddress(address: String, limit: Int): F[List[Transaction]]
  def findByAddress(address: String, limit: Int, startAfter: String): F[List[Transaction]]
}

object TransactionsRepository {

  type Id[T] = T
  trait Blocking extends TransactionsRepository[Id]

  class FutureImpl @Inject()(blocking: Blocking)(implicit ec: DatabaseExecutionContext)
      extends TransactionsRepository[Future] {
    override def get(hash: String): Future[Option[Transaction]] = Future {
      blocking.get(hash)
    }

    override def findByAddress(address: String, limit: Int): Future[List[Transaction]] = Future {
      blocking.findByAddress(address, limit)
    }

    override def findByAddress(address: String, limit: Int, startAfter: String): Future[List[Transaction]] = Future {
      blocking.findByAddress(address, limit, startAfter)
    }
  }
}
