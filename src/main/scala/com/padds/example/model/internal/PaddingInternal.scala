package com.padds.example.model.internal

import com.padds.example.guardrail.definitions.{PaddingOrder => GuardrailPaddingOrder}
import com.padds.example.proto.v1.common.{PaddingOrder => ScalaPbPaddingOrder}

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID

object PaddingInternal {

  def apply(playerId: UUID, paddingId: UUID, quantity: Int): PaddingOrderInternal = {
    PaddingOrderInternal(
      orderId = UUID.randomUUID(),
      playerId = playerId,
      paddingId = paddingId,
      quantity = quantity,
      ZonedDateTime.now(ZoneOffset.UTC)
    )
  }

  case class PaddingOrderInternal(
      orderId: UUID,
      playerId: UUID,
      paddingId: UUID,
      quantity: Int,
      orderPlaced: ZonedDateTime
  ) {

    def toGuardrailModel: GuardrailPaddingOrder = {
      GuardrailPaddingOrder(
        orderId = Option(this.orderId.toString),
        playerId = Option(this.playerId.toString),
        paddingId = Option(this.paddingId.toString),
        orderPlacedEpoch = Option(this.orderPlaced.toEpochSecond)
      )

    }

    def toScalaPbModel: ScalaPbPaddingOrder = {
      ScalaPbPaddingOrder(
        orderId = this.orderId.toString,
        playerId = this.playerId.toString,
        paddingId = this.paddingId.toString,
        orderPlacedEpoch = this.orderPlaced.toEpochSecond
      )

    }
  }

}
