import { useEffect, useRef, useState } from "react";
import { courierApi } from "./api";
import type { LocationSample } from "./types";

export type LocationStatus =
  | "off"
  | "locating"
  | "sharing"
  | "denied"
  | "unavailable";

const MIN_INTERVAL_MS = 3_000; // saat bergerak: maksimal 1 kiriman per 3 detik
const HEARTBEAT_MS = 10_000; // saat diam: tetap kirim (Redis menghapus kurir tanpa heartbeat > 30 detik)

const finite = (n: number | null) =>
  n != null && Number.isFinite(n) ? n : undefined;

export function useLocationReporter(enabled: boolean) {
  // Dukungan GPS tidak berubah selama aplikasi berjalan → cukup dihitung, bukan disimpan di state
  const supported =
    typeof navigator !== "undefined" && "geolocation" in navigator;

  // Hanya status yang berasal dari GPS (callback watchPosition) yang perlu state
  const [watchStatus, setWatchStatus] =
    useState<Exclude<LocationStatus, "off">>("locating");
  const [position, setPosition] = useState<LocationSample | null>(null);
  const [lastSentAt, setLastSentAt] = useState<number | null>(null);
  const latest = useRef<LocationSample | null>(null);
  const lastSent = useRef(0);

  useEffect(() => {
    if (!enabled || !supported) return;
    let stopped = false;

    const send = async (force: boolean) => {
      const sample = latest.current;
      const now = Date.now();
      if (!sample || (!force && now - lastSent.current < MIN_INTERVAL_MS))
        return;
      lastSent.current = now;
      try {
        await courierApi.sendLocation(sample);
        if (!stopped) setLastSentAt(now);
      } catch {
        // gagal sekali (mis. sinyal hilang) tidak apa — kiriman berikutnya datang beberapa detik lagi
      }
    };

    const watchId = navigator.geolocation.watchPosition(
      ({ coords }) => {
        const sample: LocationSample = {
          lat: coords.latitude,
          lng: coords.longitude,
          speed: finite(coords.speed), // null/NaN saat diam → tidak dikirim
          heading: finite(coords.heading),
        };
        latest.current = sample;
        setPosition(sample);
        setWatchStatus("sharing");
        void send(false);
      },
      (err) =>
        setWatchStatus(
          err.code === err.PERMISSION_DENIED ? "denied" : "unavailable",
        ),
      { enableHighAccuracy: true, maximumAge: 5_000, timeout: 20_000 },
    );
    const heartbeat = setInterval(() => void send(true), HEARTBEAT_MS);

    return () => {
      stopped = true;
      navigator.geolocation.clearWatch(watchId);
      clearInterval(heartbeat);
      // Saat online lagi nanti, mulai dari "mencari lokasi" — bukan status sesi sebelumnya
      setWatchStatus("locating");
    };
  }, [enabled, supported]);

  // Status akhir DIHITUNG saat render → tidak ada setState di badan effect
  const status: LocationStatus = !enabled
    ? "off"
    : !supported
      ? "unavailable"
      : watchStatus;

  return { status, position, lastSentAt };
}
