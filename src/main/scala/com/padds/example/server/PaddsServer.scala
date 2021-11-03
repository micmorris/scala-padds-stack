package com.padds.example.server

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.stream.Materializer
import com.padds.example.config.ServiceConfig
import com.padds.example.metrics.PrometheusMetrics
import com.padds.example.routes.PaddsRoutes
import com.padds.example.service.PaddsOperationService
import fr.davit.akka.http.metrics.core.HttpMetrics._
import fr.davit.akka.http.metrics.prometheus.PrometheusRegistry

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps

object PaddsServer extends App with PaddsRoutes with ServiceConfig {

  implicit val system: ActorSystem = ActorSystem("PaddsServer")
  implicit val materializer: Materializer = Materializer.matFromSystem
  implicit val ec: ExecutionContext = ExecutionContext.global
  override val log = Logging(system, this.getClass)

  override val metricsRegistry: PrometheusRegistry = PrometheusMetrics()

  override val paddsOperationService: PaddsOperationService = new PaddsOperationService()

  implicit def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      // The exceptions should basically all map to 500
      case ex: Exception =>
        val message =
          s"Encountered exception [[${ex.getClass.toGenericString}]] with message [[${ex.getMessage}]]"
        log.warning(message)
        complete(HttpResponse(StatusCodes.InternalServerError, entity = message))
    }

  val binding = Http()
    .newMeteredServerAt(
      interface = hostConfig.listeningHost.host,
      port = hostConfig.listeningHost.port,
      metricsHandler = metricsRegistry
    )
    .bindFlow(routes)

  val shutdown = CoordinatedShutdown(system)

  shutdown.addTask(CoordinatedShutdown.PhaseServiceUnbind, "http-unbind") { () =>
    log.info("Unbinding HTTP to let in-flight content finish...")
    binding.flatMap(_.unbind()).map(_ => Done)
  }

  shutdown.addTask(CoordinatedShutdown.PhaseServiceRequestsDone, "http-graceful-termination") { () =>
    log.info("Waiting for in-flight content to finish...")
    binding
      .flatMap(b => akka.pattern.after(5 seconds, system.scheduler)(b.terminate(1 minute)))
      .map(_ => Done)
  }

  log.info(s"Server online at http://${hostConfig.listeningHost.host}:${hostConfig.listeningHost.port}/")

  Await.result(system.whenTerminated, Duration.Inf)

  println("Server terminated successfully.")

}
