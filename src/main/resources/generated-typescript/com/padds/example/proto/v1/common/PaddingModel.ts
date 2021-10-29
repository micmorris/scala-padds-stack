/* eslint-disable */
export const protobufPackage = "com.padds.example.proto.v1.common";

export enum PaddingType {
  NO_PADDING = 0,
  SHOULDER_PADS = 1,
  SHIN_GUARDS = 2,
  GLOVES = 3,
  ELBOW_PADS = 4,
}

export interface PaddingOrder {
  /** UUID as string */
  orderId: string;
  /** UUID as string */
  playerId: string;
  /** UUID as string */
  paddingId: string;
  /** When the order was placed as epoch seconds */
  orderPlacedEpoch: number;
}

export interface Padding {
  /** UUID as string */
  paddingId: string;
  /** Type of padding */
  paddingType: PaddingType;
}
