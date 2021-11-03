package com.padds.example.service

import com.padds.example.model.internal.PaddingInternal.PaddingOrderInternal

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object PaddsOperationService {
  def apply() = new PaddsOperationService()
}

class PaddsOperationService {

  def orderPadding(playerId: UUID, paddingId: UUID, quantity: Int)(
      implicit ex: ExecutionContext
  ): Future[PaddingOrderInternal] = {
    //TODO: wire up DDB instead of stub response
    Future.successful(
      PaddingOrderInternal(
        orderId = UUID.randomUUID(),
        playerId = playerId,
        paddingId = paddingId,
        quantity = quantity,
        ZonedDateTime.now(ZoneOffset.UTC)
      )
    )
  }

  def getPaddingOrders(orderIds: List[UUID])(
      implicit ex: ExecutionContext
  ): Future[List[PaddingOrderInternal]] = {
    //TODO: wire up DDB instead of stub response
    Future.successful(
      orderIds
        .take(2)
        .map(orderId =>
          PaddingOrderInternal(
            orderId = orderId,
            playerId = UUID.randomUUID(),
            paddingId = UUID.randomUUID(),
            quantity = 1,
            ZonedDateTime.now(ZoneOffset.UTC).minusDays(1)
          )
        )
    )
  }

}
