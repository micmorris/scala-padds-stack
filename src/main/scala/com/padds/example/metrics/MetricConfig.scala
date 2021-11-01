package com.padds.example.metrics

import io.prometheus.client.{CollectorRegistry, Summary}

object MetricConfig {

  val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
  private val Tolerance = 0.001

  object GuardrailMetrics {

    private val ROUTES_GUARDRAIL_NAMESPACE = "routes_guardrail"

    lazy val GET_PADDING_ORDERS_SUMMARY: Summary = Summary
      .build()
      .namespace(ROUTES_GUARDRAIL_NAMESPACE)
      .name("get_padding_orders_duration_seconds")
      .help("Get padding orders routed request")
      .quantile(0.5, Tolerance)
      .quantile(0.9, Tolerance)
      .quantile(0.95, Tolerance)
      .quantile(0.99, Tolerance)
      .register(collectorRegistry)

    lazy val ORDER_PADDING_SUMMARY: Summary = Summary
      .build()
      .namespace(ROUTES_GUARDRAIL_NAMESPACE)
      .name("order_padding_duration_seconds")
      .help("Order padding routed request")
      .quantile(0.5, Tolerance)
      .quantile(0.9, Tolerance)
      .quantile(0.95, Tolerance)
      .quantile(0.99, Tolerance)
      .register(collectorRegistry)

  }

  object AkkaMetrics {

    private val ROUTES_AKKA_NAMESPACE = "routes_akka"

    lazy val GET_PADDING_ORDERS_SUMMARY: Summary = Summary
      .build()
      .namespace(ROUTES_AKKA_NAMESPACE)
      .name("get_padding_orders_duration_seconds")
      .help("Get padding orders routed request")
      .quantile(0.5, Tolerance)
      .quantile(0.9, Tolerance)
      .quantile(0.95, Tolerance)
      .quantile(0.99, Tolerance)
      .register(collectorRegistry)

    lazy val ORDER_PADDING_SUMMARY: Summary = Summary
      .build()
      .namespace(ROUTES_AKKA_NAMESPACE)
      .name("order_padding_duration_seconds")
      .help("Order padding routed request")
      .quantile(0.5, Tolerance)
      .quantile(0.9, Tolerance)
      .quantile(0.95, Tolerance)
      .quantile(0.99, Tolerance)
      .register(collectorRegistry)
  }

}
