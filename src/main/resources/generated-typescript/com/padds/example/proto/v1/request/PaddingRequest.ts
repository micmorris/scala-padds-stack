/* eslint-disable */
export const protobufPackage = "com.padds.example.proto.v1.request";

export interface OrderPaddingForPlayerRequest {
  /** UUID as string */
  playerId: string;
  /** UUID as string */
  paddingId: string;
  /** Amount to order */
  qty: number;
}

export interface GetPaddingOrdersRequest {
  /** UUIDs as strings */
  orderIds: string[];
}
