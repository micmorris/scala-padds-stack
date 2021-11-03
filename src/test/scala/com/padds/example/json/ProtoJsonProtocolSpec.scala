package com.padds.example.json

import com.padds.example.proto.v1.request._
import io.circe.parser._
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.wordspec.AnyWordSpec
import scalapb.GeneratedMessage

class ProtoJsonProtocolSpec extends AnyWordSpec with Matchers with ProtoJsonProtocol {

  val SnakeCase = true
  val CamelCase = false

  // NOTE: These tests are just demonstrative, you don't have to actually test your model DeSer
  "ProtoJsonProtocol" should {
    val tableInput = Table(
      ("name", "jsonString", "protoModel", "isSnakeCase"), // First tuple defines column names
      (
        "POST /v1/order/padding",
        """{
          |    "playerId": "ade4c24f-65bd-45d5-9deb-67c478d6fc1d",
          |    "paddingId": "6a23c799-96b5-4e33-9b50-8d583caac874",
          |    "qty": 1
          |}
          |""".stripMargin,
        OrderPaddingForPlayerRequest(
          playerId = "ade4c24f-65bd-45d5-9deb-67c478d6fc1d",
          paddingId = "6a23c799-96b5-4e33-9b50-8d583caac874",
          qty = 1
        ),
        CamelCase
      ),
      (
        "POST /v1/order/padding",
        """{
          |    "player_id": "ade4c24f-65bd-45d5-9deb-67c478d6fc1d",
          |    "padding_id": "6a23c799-96b5-4e33-9b50-8d583caac874",
          |    "qty": 1
          |}
          |""".stripMargin,
        OrderPaddingForPlayerRequest(
          playerId = "ade4c24f-65bd-45d5-9deb-67c478d6fc1d",
          paddingId = "6a23c799-96b5-4e33-9b50-8d583caac874",
          qty = 1
        ),
        SnakeCase
      ),
      (
        "POST /v1/order/lookup",
        """{
          |    "orderIds":
          |    [
          |        "1ce91e38-4601-4354-ad1b-2c5c1c70da1a",
          |        "f766cfce-6edd-4e89-aa78-f3018212080f"
          |    ]
          |}
          |""".stripMargin,
        GetPaddingOrdersRequest(
          orderIds = Seq(
            "1ce91e38-4601-4354-ad1b-2c5c1c70da1a",
            "f766cfce-6edd-4e89-aa78-f3018212080f"
          )
        ),
        CamelCase
      ),
      (
        "POST /v1/order/lookup",
        """{
          |    "order_ids":
          |    [
          |        "1ce91e38-4601-4354-ad1b-2c5c1c70da1a",
          |        "f766cfce-6edd-4e89-aa78-f3018212080f"
          |    ]
          |}
          |""".stripMargin,
        GetPaddingOrdersRequest(
          orderIds = Seq(
            "1ce91e38-4601-4354-ad1b-2c5c1c70da1a",
            "f766cfce-6edd-4e89-aa78-f3018212080f"
          )
        ),
        SnakeCase
      )
    )

    forAll(tableInput) {
      (name: String, jsonString: String, protoModel: GeneratedMessage, isSnakeCase: Boolean) =>
        s"map between json and proto at $name with snake_case $isSnakeCase" in {
          // Json => Proto
          val resultProto = protoModel match {
            case _: OrderPaddingForPlayerRequest =>
              jsonString.deserializeJsonIntoProto[OrderPaddingForPlayerRequest]
            case _: GetPaddingOrdersRequest =>
              jsonString.deserializeJsonIntoProto[GetPaddingOrdersRequest]
            case _ =>
              throw new Exception("You probably have to add the new possible types to this test")
          }
          resultProto should be(protoModel)

          // Proto => Json
          val resultJson = if (isSnakeCase) {
            protoModel.serializeProtoToJson_SnakeCase
          } else {
            protoModel.serializeProtoToJson
          }

          //Use circe to parse both strings and make sure they're both compact and sorted
          parse(resultJson).right.get.noSpacesSortKeys should
          be(parse(jsonString).right.get.noSpacesSortKeys)
        }
    }

  }

}
