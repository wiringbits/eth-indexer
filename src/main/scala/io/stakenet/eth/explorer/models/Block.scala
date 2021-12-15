package io.stakenet.eth.explorer.models

trait Block {
  def number: BigInt
  def hash: String
  def parentHash: String
  def nonce: BigInt
  def sha3Uncles: String
  def transactionsRoot: String
  def stateRoot: String
  def receiptRoot: String
  def author: Option[String]
  def miner: String
  def mixHash: String
  def difficulty: BigInt
  def totalDifficulty: BigInt
  def extraData: String
  def size: BigInt
  def gasLimit: BigInt
  def gasUsed: BigInt
  def timestamp: BigInt
}

object Block {
  case class WithoutTransactions(
      number: BigInt,
      hash: String,
      parentHash: String,
      nonce: BigInt,
      sha3Uncles: String,
      transactionsRoot: String,
      stateRoot: String,
      receiptRoot: String,
      author: Option[String],
      miner: String,
      mixHash: String,
      difficulty: BigInt,
      totalDifficulty: BigInt,
      extraData: String,
      size: BigInt,
      gasLimit: BigInt,
      gasUsed: BigInt,
      timestamp: BigInt
  ) extends Block {

    def withTransactions(transactions: List[Transaction]): Block.WithTransactions = {
      Block.WithTransactions(this, transactions)
    }
  }

  case class WithTransactions(
      private val block: Block.WithoutTransactions,
      transactions: List[Transaction]
  ) extends Block {
    require(
      transactions.forall(_.blockHash == hash),
      s"transactions ${transactions.filter(_.blockHash != hash)} do not belong to block $hash"
    )

    def number: BigInt = block.number
    def hash: String = block.hash
    def parentHash: String = block.parentHash
    def nonce: BigInt = block.nonce
    def sha3Uncles: String = block.sha3Uncles
    def transactionsRoot: String = block.transactionsRoot
    def stateRoot: String = block.stateRoot
    def receiptRoot: String = block.receiptRoot
    def author: Option[String] = block.author
    def miner: String = block.miner
    def mixHash: String = block.mixHash
    def difficulty: BigInt = block.difficulty
    def totalDifficulty: BigInt = block.totalDifficulty
    def extraData: String = block.extraData
    def size: BigInt = block.size
    def gasLimit: BigInt = block.gasLimit
    def gasUsed: BigInt = block.gasUsed
    def timestamp: BigInt = block.timestamp
  }
}
