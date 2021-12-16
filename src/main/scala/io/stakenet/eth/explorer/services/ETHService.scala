package io.stakenet.eth.explorer.services

import io.stakenet.eth.explorer.executors.BlockingExecutionContext
import io.stakenet.eth.explorer.models.Block
import io.stakenet.eth.explorer.models.transformers.{toBlock, toTransaction}
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response
import org.web3j.protocol.core.methods.response.{EthBlock, TransactionReceipt}

import java.math.BigInteger
import javax.inject.Inject
import scala.compat.java8.FutureConverters
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOptional
import scala.util.Try

trait ETHService {

  def getLatestBlockNumber(): Future[BigInt]
  def getBlock(number: BigInt, withTransactions: Boolean): Future[Block]

}

class ETHServiceRPCImpl @Inject() (ethClient: Web3j)(implicit ec: BlockingExecutionContext) extends ETHService {

  override def getLatestBlockNumber(): Future[BigInt] = {
    FutureConverters
      .toScala(ethClient.ethBlockNumber().sendAsync())
      .map { result =>
        (Try(Option(result.getBlockNumber)).toOption.flatten, Option(result.getError)) match {
          case (Some(number), _) =>
            number

          case (_, Some(error)) =>
            throw new ETHService.Error.CouldNotGetLatestBlockNumber(error.getCode, error.getMessage)

          case _ =>
            throw new ETHService.Error.UnexpectedResponse()
        }
      }
  }

  override def getBlock(number: BigInt, withTransactions: Boolean): Future[Block] = {
    getEthBlock(number, withTransactions).flatMap { ethBlock =>
      val block = Try(toBlock(ethBlock)).getOrElse(throw new ETHService.Error.CouldNotParseBlock(number))

      Try(ethBlock.getTransactions.asScala.map(_.asInstanceOf[response.Transaction]).toList).toOption match {
        case Some(ethTransactions) =>
          val transactions = ethTransactions.map { transaction =>
            getTransactionReceipt(transaction.getHash)
              .map(toTransaction(transaction, _, block.timestamp))
          }

          Future.sequence(transactions).map(block.withTransactions)

        case None =>
          Future.successful(block)
      }
    }
  }

  private def getEthBlock(number: BigInt, withTransactions: Boolean): Future[EthBlock.Block] = {
    val blockNumber = DefaultBlockParameter.valueOf(new BigInteger(number.toString))

    FutureConverters.toScala(ethClient.ethGetBlockByNumber(blockNumber, withTransactions).sendAsync()).map { result =>
      (Option(result.getBlock), Option(result.getError)) match {
        case (Some(ethBlock), _) =>
          ethBlock

        case (_, Some(error)) =>
          throw new ETHService.Error.CouldNotGetBlock(number, error.getCode, error.getMessage)

        case _ =>
          throw new ETHService.Error.UnexpectedResponse()
      }
    }
  }

  private def getTransactionReceipt(hash: String): Future[Option[TransactionReceipt]] = {
    FutureConverters.toScala(ethClient.ethGetTransactionReceipt(hash).sendAsync()).map { receipt =>
      receipt.getTransactionReceipt.toScala
    }
  }
}

object ETHService {

  object Error {

    class CouldNotGetLatestBlockNumber(code: Int, message: String) extends RuntimeException {

      override def getMessage: String = {
        s"An error occurred getting latest block number: $message(error code: $code)"
      }
    }

    class CouldNotGetBlock(number: BigInt, code: Int, message: String) extends RuntimeException {

      override def getMessage: String = {
        s"An error occurred getting block $number: $message(error code: $code)"
      }
    }

    class CouldNotParseBlock(number: BigInt) extends RuntimeException {

      override def getMessage: String = {
        s"An error occurred parsing block $number"
      }
    }

    class UnexpectedResponse() extends RuntimeException {

      override def getMessage: String = {
        s"Unexpected response"
      }
    }

    class TransactionReceiptNotFound(transactionHash: String) extends RuntimeException {

      override def getMessage: String = {
        s"Transaction receipt for $transactionHash not found"
      }
    }
  }
}
