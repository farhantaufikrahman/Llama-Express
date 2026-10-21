import { api } from "../../lib/api/client";
import type { TrackingSnapshot } from "./types";

export const trackingApi = {
  forOrder: (code: string, signal?: AbortSignal) =>
    api<TrackingSnapshot>(
      `/api/v1/orders/${encodeURIComponent(code)}/tracking`,
      { signal },
    ),
  forPublic: (token: string, signal?: AbortSignal) =>
    api<TrackingSnapshot>(`/api/v1/track/${encodeURIComponent(token)}`, {
      signal,
      auth: false,
    }),
};

export const trackingKeys = {
  order: (code: string) => ["tracking", "order", code] as const,
  public: (token: string) => ["tracking", "public", token] as const,
};

/** Status di mana kurir sudah terlibat → ada data kurir & jejak */
export const COURIER_PHASE = new Set([
  "ASSIGNED",
  "PICKING_UP",
  "PICKED_UP",
  "IN_TRANSIT",
  "DELIVERED",
]);
