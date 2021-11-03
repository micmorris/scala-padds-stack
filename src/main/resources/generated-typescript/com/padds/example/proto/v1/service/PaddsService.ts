/* eslint-disable */
import {
  OrderPaddingForPlayerResponse,
  GetPaddingOrdersResponse,
} from "../../../../../../com/padds/example/proto/v1/response/PaddingResponse";
import {
  OrderPaddingForPlayerRequest,
  GetPaddingOrdersRequest,
} from "../../../../../../com/padds/example/proto/v1/request/PaddingRequest";
import { Empty } from "../../../../../../google/protobuf/empty";

export const protobufPackage = "com.padds.example.proto.v1.service";

export interface PaddsService {
  /** v1 OrderPadding */
  OrderPadding(
    request: OrderPaddingForPlayerRequest
  ): Promise<OrderPaddingForPlayerResponse>;
  /** v1 GetPaddingOrder */
  GetPaddingOrder(request: Empty): Promise<GetPaddingOrdersResponse>;
  /** v1 GetPaddingOrders */
  GetPaddingOrdersBatch(
    request: GetPaddingOrdersRequest
  ): Promise<GetPaddingOrdersResponse>;
}
