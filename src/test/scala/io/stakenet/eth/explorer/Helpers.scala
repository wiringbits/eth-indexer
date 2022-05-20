package io.stakenet.eth.explorer

import io.stakenet.eth.explorer.executors.{BlockingExecutionContext, DatabaseExecutionContext}
import io.stakenet.eth.explorer.models.{Block, Transaction, TransactionStatus}

import scala.concurrent.ExecutionContext
import scala.util.Random

object Helpers {

  def randomBlock(): Block.WithoutTransactions = {
    Block.WithoutTransactions(
      number = Random.nextInt(),
      hash = randomHash(),
      parentHash = randomHash(),
      nonce = Random.nextInt(),
      sha3Uncles = randomHash(),
      transactionsRoot = randomHash(),
      stateRoot = randomHash(),
      receiptRoot = randomHash(),
      author = Some(""),
      miner = "",
      mixHash = randomHash(),
      difficulty = Random.nextInt(),
      totalDifficulty = Random.nextInt(),
      extraData = randomHash(),
      size = Random.nextInt(),
      gasLimit = Random.nextInt(),
      gasUsed = Random.nextInt(),
      timestamp = Random.nextInt()
    )
  }

  def randomTransaction(): Transaction = {
    Transaction(
      hash = randomHash(),
      nonce = Random.nextInt(),
      blockHash = randomHash(),
      blockNumber = Random.nextInt(),
      transactionIndex = Random.nextInt(),
      from = randomHash(),
      to = Some(randomHash()),
      value = Random.nextInt(),
      gasPrice = Random.nextInt(),
      gas = Random.nextInt(),
      input = randomHash(),
      creates = Some(randomHash()),
      publicKey = Some(randomHash()),
      raw = Some(randomHash()),
      timestamp = Random.nextInt(),
      status = Some(TransactionStatus.Success),
      confirmations = 0
    )
  }

  def randomHash(): String = {
    s"0x${Random.alphanumeric.take(64).mkString.toLowerCase}"
  }

  object Executors {
    implicit val globalEC: ExecutionContext = scala.concurrent.ExecutionContext.global

    implicit val blockingEC: BlockingExecutionContext = new BlockingExecutionContext {
      override def execute(runnable: Runnable): Unit = globalEC.execute(runnable)

      override def reportFailure(cause: Throwable): Unit = globalEC.reportFailure(cause)
    }

    implicit val databaseEC: DatabaseExecutionContext = new DatabaseExecutionContext {
      override def execute(runnable: Runnable): Unit = globalEC.execute(runnable)

      override def reportFailure(cause: Throwable): Unit = globalEC.reportFailure(cause)
    }
  }
}
