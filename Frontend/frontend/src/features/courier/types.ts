import type {
  ItemCategory,
  Location,
  OrderStatus,
  PaymentMethod,
  ServiceType,
  VehicleType,
} from "../orders/types";

export interface CourierProfile {
  userId: number;
  fullName: string;
  phone: string;
  vehicleType: VehicleType;
  plateNumber: string;
  verificationStatus: "PENDING" | "VERIFIED" | "REJECTED";
  online: boolean;
  ratingAvg: number | null;
  ratingCount: number;
}

export interface Offer {
  offerId: number;
  orderCode: string;
  serviceType: ServiceType;
  vehicleType: VehicleType;
  itemCategory: ItemCategory;
  pickup: Location;
  dropoff: Location;
  distanceMeters: number;
  priceTotal: number;
  paymentMethod: PaymentMethod;
  expiresAt: string;
}

export interface CourierJob {
  code: string;
  status: OrderStatus;
  itemCategory: ItemCategory;
  itemDesc: string | null;
  weightKg: number | null;
  pickup: Location;
  dropoff: Location;
  recipientName: string;
  recipientPhone: string;
  notes: string | null;
  priceTotal: number;
  paymentMethod: PaymentMethod;
  assignedAt: string;
}

export interface Proof {
  orderCode: string;
  photoUrl: string;
  receivedBy: string | null;
  capturedAt: string;
}

export interface DailyEarning {
  date: string;
  deliveredCount: number;
  grossTotal: number;
  courierEarning: number;
}

export interface EarningsSummary {
  from: string;
  to: string;
  deliveredCount: number;
  grossTotal: number;
  courierEarning: number;
  cashCollected: number;
  netPayout: number;
  days: DailyEarning[];
}

export interface LocationSample {
  lat: number;
  lng: number;
  speed?: number;
  heading?: number;
}
