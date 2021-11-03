package com.padds.example.config

object PureConfigModel {

  case class HostConfig(
      listeningHost: HostPortPair
  )

  case class HostPortPair(
      host: String,
      port: Int
  )

}
