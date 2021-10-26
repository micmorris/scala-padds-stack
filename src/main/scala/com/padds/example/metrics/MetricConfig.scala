package com.padds.example.metrics

import io.prometheus.client.{CollectorRegistry, Summary}

object MetricConfig {

  val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
  private val ROUTES_NAMESPACE = "routes"
  private val Tolerance = 0.001

  lazy val ROUTES_GET_THING_SUMMARY: Summary = Summary
    .build()
    .namespace(ROUTES_NAMESPACE)
    .name("get_thing_duration_seconds")
    .help("Get a thing routed request")
    .quantile(0.5, Tolerance)
    .quantile(0.9, Tolerance)
    .quantile(0.95, Tolerance)
    .quantile(0.99, Tolerance)
    .register(collectorRegistry)

}
