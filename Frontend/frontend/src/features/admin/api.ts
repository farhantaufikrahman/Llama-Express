import { api, apiDownload } from "../../lib/api/client";
import type { PageResponse } from "../../lib/api/types";
import type { CourierProfile } from "../courier/types";
import type {
  AdminOrderDetail,
  AdminOrderSummary,
  AdminUser,
  DashboardSummary,
  OrderFilter,
  Tariff,
  TariffInput,
  VerificationStatus,
} from "./types";

/** Buang filter kosong → query string bersih & cache key stabil */
export function toQuery(
  params: Record<string, string | number | undefined>,
): string {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== "") q.set(k, String(v));
  });
  return q.toString();
}

const order = (code: string) =>
  `/api/v1/admin/orders/${encodeURIComponent(code)}`;

export const adminApi = {
  dashboard: (signal?: AbortSignal) =>
    api<DashboardSummary>("/api/v1/admin/dashboard/summary", { signal }),

  orders: (filter: OrderFilter, page: number, signal?: AbortSignal) =>
    api<PageResponse<AdminOrderSummary>>(
      `/api/v1/admin/orders?${toQuery({ ...filter, page, size: 20 })}`,
      { signal },
    ),
  order: (code: string, signal?: AbortSignal) =>
    api<AdminOrderDetail>(order(code), { signal }),
  cancelOrder: (code: string, reason: string) =>
    api<AdminOrderDetail>(`${order(code)}/cancel`, {
      method: "POST",
      body: { reason },
    }),
  exportOrders: (filter: OrderFilter) =>
    apiDownload(`/api/v1/admin/orders/export?${toQuery({ ...filter })}`, {
      accept: "text/csv",
    }),

  couriers: (status: VerificationStatus, page: number, signal?: AbortSignal) =>
    api<PageResponse<CourierProfile>>(
      `/api/v1/admin/couriers?${toQuery({ status, page, size: 20 })}`,
      { signal },
    ),
  verifyCourier: (id: number) =>
    api<CourierProfile>(`/api/v1/admin/couriers/${id}/verify`, {
      method: "PATCH",
    }),
  rejectCourier: (id: number) =>
    api<CourierProfile>(`/api/v1/admin/couriers/${id}/reject`, {
      method: "PATCH",
    }),

  tariffs: (signal?: AbortSignal) =>
    api<Tariff[]>("/api/v1/admin/tariffs", { signal }),
  createTariff: (body: TariffInput) =>
    api<Tariff>("/api/v1/admin/tariffs", { method: "POST", body }),

  users: (
    role: "CUSTOMER" | "COURIER",
    q: string,
    page: number,
    signal?: AbortSignal,
  ) =>
    api<PageResponse<AdminUser>>(
      `/api/v1/admin/users?${toQuery({ role, q, page, size: 20 })}`,
      { signal },
    ),
  suspendUser: (id: number) =>
    api<AdminUser>(`/api/v1/admin/users/${id}/suspend`, { method: "PATCH" }),
  activateUser: (id: number) =>
    api<AdminUser>(`/api/v1/admin/users/${id}/activate`, { method: "PATCH" }),
};

export const adminKeys = {
  all: ["admin"] as const,
  dashboard: ["admin", "dashboard"] as const,
  orders: (filter: OrderFilter, page: number) =>
    ["admin", "orders", filter, page] as const,
  order: (code: string) => ["admin", "order", code] as const,
  couriers: (status: VerificationStatus, page: number) =>
    ["admin", "couriers", status, page] as const,
  tariffs: ["admin", "tariffs"] as const,
  users: (role: string, q: string, page: number) =>
    ["admin", "users", role, q, page] as const,
};
