package com.padds.example.config

import com.padds.example.config.PureConfigModel.HostConfig
import pureconfig.ConfigSource
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._

trait ServiceConfig {

  implicit val configHint: ProductHint[Any] =
    ProductHint[Any](useDefaultArgs = false, allowUnknownKeys = false)

  val hostConfig: HostConfig =
    ConfigSource.default.at("padds.host-config").loadOrThrow[HostConfig]

}
