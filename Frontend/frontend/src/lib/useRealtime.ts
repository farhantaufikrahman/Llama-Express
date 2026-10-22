import { useEffect, useRef, useSyncExternalStore } from "react";
import {
  onReconnect,
  realtimeStatusStore,
  subscribe,
  type RealtimeStatus,
} from "./realtime";

/**
 * Status koneksi WebSocket (idle/connecting/connected/disconnected) sebagai state React.
 * useSyncExternalStore = cara resmi React membaca store di LUAR React (modul realtime.ts)
 * tanpa "tearing" (komponen berbeda melihat nilai berbeda di render yang sama).
 */
export function useRealtimeStatus(): RealtimeStatus {
  return useSyncExternalStore(
    realtimeStatusStore.subscribe,
    realtimeStatusStore.get,
    realtimeStatusStore.get,
  );
}

/**
 * Berlangganan satu topik STOMP selama komponen terpasang.
 * - destination null/undefined → tidak berlangganan (mis. order belum dimuat)
 * - handler disimpan di ref → re-render TIDAK memicu unsubscribe/subscribe ulang;
 *   hanya pergantian destination yang memicunya.
 */
export function useTopic<T = unknown>(
  destination: string | null | undefined,
  handler: (body: T) => void,
): void {
  const handlerRef = useRef(handler);

  useEffect(() => {
    handlerRef.current = handler; // selalu panggil versi handler terbaru (closure segar)
  });

  useEffect(() => {
    if (!destination) return;
    return subscribe(destination, (body) => handlerRef.current(body as T));
  }, [destination]);
}

/**
 * Jalankan callback setiap kali koneksi realtime PULIH setelah terputus
 * (mis. pod backend diganti saat deploy) → halaman mengambil ulang data yang mungkin terlewat.
 * - callback null/undefined → tidak mendengarkan
 * - callback disimpan di ref (pola yang sama dengan useTopic)
 */
export function useOnReconnect(
  callback: (() => void) | null | undefined,
): void {
  const callbackRef = useRef(callback);

  useEffect(() => {
    callbackRef.current = callback;
  });

  const enabled = !!callback;
  useEffect(() => {
    if (!enabled) return;
    return onReconnect(() => callbackRef.current?.());
  }, [enabled]);
}
