import type {
  Location,
  OrderStatus,
  TimelineEntry,
  VehicleType,
} from "../orders/types";

export interface LocationPoint {
  lat: number;
  lng: number;
  at: string;
}

export interface CourierInfo {
  name: string;
  phone: string | null; // null di link publik
  vehicleType: VehicleType;
  plateNumber: string;
  ratingAvg: number | null;
}

export interface TrackingSnapshot {
  orderCode: string;
  status: OrderStatus;
  pickup: Location;
  dropoff: Location;
  courier: CourierInfo | null;
  lastLocation: LocationPoint | null;
  route: LocationPoint[];
  timeline: TimelineEntry[];
}

/** Pesan dari /topic/orders/{code}/status (TrackingWebSocketBridge.StatusMessage) */
export interface StatusMessage {
  orderCode: string;
  status: OrderStatus;
  note: string | null;
  at: string;
}

/** Pesan dari /topic/orders/{code}/location (CourierLocationEvent) */
export interface LocationMessage {
  orderCode: string;
  lat: number;
  lng: number;
  speed: number | null;
  heading: number | null;
  recordedAt: string;
}
