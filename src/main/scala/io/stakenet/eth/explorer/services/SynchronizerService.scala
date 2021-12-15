package io.stakenet.eth.explorer.services

import io.stakenet.eth.explorer.config.SynchronizerConfig
import io.stakenet.eth.explorer.models.Block
import io.stakenet.eth.explorer.repository.blocks.BlocksRepository
import javax.inject.Inject
import org.slf4j.LoggerFactory

import scala.collection.immutable.NumericRange
import scala.concurrent.{ExecutionContext, Future}

trait LedgerSynchronizer {
  def synchronize(number: BigInt): Future[Unit]
}

class SynchronizerService @Inject()(
    ethService: ETHService,
    blocksRepository: BlocksRepository.FutureImpl,
    synchronizerConfig: SynchronizerConfig
)(
    implicit ec: ExecutionContext
) extends LedgerSynchronizer {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Synchronize the given block with our ledger database.
   *
   * The synchronization involves a very complex logic in order to handle
   * several corner cases, be sure to not call this method concurrently
   * because the behavior is undefined.
   */
  def synchronize(number: BigInt): Future[Unit] = {
    for {
      block <- fetchBlock(number)
      _ <- synchronize(block)
    } yield ()
  }

  private def synchronize(block: Block): Future[Unit] = {
    logger.info(s"Synchronize block ${block.number}, hash = ${block.hash}")

    for {
      latestBlock <- blocksRepository.getLatestBlock()

      _ <- latestBlock
        .map(latestBlock => onLatestBlock(latestBlock, block))
        .getOrElse(onEmptyLedger(block))
    } yield ()
  }

  /**
   * 1. current ledger is empty:
   * 1.1. the given block is the genensis block, it is added.
   * 1.2. the given block is not the genesis block, sync everything until the given block.
   */
  private def onEmptyLedger(block: Block): Future[Unit] = {
    if (block.number == 0) {
      logger.info(s"Synchronize genesis block on empty ledger, hash = ${block.hash}")

      blocksRepository.create(block)
    } else {
      logger.info(s"Synchronize block ${block.number} on empty ledger, hash = ${block.hash}")

      for {
        _ <- sync(BigInt(0) until block.number)
        _ <- synchronize(block)
      } yield ()
    }
  }

  /**
   * 2. current ledger has blocks until N, given block height H:
   * 2.1. if N+1 == H and its previous blockhash is N, it is added.
   * 2.2. if N+1 == H and its previous blockhash isn't N, pick the expected block N from H and apply the whole process with it, then, apply H.
   * 2.3. if H > N+1, sync everything until H.
   * 2.4. if H <= N, if the hash already exists, it is ignored.
   * 2.5. if H <= N, if the hash doesn't exists, remove blocks from N to H (included), then, add the new H.
   */
  private def onLatestBlock(ledgerBlock: Block, newBlock: Block): Future[Unit] = {
    if (ledgerBlock.number + 1 == newBlock.number && newBlock.parentHash == ledgerBlock.hash) {
      logger.info(s"Appending block ${newBlock.number}, hash = ${newBlock.hash}")

      blocksRepository.create(newBlock)
    } else if (ledgerBlock.number + 1 == newBlock.number) {
      logger.info(s"Reorganization to push block ${newBlock.number}, hash = ${newBlock.hash}")

      for {
        previousBlock <- fetchBlock(newBlock.number - 1)
        _ <- synchronize(previousBlock)
        _ <- synchronize(newBlock)
      } yield ()
    } else if (newBlock.number > ledgerBlock.number) {
      logger.info(s"Filling holes to push block ${newBlock.number}, hash = ${newBlock.hash}")

      for {
        _ <- sync(ledgerBlock.number + 1 until newBlock.number)
        _ <- synchronize(newBlock)
      } yield ()
    } else {
      for {
        expectedBlockMaybe <- blocksRepository.findByHash(newBlock.hash)

        _ = logger.info(
          s"Checking possible existing block ${newBlock.number}, hash = ${newBlock.hash}, exists = ${expectedBlockMaybe.isDefined}"
        )
        _ <- expectedBlockMaybe
          .map(_ => Future.unit)
          .getOrElse {
            for {
              _ <- trimTo(newBlock.number)
              _ <- synchronize(newBlock)
            } yield ()
          }
      } yield ()
    }
  }

  /**
   * Sync the given range to our ledger.
   */
  private def sync(range: NumericRange.Exclusive[BigInt]): Future[Unit] = {
    logger.info(s"Syncing block range = $range")

    range.foldLeft[Future[Unit]](Future.unit) {
      case (previous, number) =>
        for {
          _ <- previous
          block <- fetchBlock(number)
          _ <- synchronize(block)
        } yield ()
    }
  }

  /**
   * Trim the ledger until the given block number, if the number is 4,
   * the last stored block will be 3.
   */
  private def trimTo(number: BigInt): Future[Unit] = {
    for {
      blockMaybe <- blocksRepository.getLatestBlock()

      _ <- blockMaybe
        .map { block =>
          blocksRepository.removeBlock(block.hash).flatMap { _ =>
            logger.info(s"Trimmed block ${block.number} from the ledger")

            if (block.number == number) {
              Future.unit
            } else {
              trimTo(number)
            }
          }
        }
        .getOrElse(Future.unit)
    } yield ()
  }

  private def fetchBlock(number: BigInt): Future[Block] = {
    val withTransactions = number >= synchronizerConfig.syncTransactionsFromBlock

    ethService.getBlock(number, withTransactions)
  }
}
