package io.stakenet.eth.explorer.modules

import com.google.inject.AbstractModule
import io.stakenet.eth.explorer.services.{CurrencyService, CurrencyServiceCoinMarketCapImpl}

class CurrencyServiceModule extends AbstractModule {

  override def configure(): Unit = {
    val _ = bind(classOf[CurrencyService]).to(classOf[CurrencyServiceCoinMarketCapImpl])
  }
}
