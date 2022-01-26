package io.stakenet.eth.explorer.services

import com.google.inject.Inject
import io.stakenet.eth.explorer.models.Block
import io.stakenet.eth.explorer.repository.blocks.BlocksRepository

import scala.concurrent.Future

class BlocksService @Inject() (blocksRepository: BlocksRepository.FutureImpl) {

  def getLatest(): Future[Option[Block]] = {
    blocksRepository.getLatestBlock()
  }
}
