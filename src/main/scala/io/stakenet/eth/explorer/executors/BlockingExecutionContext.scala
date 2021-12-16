package io.stakenet.eth.explorer.executors

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.CustomExecutionContext

import scala.concurrent.ExecutionContext

trait BlockingExecutionContext extends ExecutionContext

object BlockingExecutionContext {

  @Singleton
  class AkkaBased @Inject() (system: ActorSystem)
      extends CustomExecutionContext(system, "blocking.dispatcher")
      with BlockingExecutionContext
}
