/* eslint-disable */
import { PaddingOrder } from "../../../../../../com/padds/example/proto/v1/common/PaddingModel";

export const protobufPackage = "com.padds.example.proto.v1.response";

export interface OrderPaddingForPlayerResponse {
  failureResponse?: OrderFailure | undefined;
  orderSuccessResponse?: OrderSuccess | undefined;
}

export interface OrderSuccess {
  order?: PaddingOrder;
}

export interface OrderFailure {
  failureReason: string;
  failureId: number;
}

export interface GetPaddingOrdersResponse {
  failureResponse?: OrderFailure | undefined;
  orderContentResponse?: OrderContent | undefined;
}

export interface OrderContent {
  orders: PaddingOrder[];
}
