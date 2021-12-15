package io.stakenet.eth.explorer.repositories.blocks

import io.stakenet.eth.explorer.repository.blocks.BlocksPostgresRepository
import io.stakenet.eth.explorer.{DatabaseSpec, Helpers}
import org.postgresql.util.PSQLException
import org.scalatest.OptionValues._
import org.scalatest.wordspec.AnyWordSpec

class BlocksPostgresRepositorySpec extends AnyWordSpec with DatabaseSpec {
  private lazy val repository = new BlocksPostgresRepository(database)

  "create" should {
    "create a block" in {
      val block = Helpers.randomBlock()

      repository.create(block)

      succeed
    }

    "create a block with transactions" in {
      val block = Helpers.randomBlock()
      val transactions = List(
        Helpers.randomTransaction().copy(blockNumber = block.number, blockHash = block.hash),
        Helpers.randomTransaction().copy(blockNumber = block.number, blockHash = block.hash),
        Helpers.randomTransaction().copy(blockNumber = block.number, blockHash = block.hash)
      )

      repository.create(block.withTransactions(transactions))

      succeed
    }

    "fail on duplicated block hash" in {
      val block1 = Helpers.randomBlock()
      val block2 = Helpers.randomBlock().copy(hash = block1.hash)

      repository.create(block1)
      val error = intercept[PSQLException] {
        repository.create(block2)
      }

      error.getMessage mustBe s"block ${block1.hash} already exist"
    }

    "fail on duplicated block number" in {
      val block1 = Helpers.randomBlock()
      val block2 = Helpers.randomBlock().copy(number = block1.number)

      repository.create(block1)
      val error = intercept[PSQLException] {
        repository.create(block2)
      }

      error.getMessage mustBe s"block ${block1.number} already exist"
    }

    "fail on duplicated parent" in {
      val block1 = Helpers.randomBlock()
      val block2 = Helpers.randomBlock().copy(parentHash = block1.parentHash)

      repository.create(block1)
      val error = intercept[PSQLException] {
        repository.create(block2)
      }

      error.getMessage mustBe s"block with parent ${block1.parentHash} already exist"
    }

    "fail on duplicated transaction hash" in {
      val block = Helpers.randomBlock()
      val transaction1 = Helpers.randomTransaction().copy(blockNumber = block.number, blockHash = block.hash)
      val transaction2 = Helpers
        .randomTransaction()
        .copy(
          blockNumber = block.number,
          blockHash = block.hash,
          hash = transaction1.hash
        )
      val transactions = List(transaction1, transaction2)

      val error = intercept[PSQLException] {
        repository.create(block.withTransactions(transactions))
      }

      error.getMessage mustBe s"transaction ${transaction1.hash} already exist"

      succeed
    }

    "fail when transaction block number does not exist" in {
      val block = Helpers.randomBlock()
      val transaction = Helpers.randomTransaction().copy(blockHash = block.hash)
      val transactions = List(transaction)

      val error = intercept[PSQLException] {
        repository.create(block.withTransactions(transactions))
      }

      error.getMessage mustBe s"block ${transaction.blockNumber} does not exist"

      succeed
    }
  }

  "getLatestBlock" should {
    "get the block with the highest block number" in {
      val block1 = Helpers.randomBlock().copy(number = 1)
      val block2 = Helpers.randomBlock().copy(number = 2)
      val block3 = Helpers.randomBlock().copy(number = 3)

      repository.create(block1)
      repository.create(block2)
      repository.create(block3)

      val result = repository.getLatestBlock().value

      result mustBe block3
    }

    "get None when there are no blocks" in {
      val result = repository.getLatestBlock()

      result mustBe None
    }
  }

  "removeBlock" should {
    "delete a block" in {
      val block = Helpers.randomBlock()

      repository.create(block)

      val result = repository.removeBlock(block.hash).value
      result mustBe block
    }

    "delete a block with transactions" in {
      val block = Helpers.randomBlock()
      val transactions1 = Helpers.randomTransaction().copy(blockHash = block.hash, blockNumber = block.number)
      val transactions2 = Helpers.randomTransaction().copy(blockHash = block.hash, blockNumber = block.number)

      repository.create(block.withTransactions(List(transactions1, transactions2)))

      val result = repository.removeBlock(block.hash).value
      result mustBe block
    }

    "succeed when block does not exists" in {
      val block = Helpers.randomBlock()

      val result = repository.removeBlock(block.hash)
      result mustBe None
    }
  }
}
