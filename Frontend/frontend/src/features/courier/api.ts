import { api } from "../../lib/api/client";
import type {
  CourierJob,
  CourierProfile,
  EarningsSummary,
  LocationSample,
  Offer,
  Proof,
} from "./types";

const job = (code: string) =>
  `/api/v1/courier/jobs/${encodeURIComponent(code)}`;

export const courierApi = {
  me: (signal?: AbortSignal) =>
    api<CourierProfile>("/api/v1/courier/me", { signal }),
  setOnline: (online: boolean) =>
    api<CourierProfile>("/api/v1/courier/me/status", {
      method: "PATCH",
      body: { online },
    }),
  sendLocation: (body: LocationSample) =>
    api<void>("/api/v1/courier/me/location", { method: "POST", body }),

  offers: (signal?: AbortSignal) =>
    api<Offer[]>("/api/v1/courier/offers", { signal }),
  accept: (offerId: number) =>
    api<CourierJob>(`/api/v1/courier/offers/${offerId}/accept`, {
      method: "POST",
    }),
  reject: (offerId: number) =>
    api<void>(`/api/v1/courier/offers/${offerId}/reject`, { method: "POST" }),

  /** 204 (tidak ada tugas) → null. TanStack Query melarang data bernilai undefined. */
  activeJob: async (signal?: AbortSignal) =>
    (await api<CourierJob | undefined>("/api/v1/courier/jobs/active", {
      signal,
    })) ?? null,
  startPickup: (code: string) =>
    api<CourierJob>(`${job(code)}/start-pickup`, { method: "POST" }),
  pickUp: (code: string) =>
    api<CourierJob>(`${job(code)}/pickup`, { method: "POST" }),
  uploadProof: (code: string, photo: Blob, receivedBy?: string) => {
    const form = new FormData();
    form.append("photo", photo, "proof.jpg");
    if (receivedBy) form.append("receivedBy", receivedBy);
    return api<Proof>(`${job(code)}/proof`, { method: "POST", body: form });
  },
  complete: (code: string, verificationCode: string) =>
    api<CourierJob>(`${job(code)}/complete`, {
      method: "POST",
      body: { verificationCode },
    }),

  earnings: (from: string, to: string, signal?: AbortSignal) =>
    api<EarningsSummary>(`/api/v1/courier/earnings?from=${from}&to=${to}`, {
      signal,
    }),
};

export const courierKeys = {
  me: ["courier", "me"] as const,
  offers: ["courier", "offers"] as const,
  activeJob: ["courier", "job"] as const,
  earnings: (from: string, to: string) =>
    ["courier", "earnings", from, to] as const,
};
