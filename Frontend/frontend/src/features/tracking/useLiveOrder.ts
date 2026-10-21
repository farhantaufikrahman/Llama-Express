import { useQueryClient } from "@tanstack/react-query";
import { useTopic } from "../../lib/useRealtime";
import { orderKeys } from "../orders/api";
import { trackingKeys } from "./api";
import type { LocationMessage, StatusMessage, TrackingSnapshot } from "./types";

const MAX_PATH_POINTS = 500; // jejak dibatasi supaya memori & render tetap ringan pada perjalanan panjang

/**
 * Dua strategi berbeda untuk dua jenis pesan:
 * - STATUS (jarang, ~6x per order, menyangkut banyak data) → tandai cache basi, biarkan REST mengambil data lengkap.
 * - LOKASI (sering, tiap ±3 detik, kecil) → tempel langsung ke cache tanpa request apa pun.
 */
export function useLiveOrder(code: string | null) {
  const queryClient = useQueryClient();

  useTopic<StatusMessage>(code ? `/topic/orders/${code}/status` : null, () => {
    if (!code) return;
    void queryClient.invalidateQueries({ queryKey: orderKeys.detail(code) });
    void queryClient.invalidateQueries({ queryKey: trackingKeys.order(code) });
    void queryClient.invalidateQueries({ queryKey: orderKeys.list() });
  });

  useTopic<LocationMessage>(
    code ? `/topic/orders/${code}/location` : null,
    (m) => {
      if (!code) return;
      const point = { lat: m.lat, lng: m.lng, at: m.recordedAt };
      queryClient.setQueryData<TrackingSnapshot>(
        trackingKeys.order(code),
        (prev) =>
          prev
            ? {
                ...prev,
                lastLocation: point,
                route: [...prev.route, point].slice(-MAX_PATH_POINTS),
              }
            : prev,
      );
    },
  );
}
