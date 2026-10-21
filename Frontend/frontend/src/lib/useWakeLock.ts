import { useEffect } from "react";

/** Jaga layar tetap menyala selama `active` (mis. sedang mengantar). Diam saja kalau browser tidak mendukung. */
export function useWakeLock(active: boolean) {
  useEffect(() => {
    if (!active || !("wakeLock" in navigator)) return;
    let lock: WakeLockSentinel | null = null;
    const acquire = async () => {
      try {
        lock = await navigator.wakeLock.request("screen");
      } catch {
        // ditolak (mis. baterai lemah) — bukan error fatal
      }
    };
    // Wake lock otomatis lepas saat tab disembunyikan → minta lagi saat tab terlihat kembali
    const onVisible = () =>
      document.visibilityState === "visible" && void acquire();
    void acquire();
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      document.removeEventListener("visibilitychange", onVisible);
      void lock?.release();
    };
  }, [active]);
}
