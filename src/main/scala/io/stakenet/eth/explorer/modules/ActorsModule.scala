package io.stakenet.eth.explorer.modules

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import io.stakenet.eth.explorer.tasks.currencySynchronizer.CurrencySynchronizerActor
import javax.inject.Singleton

class ActorsModule extends AbstractModule {

  @Provides
  @Singleton
  def EthSynchronizer()(implicit actorSystem: ActorSystem): CurrencySynchronizerActor.EthRef = {
    CurrencySynchronizerActor.EthRef()
  }

  @Provides
  @Singleton
  def WethSynchronizer()(implicit actorSystem: ActorSystem): CurrencySynchronizerActor.WethRef = {
    CurrencySynchronizerActor.WethRef()
  }

  @Provides
  @Singleton
  def UsdtSynchronizer()(implicit actorSystem: ActorSystem): CurrencySynchronizerActor.UsdtRef = {
    CurrencySynchronizerActor.UsdtRef()
  }
}
