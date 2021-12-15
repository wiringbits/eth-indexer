package io.stakenet.eth.explorer.modules

import com.google.inject.AbstractModule
import io.stakenet.eth.explorer.repository.blocks.{BlocksPostgresRepository, BlocksRepository}
import io.stakenet.eth.explorer.repository.transactions.{TransactionsPostgresRepository, TransactionsRepository}

class RepositoriesModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[BlocksRepository.Blocking]).to(classOf[BlocksPostgresRepository])
    bind(classOf[TransactionsRepository.Blocking]).to(classOf[TransactionsPostgresRepository])

    ()
  }
}
