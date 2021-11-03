package com.padds.example.json

import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import scalapb._
import scalapb.json4s.JsonFormat

import scala.util.Try

trait ProtoJsonProtocol {

  // This will output snake_case fields in JSON output string
  implicit private val scalaPbPrinter = new scalapb.json4s.Printer().preservingProtoFieldNames

  // For AkkaHTTP Unmarshalling
  implicit def unmarshalProto[T <: GeneratedMessage : GeneratedMessageCompanion]
      : FromEntityUnmarshaller[T] = {
    Unmarshaller.stringUnmarshaller.map(JsonFormat.fromJsonString[T](_))
  }

  implicit class ProtoSerializer[T <: GeneratedMessage : GeneratedMessageCompanion](
      proto: T
  ) {

    def serializeProtoIntoJson: String = {
      scalaPbPrinter.print(proto)
    }
  }

  implicit class ProtoDeserializer(protoJson: String) {

    def deserializeJsonIntoProto[T <: GeneratedMessage : GeneratedMessageCompanion]: T =
      JsonFormat.fromJsonString[T](protoJson)

    def deserializeJsonIntoMaybeProto[T <: GeneratedMessage : GeneratedMessageCompanion]: Try[T] =
      Try(protoJson.deserializeJsonIntoProto)
  }

}
