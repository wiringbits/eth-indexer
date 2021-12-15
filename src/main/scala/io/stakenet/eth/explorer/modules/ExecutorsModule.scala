package io.stakenet.eth.explorer.modules

import com.google.inject.AbstractModule
import io.stakenet.eth.explorer.executors.{BlockingExecutionContext, DatabaseExecutionContext}

class ExecutorsModule extends AbstractModule {
  override def configure(): Unit = {
    val _ = (
      bind(classOf[BlockingExecutionContext]).to(classOf[BlockingExecutionContext.AkkaBased]),
      bind(classOf[DatabaseExecutionContext]).to(classOf[DatabaseExecutionContext.AkkaBased])
    )
  }
}
