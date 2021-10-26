package com.padds.example.metrics

import com.padds.example.metrics.MetricConfig.collectorRegistry
import fr.davit.akka.http.metrics.prometheus.{PrometheusRegistry, PrometheusSettings}

object PrometheusMetrics {

  def apply(): PrometheusRegistry =
    PrometheusRegistry(
      settings = PrometheusSettings.default
        .withIncludeStatusDimension(true)
        .withIncludeMethodDimension(true),
      underlying = collectorRegistry
    )

}
