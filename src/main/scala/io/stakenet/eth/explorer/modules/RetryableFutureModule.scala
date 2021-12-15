package io.stakenet.eth.explorer.modules

import akka.actor.{ActorSystem, Scheduler}
import com.google.inject.{AbstractModule, Provides}
import io.stakenet.eth.explorer.config.RetryConfig

import scala.concurrent.duration._

class RetryableFutureModule extends AbstractModule {

  override def configure(): Unit = {}

  @Provides
  def retryConfig(): RetryConfig = {
    RetryConfig(1.second, 65.seconds)
  }

  @Provides
  def scheduler(actorSystem: ActorSystem): Scheduler = {
    actorSystem.scheduler
  }
}
