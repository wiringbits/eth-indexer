package io.stakenet.eth.explorer.models

import scala.util.Try

case class Transaction(
    hash: String,
    nonce: BigInt,
    blockHash: String,
    blockNumber: BigInt,
    transactionIndex: BigInt,
    from: String,
    to: Option[String],
    value: BigInt,
    gasPrice: BigInt,
    gas: BigInt,
    input: String,
    creates: Option[String],
    publicKey: Option[String],
    raw: Option[String],
    timestamp: BigInt,
    status: Option[TransactionStatus],
    confirmations: BigInt
) {

  // see https://medium.com/swlh/understanding-data-payloads-in-ethereum-transactions-354dbe995371 for more information
  def tokenTransferRecipient: Option[String] = {
    // method id is the first 4 bytes from the Keccak-256 hash of the method signature, for ERC20 contracts the transfer
    // method has the following signature "transfer(address, uint256)" which is hashed to:
    // "0xa9059cbb2ab09eb219583f4a59a5d0623ade346d962bcd4e46b11da047c9049b"
    val tokenTransferMethodId = "a9059cbb"
    val hexData = input.substring(2) // remove 0x from the string

    for {
      // first 4 bytes of input is the called method id
      _ <- Try(hexData.substring(0, 8)).toOption.filter(_ == tokenTransferMethodId)

      // after the method id parameters are encoded in 32 bytes blocks so the recipient address would be the next
      // 32 bytes after the method id, since addresses have only 20 bytes we skip the first 12 bytes
      address <- Try(hexData.substring(8 + 24, 8 + 24 + 40)).toOption
    } yield s"0x$address"
  }
}
