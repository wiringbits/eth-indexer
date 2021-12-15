package io.stakenet.eth.explorer.modules

import io.stakenet.eth.explorer.tasks.TransactionStatusSynchronizerTask
import play.api.inject.{SimpleModule, bind}

class TransactionStatusSynchronizerModule extends SimpleModule(bind[TransactionStatusSynchronizerTask].toSelf.eagerly())
