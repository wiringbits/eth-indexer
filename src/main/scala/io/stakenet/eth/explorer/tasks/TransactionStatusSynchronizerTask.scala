package io.stakenet.eth.explorer.tasks

import akka.actor.ActorSystem
import anorm._
import io.stakenet.eth.explorer.config.SynchronizerConfig
import io.stakenet.eth.explorer.models.{Transaction, TransactionStatus}
import javax.inject.Inject
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import play.api.db.Database

import scala.annotation.nowarn
import scala.compat.java8.FutureConverters
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.jdk.OptionConverters._

class TransactionStatusSynchronizerTask @Inject()(
    database: Database,
    synchronizerConfig: SynchronizerConfig,
    ethClient: Web3j
)(implicit ec: ExecutionContext, actorSystem: ActorSystem) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  start()

  def start() = {
    logger.info("starting TransactionStatusSynchronizerTask")
    actorSystem.scheduler.scheduleOnce(5.seconds) {
      run()
    }
  }

  private def run(): Unit = {
    val lastBlock = 12059019
    for (number <- synchronizerConfig.syncTransactionsFromBlock to lastBlock) {
      logger.info(s"updating transactions from block $number")

      getTransactionsByBlockNumber(number).foreach { transaction =>
        FutureConverters
          .toScala(ethClient.ethGetTransactionReceipt(transaction.hash).sendAsync())
          .foreach { receipt =>
            val success = receipt.getTransactionReceipt.toScala
              .getOrElse(throw new RuntimeException(s"Could not get receipt for ${transaction.hash}"))
              .isStatusOK

            val status = if (success) TransactionStatus.Success else TransactionStatus.Fail

            updateStatus(transaction.hash, status)
          }
      }
    }
  }

  private def getTransactionsByBlockNumber(number: BigInt): List[Transaction] = {
    @nowarn
    implicit val transactionStatusColumn: Column[TransactionStatus] = Column.columnToString.map(
      TransactionStatus.withNameInsensitive
    )

    val parser: RowParser[Transaction] = Macro.parser[Transaction](
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
      "status"
    )

    database.withConnection { implicit connection =>
      SQL"""
         SELECT
           t.hash, t.nonce, t.block_hash, t.block_number, t.transaction_index, t.from_address, t.to_address,
           t.value, t.gas_price, t.gas, t.input, t.creates, t.public_key, t.raw, b.time AS timestamp, t.status
         FROM transactions t
         INNER JOIN blocks b USING(block_hash)
         WHERE t.block_number = $number
         ORDER BY b.time DESC, t.hash ASC
       """.as(parser.*)
    }
  }

  private def updateStatus(hash: String, status: TransactionStatus): Unit = {
    database.withConnection { implicit connection =>
      SQL"""
         UPDATE transactions
         SET status = ${status.entryName}::TRANSACTION_STATUS
         WHERE hash = $hash
       """.executeUpdate()

    }

    ()
  }
}
