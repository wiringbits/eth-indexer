package io.stakenet.eth.explorer.modules

import io.stakenet.eth.explorer.tasks.PollerSynchronizerTask
import play.api.inject.{SimpleModule, bind}

class PollerSynchronizerModule extends SimpleModule(bind[PollerSynchronizerTask].toSelf.eagerly())
