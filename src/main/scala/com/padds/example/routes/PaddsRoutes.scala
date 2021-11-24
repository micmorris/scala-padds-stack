package com.padds.example.routes

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.padds.example.json.ProtoJsonProtocol
import com.padds.example.metrics.MetricConfig._
import com.padds.example.metrics.MetricTiming
import com.padds.example.proto.v1.request.{
  GetPaddingOrdersRequest => ScalaPbGetPaddingOrdersRequest,
  OrderPaddingForPlayerRequest => ScalaPbOrderPaddingForPlayerRequest
}
import com.padds.example.proto.v1.response.{
  GetPaddingOrdersResponse => ScalaPbGetPaddingOrdersResponse,
  OrderContent => ScalaPbOrderContent,
  OrderPaddingForPlayerResponse => ScalaPbOrderPaddingForPlayerResponse,
  OrderSuccess => ScalaPbOrderSuccess
}
import com.padds.example.service.PaddsOperationService
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._
import fr.davit.akka.http.metrics.prometheus.PrometheusRegistry
import fr.davit.akka.http.metrics.prometheus.marshalling.PrometheusMarshallers.marshaller
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait PaddsRoutes extends MetricTiming with ProtoJsonProtocol {

  implicit def system: ActorSystem
  val log: LoggingAdapter
  val paddsOperationService: PaddsOperationService
  val metricsRegistry: PrometheusRegistry

  lazy val routes: Route = {
    concat(
      akkaHttpRoutes,
      path("http-metrics") {
        get {
          metrics(metricsRegistry)
        }
      }
    )
  }

  val akkaHttpRoutes: Route =
    extractExecutionContext { implicit ec =>
      pathPrefix("v1") {
        concat(
          path("order" / "padding") {
            post {
              entity(as[ScalaPbOrderPaddingForPlayerRequest]) { orderPaddingForPlayerRequest =>
                time(
                  ORDER_PADDING_SUMMARY.observe,
                  akkaComplete(
                    paddsOperationService
                      .orderPadding(
                        playerId = UUID.fromString(orderPaddingForPlayerRequest.playerId),
                        paddingId = UUID.fromString(orderPaddingForPlayerRequest.paddingId),
                        quantity = orderPaddingForPlayerRequest.qty
                      )
                      .map(internalOrder =>
                        ScalaPbOrderPaddingForPlayerResponse(
                          responseOption =
                            ScalaPbOrderPaddingForPlayerResponse.ResponseOption.OrderSuccessResponse(
                              ScalaPbOrderSuccess(
                                Option(internalOrder.toScalaPbModel)
                              )
                            )
                        )
                      )
                  )
                )
              }
            }
          },
          path("orderId" / Segment) { orderId =>
            get {
              time(
                GET_PADDING_ORDERS_SUMMARY.observe,
                akkaComplete(
                  paddsOperationService
                    .getPaddingOrders(
                      orderIds = List(UUID.fromString(orderId))
                    )
                    .map(internalOrders =>
                      ScalaPbGetPaddingOrdersResponse(
                        responseOption = ScalaPbGetPaddingOrdersResponse.ResponseOption.OrderContentResponse(
                          ScalaPbOrderContent(internalOrders.map(_.toScalaPbModel))
                        )
                      )
                    )
                )
              )
            }
          },
          path("order" / "lookup") {
            post {
              entity(as[ScalaPbGetPaddingOrdersRequest]) { getPaddingOrdersRequest =>
                time(
                  GET_PADDING_ORDERS_SUMMARY.observe,
                  akkaComplete(
                    paddsOperationService
                      .getPaddingOrders(
                        orderIds = getPaddingOrdersRequest.orderIds
                          .map(UUID.fromString)
                          .toList
                      )
                      .map(internalOrders =>
                        ScalaPbGetPaddingOrdersResponse(
                          responseOption =
                            ScalaPbGetPaddingOrdersResponse.ResponseOption.OrderContentResponse(
                              ScalaPbOrderContent(internalOrders.map(_.toScalaPbModel))
                            )
                        )
                      )
                  )
                )
              }
            }
          }
        )
      }
    }

  private def akkaComplete[T <: GeneratedMessage : GeneratedMessageCompanion](response: Future[T]): Route = {
    onComplete(response) {
      case Success(value) =>
        complete(
          HttpResponse(
            entity = HttpEntity(
              ContentTypes.`application/json`,
              value.serializeProtoToJson
            ),
            status = StatusCodes.OK
          )
        )
      case Failure(exception) => ??? //TODO: Show exception handling for Akka
    }
  }
}
