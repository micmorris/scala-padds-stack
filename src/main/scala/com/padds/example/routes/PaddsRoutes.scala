package com.padds.example.routes

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.padds.example.guardrail._
import com.padds.example.guardrail.definitions.{
  GetPaddingOrdersRequest => GuardrailGetPaddingOrdersRequest,
  GetPaddingOrdersResponse => GuardrailGetPaddingOrdersResponse,
  OrderContent => GuardrailOrderContent,
  OrderPaddingForPlayerRequest => GuardrailOrderPaddingForPlayerRequest,
  OrderPaddingForPlayerResponse => GuardrailOrderPaddingForPlayerResponse,
  OrderSuccess => GuardrailOrderSuccess
}
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
      (get & path("http-metrics")) {
        metrics(metricsRegistry)
      },
      pathPrefix("akka-scalapb") { akkaHttpRoutes },
      pathPrefix("guardrail") { guardrailRoutes }
    )
  }

  // This is an alternative to making your own AkkaHTTP routes and using ScalaPB DeSer
  val guardrailRoutes: Route =
    extractExecutionContext { implicit ec =>
      Resource.routes(new Handler {

        //TODO: Show exception handling for Guardrail

        override def paddsServiceOrderPadding(respond: Resource.PaddsServiceOrderPaddingResponse.type)(
            body: GuardrailOrderPaddingForPlayerRequest
        ): Future[Resource.PaddsServiceOrderPaddingResponse] =
          time(
            GuardrailMetrics.ORDER_PADDING_SUMMARY.observe,
            paddsOperationService
              .orderPadding(
                playerId = UUID.fromString(body.playerId.get),
                paddingId = UUID.fromString(body.paddingId.get),
                quantity = body.qty.getOrElse(0)
              )
              .map(internalOrder =>
                GuardrailOrderPaddingForPlayerResponse(
                  failureResponse = None,
                  orderSuccessResponse = Option(
                    GuardrailOrderSuccess(
                      Option(internalOrder.toGuardrailModel)
                    )
                  )
                )
              )
          )

        override def paddsServiceGetPaddingOrder(respond: Resource.PaddsServiceGetPaddingOrderResponse.type)(
            orderId: String
        ): Future[Resource.PaddsServiceGetPaddingOrderResponse] =
          time(
            GuardrailMetrics.GET_PADDING_ORDERS_SUMMARY.observe,
            paddsOperationService
              .getPaddingOrders(
                orderIds = List(UUID.fromString(orderId))
              )
              .map(internalOrders =>
                GuardrailGetPaddingOrdersResponse(
                  failureResponse = None,
                  orderContentResponse =
                    Option(GuardrailOrderContent(Option(internalOrders.map(_.toGuardrailModel).toVector)))
                )
              )
          )

        override def paddsServiceGetPaddingOrdersBatch(
            respond: Resource.PaddsServiceGetPaddingOrdersBatchResponse.type
        )(
            body: GuardrailGetPaddingOrdersRequest
        ): Future[Resource.PaddsServiceGetPaddingOrdersBatchResponse] =
          time(
            GuardrailMetrics.GET_PADDING_ORDERS_SUMMARY.observe,
            paddsOperationService
              .getPaddingOrders(
                orderIds = body.orderIds
                  .getOrElse(Vector.empty)
                  .map(UUID.fromString)
                  .toList
              )
              .map(internalOrders =>
                GuardrailGetPaddingOrdersResponse(
                  failureResponse = None,
                  orderContentResponse =
                    Option(GuardrailOrderContent(Option(internalOrders.map(_.toGuardrailModel).toVector)))
                )
              )
          )

      })
    }

  // This is an alternative to Guardrail making the routes and DeSer for models
  val akkaHttpRoutes: Route =
    extractExecutionContext { implicit ec =>
      pathPrefix("v1") {
        concat(
          path("order" / "padding") {
            post {
              entity(as[ScalaPbOrderPaddingForPlayerRequest]) { orderPaddingForPlayerRequest =>
                time(
                  AkkaMetrics.ORDER_PADDING_SUMMARY.observe,
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
                AkkaMetrics.GET_PADDING_ORDERS_SUMMARY.observe,
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
                  AkkaMetrics.GET_PADDING_ORDERS_SUMMARY.observe,
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
              value.serializeProtoIntoJson
            ),
            status = StatusCodes.OK
          )
        )
      case Failure(exception) => ??? //TODO: Show exception handling for Akka
    }
  }
}
