package io.stakenet.eth.explorer.executors

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.CustomExecutionContext

import scala.concurrent.ExecutionContext

trait DatabaseExecutionContext extends ExecutionContext

object DatabaseExecutionContext {

  @Singleton
  class AkkaBased @Inject() (system: ActorSystem)
      extends CustomExecutionContext(system, "database.dispatcher")
      with DatabaseExecutionContext
}
