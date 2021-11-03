package com.padds.example.json

import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import scalapb._
import scalapb.json4s.{JsonFormat, Printer}

import scala.util.Try

trait ProtoJsonProtocol {

  // This will typically output camelCase fields in the JSON output string
  private val defaultPrinter = new Printer()

  // This will preserve the Proto fields exactly,
  // typically outputting snake_case fields in the JSON output string
  private val snakePrinter = new Printer().preservingProtoFieldNames

  // For AkkaHTTP Unmarshalling
  implicit def unmarshalProto[T <: GeneratedMessage : GeneratedMessageCompanion]
      : FromEntityUnmarshaller[T] = {
    Unmarshaller.stringUnmarshaller.map(JsonFormat.fromJsonString[T](_))
  }

  implicit class ProtoSerializer[T <: GeneratedMessage](
      proto: T
  ) {

    def serializeProtoToJson: String = {
      defaultPrinter.print(proto)
    }

    def serializeProtoToJson_SnakeCase: String = {
      snakePrinter.print(proto)
    }

  }

  implicit class ProtoDeserializer(protoJson: String) {

    def deserializeJsonIntoProto[T <: GeneratedMessage : GeneratedMessageCompanion]: T =
      JsonFormat.fromJsonString[T](protoJson)

    def deserializeJsonIntoMaybeProto[T <: GeneratedMessage : GeneratedMessageCompanion]: Try[T] =
      Try(protoJson.deserializeJsonIntoProto)
  }

}
