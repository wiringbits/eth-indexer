package io.stakenet.eth.explorer.modules

import io.stakenet.eth.explorer.tasks.currencySynchronizer.CurrencySynchronizerTask
import play.api.inject.{SimpleModule, bind}

class CurrencySynchronizerModule extends SimpleModule(bind[CurrencySynchronizerTask].toSelf.eagerly())
