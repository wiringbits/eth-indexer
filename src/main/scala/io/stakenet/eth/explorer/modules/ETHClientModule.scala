package io.stakenet.eth.explorer.modules

import com.google.inject.{AbstractModule, Provides}
import io.stakenet.eth.explorer.config.ETHConfig
import javax.inject.Singleton
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

class ETHClientModule extends AbstractModule {

  @Provides
  @Singleton
  def ethClient(ethConfig: ETHConfig): Web3j = {
    Web3j.build(new HttpService(ethConfig.url))
  }
}
