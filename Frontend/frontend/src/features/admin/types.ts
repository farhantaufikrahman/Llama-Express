import type { CourierProfile } from "../courier/types";
import type {
  Order,
  OrderStatus,
  PaymentMethod,
  PaymentStatus,
  ServiceType,
  VehicleType,
} from "../orders/types";
import type { Payment } from "../payment/api";
import type { Role } from "../../lib/api/types";

export interface DashboardSummary {
  date: string;
  ordersToday: number;
  activeOrders: number;
  searchingCourier: number;
  couriersOnline: number;
  deliveredToday: number;
  revenueToday: number;
  avgDeliveryMinutes: number | null;
  statusBreakdown: Partial<Record<OrderStatus, number>>;
}

export interface AdminOrderSummary {
  code: string;
  status: OrderStatus;
  serviceType: ServiceType;
  customerName: string;
  courierName: string | null;
  priceTotal: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  createdAt: string;
}

export interface Party {
  id: number;
  fullName: string;
  email: string;
  phone: string;
}

export interface AdminOrderDetail {
  order: Order;
  customer: Party;
  courier: Party | null;
  latestPayment: Payment | null;
}

export interface OrderFilter {
  q?: string;
  status?: OrderStatus;
  serviceType?: ServiceType;
  paymentMethod?: PaymentMethod;
  from?: string; // YYYY-MM-DD
  to?: string;
}

export interface Tariff {
  id: number;
  serviceType: ServiceType;
  vehicleType: VehicleType;
  baseFare: number;
  perKmFare: number;
  minFare: number;
  activeFrom: string;
  activeTo: string | null;
  active: boolean;
}

export interface TariffInput {
  serviceType: ServiceType;
  vehicleType: VehicleType;
  baseFare: number;
  perKmFare: number;
  minFare: number;
  activeFrom?: string; // ISO; kosong = mulai sekarang
}

export interface AdminUser {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  role: Role;
  status: "ACTIVE" | "SUSPENDED";
  createdAt: string;
}

export type VerificationStatus = CourierProfile["verificationStatus"];
