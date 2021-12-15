package io.stakenet.eth.explorer.modules

import com.google.inject.AbstractModule
import io.stakenet.eth.explorer.services.{ETHService, ETHServiceRPCImpl}

class ETHServiceModule extends AbstractModule {

  override def configure(): Unit = {
    val _ = bind(classOf[ETHService]).to(classOf[ETHServiceRPCImpl])
  }
}
