package io.stakenet.eth.explorer.services

import anorm._
import io.stakenet.eth.explorer.config.SynchronizerConfig
import io.stakenet.eth.explorer.models.Block
import io.stakenet.eth.explorer.repository.blocks
import io.stakenet.eth.explorer.repository.blocks.{BlockParsers, BlocksPostgresRepository}
import io.stakenet.eth.explorer.{DatabaseSpec, Helpers}
import org.mockito.MockitoSugar.{mock, when}
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.scalatest.Assertion
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.{ExecutionContext, Future}

class SynchronizerServiceSpec extends AsyncWordSpec with DatabaseSpec {
  // both AsyncWordSpec and DatabaseSpec have an implicit execution context, to avoid ambiguity
  // we declare a local execution context to be used(since local implicit variables are preferred)
  implicit val ec: ExecutionContext = executionContext

  s"synchronize" should {
    "add the old missing blocks while adding block N to the empty ledger" in {
      val ethService = mock[ETHService]
      val synchronizer = getService(ethService)
      val block1 = Helpers.randomBlock().copy(number = 0)
      val block2 = Helpers.randomBlock().copy(number = 1, parentHash = block1.hash)
      val block3 = Helpers.randomBlock().copy(number = 2, parentHash = block2.hash)
      val block4 = Helpers.randomBlock().copy(number = 3, parentHash = block3.hash)
      val block5 = Helpers.randomBlock().copy(number = 4, parentHash = block4.hash)

      val blocks = List(block1, block2, block3, block4, block5)
      blocks.foreach { block =>
        when(ethService.getBlock(eqTo(block.number), any[Boolean])).thenReturn(Future.successful(block))
      }

      synchronizer.synchronize(4).map { _ =>
        verifyDatabase(blocks)
      }
    }

    "append a block to the latest block" in {
      val ethService = mock[ETHService]
      val synchronizer = getService(ethService)
      val block1 = Helpers.randomBlock().copy(number = 0)
      val block2 = Helpers.randomBlock().copy(number = 1, parentHash = block1.hash)
      val block3 = Helpers.randomBlock().copy(number = 2, parentHash = block2.hash)
      val block4 = Helpers.randomBlock().copy(number = 3, parentHash = block3.hash)
      val block5 = Helpers.randomBlock().copy(number = 4, parentHash = block4.hash)

      val blocks = List(block1, block2, block3, block4, block5)
      blocks.foreach { block =>
        when(ethService.getBlock(eqTo(block.number), any[Boolean])).thenReturn(Future.successful(block))
      }

      for {
        _ <- synchronizer.synchronize(3)
        _ <- synchronizer.synchronize(4)
      } yield verifyDatabase(blocks)
    }

    "ignore a duplicated block" in {
      val ethService = mock[ETHService]
      val synchronizer = getService(ethService)
      val block1 = Helpers.randomBlock().copy(number = 0)
      val block2 = Helpers.randomBlock().copy(number = 1, parentHash = block1.hash)
      val block3 = Helpers.randomBlock().copy(number = 2, parentHash = block2.hash)
      val block4 = Helpers.randomBlock().copy(number = 3, parentHash = block3.hash)
      val block5 = Helpers.randomBlock().copy(number = 4, parentHash = block4.hash)

      val blocks = List(block1, block2, block3, block4, block5)
      blocks.foreach { block =>
        when(ethService.getBlock(eqTo(block.number), any[Boolean])).thenReturn(Future.successful(block))
      }

      for {
        _ <- synchronizer.synchronize(4)
        _ <- synchronizer.synchronize(4)
      } yield verifyDatabase(blocks)
    }

    "add the old missing blocks  blocks while adding block N to a ledger with some blocks" in {
      val ethService = mock[ETHService]
      val synchronizer = getService(ethService)
      val block1 = Helpers.randomBlock().copy(number = 0)
      val block2 = Helpers.randomBlock().copy(number = 1, parentHash = block1.hash)
      val block3 = Helpers.randomBlock().copy(number = 2, parentHash = block2.hash)
      val block4 = Helpers.randomBlock().copy(number = 3, parentHash = block3.hash)
      val block5 = Helpers.randomBlock().copy(number = 4, parentHash = block4.hash)
      val blocks = List(block1, block2, block3, block4, block5)

      blocks.foreach { block =>
        when(ethService.getBlock(eqTo(block.number), any[Boolean])).thenReturn(Future.successful(block))
      }

      for {
        _ <- synchronizer.synchronize(1)
        _ <- synchronizer.synchronize(4)
      } yield verifyDatabase(blocks)
    }

    "handle reorganization, ledger has 3 blocks, a rechain occurs from block 2 while adding new block 3" in {
      val ethService = mock[ETHService]
      val synchronizer = getService(ethService)
      val block1 = Helpers.randomBlock().copy(number = 0)
      val block2 = Helpers.randomBlock().copy(number = 1, parentHash = block1.hash)
      val block3 = Helpers.randomBlock().copy(number = 2, parentHash = block2.hash)
      val newBlock2 = Helpers.randomBlock().copy(number = 1, parentHash = block1.hash)
      val newBlock3 = Helpers.randomBlock().copy(number = 2, parentHash = newBlock2.hash)
      val blocks = List(block1, newBlock2, newBlock3)

      when(ethService.getBlock(eqTo(0), any[Boolean])).thenReturn(Future.successful(block1))
      when(ethService.getBlock(eqTo(1), any[Boolean]))
        .thenReturn(Future.successful(block2), Future.successful(newBlock2))
      when(ethService.getBlock(eqTo(2), any[Boolean]))
        .thenReturn(Future.successful(block3), Future.successful(newBlock3))

      for {
        _ <- synchronizer.synchronize(2)
        _ <- synchronizer.synchronize(2)
      } yield verifyDatabase(blocks)
    }

    "handle reorganization, ledger has 3 blocks, a rechain occurs from block 2 while adding new block 4" in {
      val ethService = mock[ETHService]
      val synchronizer = getService(ethService)
      val block1 = Helpers.randomBlock().copy(number = 0)
      val block2 = Helpers.randomBlock().copy(number = 1, parentHash = block1.hash)
      val block3 = Helpers.randomBlock().copy(number = 2, parentHash = block2.hash)
      val newBlock2 = Helpers.randomBlock().copy(number = 1, parentHash = block1.hash)
      val newBlock3 = Helpers.randomBlock().copy(number = 2, parentHash = newBlock2.hash)
      val newBlock4 = Helpers.randomBlock().copy(number = 3, parentHash = newBlock3.hash)

      val blocks = List(block1, newBlock2, newBlock3, newBlock4)

      when(ethService.getBlock(eqTo(0), any[Boolean])).thenReturn(Future.successful(block1))
      when(ethService.getBlock(eqTo(1), any[Boolean]))
        .thenReturn(Future.successful(block2), Future.successful(newBlock2))
      when(ethService.getBlock(eqTo(2), any[Boolean]))
        .thenReturn(Future.successful(block3), Future.successful(newBlock3))
      when(ethService.getBlock(eqTo(3), any[Boolean])).thenReturn(Future.successful(newBlock4))

      for {
        _ <- synchronizer.synchronize(2)
        _ <- synchronizer.synchronize(3)
      } yield verifyDatabase(blocks)
    }

    "handle reorganization, ledger has 6 blocks, a rechain occurs from block 2 while adding new block 2" in {
      val ethService = mock[ETHService]
      val synchronizer = getService(ethService)
      val block1 = Helpers.randomBlock().copy(number = 0)
      val block2 = Helpers.randomBlock().copy(number = 1, parentHash = block1.hash)
      val block3 = Helpers.randomBlock().copy(number = 2, parentHash = block2.hash)
      val block4 = Helpers.randomBlock().copy(number = 3, parentHash = block3.hash)
      val block5 = Helpers.randomBlock().copy(number = 4, parentHash = block4.hash)
      val block6 = Helpers.randomBlock().copy(number = 5, parentHash = block5.hash)
      val newBlock2 = Helpers.randomBlock().copy(number = 1, parentHash = block1.hash)

      val blocks = List(block1, newBlock2)

      when(ethService.getBlock(eqTo(0), any[Boolean])).thenReturn(Future.successful(block1))
      when(ethService.getBlock(eqTo(1), any[Boolean]))
        .thenReturn(Future.successful(block2), Future.successful(newBlock2))
      when(ethService.getBlock(eqTo(2), any[Boolean])).thenReturn(Future.successful(block3))
      when(ethService.getBlock(eqTo(3), any[Boolean])).thenReturn(Future.successful(block4))
      when(ethService.getBlock(eqTo(4), any[Boolean])).thenReturn(Future.successful(block5))
      when(ethService.getBlock(eqTo(5), any[Boolean])).thenReturn(Future.successful(block6))

      for {
        _ <- synchronizer.synchronize(5)
        _ <- synchronizer.synchronize(1)
      } yield verifyDatabase(blocks)
    }
  }

  private def getService(ethService: ETHService): SynchronizerService = {
    val blocksRepository = new BlocksPostgresRepository(database)
    val asyncBlocksPostgresRepository =
      new blocks.BlocksRepository.FutureImpl(blocksRepository)(Helpers.Executors.databaseEC)
    val synchronizerConfig = SynchronizerConfig(BigInt(0))

    new SynchronizerService(ethService, asyncBlocksPostgresRepository, synchronizerConfig)
  }

  private def verifyDatabase(expected: List[Block]): Assertion = {
    val current = database.withConnection { implicit connection =>
      SQL"""
           SELECT * FROM blocks ORDER BY number ASC
         """.as(BlockParsers.blockWithoutTransactionsParser.*)
    }

    current.map(_.hash) mustBe expected.map(_.hash)
  }
}
