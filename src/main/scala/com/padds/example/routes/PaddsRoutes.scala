package com.padds.example.routes

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.padds.example.service.PaddsService
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._
import fr.davit.akka.http.metrics.prometheus.PrometheusRegistry
import fr.davit.akka.http.metrics.prometheus.marshalling.PrometheusMarshallers.marshaller

trait PaddsRoutes {

  implicit def system: ActorSystem
  val log: LoggingAdapter
  val paddsService: PaddsService
  val metricsRegistry: PrometheusRegistry

  lazy val routes: Route = {
    concat(
      (get & path("http-metrics")) {
        metrics(metricsRegistry)
      }
    )
  }
}
