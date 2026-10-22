import { useQueryClient } from "@tanstack/react-query";
import { useOnReconnect, useTopic } from "../../lib/useRealtime";
import { orderKeys } from "../orders/api";
import { trackingKeys } from "./api";
import type { LocationMessage, StatusMessage, TrackingSnapshot } from "./types";

const MAX_PATH_POINTS = 500; // jejak dibatasi supaya memori & render tetap ringan pada perjalanan panjang

/**
 * Dua strategi berbeda untuk dua jenis pesan:
 * - STATUS (jarang, ~6x per order, menyangkut banyak data) → tandai cache basi, biarkan REST mengambil data lengkap.
 * - LOKASI (sering, tiap ±3 detik, kecil) → tempel langsung ke cache tanpa request apa pun.
 * Setelah koneksi pulih → refresh yang sama seperti event status, karena event selama terputus tidak dikirim ulang
 * (snapshot tracking dari REST juga mengisi titik lokasi yang terlewat).
 */
export function useLiveOrder(code: string | null) {
  const queryClient = useQueryClient();

  const refreshOrder = code
    ? () => {
        void queryClient.invalidateQueries({
          queryKey: orderKeys.detail(code),
        });
        void queryClient.invalidateQueries({
          queryKey: trackingKeys.order(code),
        });
        void queryClient.invalidateQueries({ queryKey: orderKeys.list() });
      }
    : null;

  useTopic<StatusMessage>(code ? `/topic/orders/${code}/status` : null, () => {
    refreshOrder?.();
  });

  useOnReconnect(refreshOrder);

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
