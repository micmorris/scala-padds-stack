package com.padds.example.json

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import scalapb._
import scalapb.json4s.{Parser, Printer}

import scala.collection.immutable
import scala.util.Try

trait ProtoJsonProtocol {

  // This will typically output camelCase fields in the JSON output string
  val pbJsonPrinter: Printer = new Printer()

  // ignoringUnknownFields is needed to keep APIs forwards-compatible with new unknown fields
  val pbJsonParser: Parser = new Parser().ignoringUnknownFields

  // This will preserve the Proto fields exactly,
  // typically outputting snake_case fields in the JSON output string
  private val snakePrinter = new Printer().preservingProtoFieldNames

  // For AkkaHTTP Unmarshalling
  implicit def unmarshalProto[T <: GeneratedMessage : GeneratedMessageCompanion]
      : FromEntityUnmarshaller[T] = {
    Unmarshaller.stringUnmarshaller.map(pbJsonParser.fromJsonString[T](_))
  }

  implicit def marshalProtoResponse[T <: GeneratedMessage : GeneratedMessageCompanion](
      implicit code: StatusCode = StatusCodes.OK,
      additionalHeaders: immutable.Seq[HttpHeader] = immutable.Seq.empty
  ): ToResponseMarshaller[T] = {
    Marshaller
      .withFixedContentType(ContentTypes.`application/json`)(proto =>
        HttpResponse(
          entity = HttpEntity(
            ContentTypes.`application/json`,
            pbJsonPrinter.print(proto)
          ),
          status = code,
          headers = additionalHeaders
        )
      )
  }

  implicit class ProtoSerializer[T <: GeneratedMessage](
      proto: T
  ) {

    def protoToJson: String = {
      pbJsonPrinter.print(proto)
    }

    def protoToSnakeCaseJson: String = {
      snakePrinter.print(proto)
    }
  }

  implicit class ProtoDeserializer(protoJson: String) {

    def jsonToProto[T <: GeneratedMessage : GeneratedMessageCompanion]: T =
      pbJsonParser.fromJsonString[T](protoJson)

    def jsonToMaybeProto[T <: GeneratedMessage : GeneratedMessageCompanion]: Try[T] =
      Try(protoJson.jsonToProto)
  }

}
