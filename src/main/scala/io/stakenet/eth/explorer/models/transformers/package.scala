package io.stakenet.eth.explorer.models

import org.web3j.protocol.core.methods.response
import org.web3j.protocol.core.methods.response.EthBlock

package object transformers {

  def toBlock(ethBlock: EthBlock.Block): Block.WithoutTransactions = {
    Block.WithoutTransactions(
      number = ethBlock.getNumber,
      hash = ethBlock.getHash,
      parentHash = ethBlock.getParentHash,
      nonce = ethBlock.getNonce,
      sha3Uncles = ethBlock.getSha3Uncles,
      transactionsRoot = ethBlock.getTransactionsRoot,
      stateRoot = ethBlock.getStateRoot,
      receiptRoot = ethBlock.getReceiptsRoot,
      author = Option(ethBlock.getAuthor),
      miner = ethBlock.getMiner,
      mixHash = ethBlock.getMixHash,
      difficulty = ethBlock.getDifficulty,
      totalDifficulty = ethBlock.getTotalDifficulty,
      extraData = ethBlock.getExtraData,
      size = ethBlock.getSize,
      gasLimit = ethBlock.getGasLimit,
      gasUsed = ethBlock.getGasUsed,
      timestamp = ethBlock.getTimestamp
    )
  }

  def toTransaction(
      transaction: response.Transaction,
      receiptMaybe: Option[response.TransactionReceipt],
      timestamp: BigInt
  ): Transaction = {
    val status = receiptMaybe map { receipt =>
      if (receipt.isStatusOK) TransactionStatus.Success else TransactionStatus.Fail
    }

    Transaction(
      hash = transaction.getHash,
      nonce = transaction.getNonce,
      blockHash = transaction.getBlockHash,
      blockNumber = transaction.getBlockNumber,
      transactionIndex = transaction.getTransactionIndex,
      from = transaction.getFrom,
      to = Option(transaction.getTo),
      value = transaction.getValue,
      gasPrice = transaction.getGasPrice,
      gas = transaction.getGas,
      input = transaction.getInput,
      creates = Option(transaction.getCreates),
      publicKey = Option(transaction.getPublicKey),
      raw = Option(transaction.getRaw),
      timestamp = timestamp,
      status = status
    )
  }
}
