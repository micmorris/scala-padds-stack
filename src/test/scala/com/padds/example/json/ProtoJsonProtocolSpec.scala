package com.padds.example.json

import com.padds.example.proto.v1.request._
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.wordspec.AnyWordSpec
import scalapb.GeneratedMessage

class ProtoJsonProtocolSpec extends AnyWordSpec with Matchers with ProtoJsonProtocol {

  // These tests are just demonstrative, you don't have to actually test your model DeSer
  "ProtoJsonProtocol" should {
    val tableInput = Table(
      ("name", "jsonString", "protoModel"), // First tuple defines column names
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
        )
      )
    )

    forAll(tableInput) { (name: String, jsonString: String, protoModel: GeneratedMessage) =>
      s"map both ways between json and proto on path $name" in {
        val actual = protoModel match {
          case _: OrderPaddingForPlayerRequest => jsonString.deserializeJsonIntoProto[OrderPaddingForPlayerRequest]
          case _: GetPaddingOrdersRequest => jsonString.deserializeJsonIntoProto[GetPaddingOrdersRequest]
          case _ =>
        }
        actual should be(protoModel)
      }
    }

  }

}
