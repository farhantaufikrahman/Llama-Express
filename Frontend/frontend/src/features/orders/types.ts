export type OrderStatus =
  | "CREATED"
  | "SEARCHING_COURIER"
  | "ASSIGNED"
  | "PICKING_UP"
  | "PICKED_UP"
  | "IN_TRANSIT"
  | "DELIVERED"
  | "CANCELLED"
  | "FAILED";
export type ServiceType = "INSTANT" | "SAME_DAY";
export type VehicleType = "MOTOR" | "CAR";
export type ItemCategory = "DOCUMENT" | "FOOD" | "GOODS";
export type PaymentMethod = "CASH" | "MIDTRANS";
export type PaymentStatus =
  | "UNPAID"
  | "PENDING"
  | "PAID"
  | "FAILED"
  | "REFUNDED";

export interface Location {
  address: string;
  lat: number;
  lng: number;
}

export interface QuoteRequest {
  serviceType: ServiceType;
  vehicleType: VehicleType;
  pickup: Location;
  dropoff: Location;
  weightKg?: number;
}

export interface Quote {
  serviceType: ServiceType;
  vehicleType: VehicleType;
  distanceMeters: number;
  etaMinutes: number;
  baseFare: number;
  distanceFare: number;
  total: number;
}

export interface CreateOrderRequest extends QuoteRequest {
  itemCategory: ItemCategory;
  itemDesc?: string;
  recipientName: string;
  recipientPhone: string;
  notes?: string;
  paymentMethod: PaymentMethod;
}

export interface TimelineEntry {
  status: OrderStatus;
  at: string;
  note: string | null;
}

export interface Order {
  code: string;
  status: OrderStatus;
  serviceType: ServiceType;
  vehicleType: VehicleType;
  itemCategory: ItemCategory;
  itemDesc: string | null;
  weightKg: number | null;
  pickup: Location;
  dropoff: Location;
  recipientName: string;
  recipientPhone: string;
  notes: string | null;
  distanceMeters: number;
  priceTotal: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  verificationCode: string | null;
  trackingToken: string;
  createdAt: string;
  cancelledAt: string | null;
  cancelReason: string | null;
  timeline: TimelineEntry[];
}

export interface OrderSummary {
  code: string;
  status: OrderStatus;
  serviceType: ServiceType;
  dropoffAddress: string;
  priceTotal: number;
  createdAt: string;
}
