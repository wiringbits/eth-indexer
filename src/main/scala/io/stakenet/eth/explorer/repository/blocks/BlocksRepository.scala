package io.stakenet.eth.explorer.repository.blocks

import io.stakenet.eth.explorer.executors.DatabaseExecutionContext
import io.stakenet.eth.explorer.models.Block
import javax.inject.Inject

import scala.concurrent.Future

trait BlocksRepository[F[_]] {
  def create(block: Block): F[Unit]

  def getLatestBlock(): F[Option[Block]]

  def removeBlock(hash: String): F[Option[Block]]

  def findByHash(hash: String): F[Option[Block]]
}

object BlocksRepository {

  type Id[T] = T
  trait Blocking extends BlocksRepository[Id]

  class FutureImpl @Inject() (blocking: Blocking)(implicit ec: DatabaseExecutionContext)
      extends BlocksRepository[Future] {

    override def create(block: Block): Future[Unit] = Future {
      blocking.create(block)
    }

    override def getLatestBlock(): Future[Option[Block]] = Future {
      blocking.getLatestBlock()
    }

    override def removeBlock(hash: String): Future[Option[Block]] = Future {
      blocking.removeBlock(hash)
    }

    override def findByHash(hash: String): Future[Option[Block]] = Future {
      blocking.findByHash(hash)
    }
  }
}
