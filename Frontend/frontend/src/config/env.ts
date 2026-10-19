/**
 * SATU-SATUNYA tempat membaca import.meta.env (di test, file ini di-stub lewat moduleNameMapper).
 * Nilai VITE_* di-"bake" saat `vite build` → bersifat PUBLIK, jangan taruh rahasia di sini.
 */
const raw = import.meta.env;

/** Kosong = API di origin yang sama (Nginx mem-proxy /api & /ws). Diisi = backend di domain lain (mis. Vercel → api.domain). */
const apiBaseUrl = (raw.VITE_API_BASE_URL ?? "").trim().replace(/\/+$/, "");

function toWsUrl(base: string): string {
  if (base) return `${base.replace(/^http/, "ws")}/ws`; // http→ws, https→wss
  const { protocol, host } = window.location;
  return `${protocol === "https:" ? "wss" : "ws"}://${host}/ws`; // halaman HTTPS wajib memakai wss (mixed content)
}

export const env = {
  apiBaseUrl,
  wsUrl: toWsUrl(apiBaseUrl),
  midtransClientKey: raw.VITE_MIDTRANS_CLIENT_KEY ?? "",
  midtransSnapUrl:
    raw.VITE_MIDTRANS_SNAP_URL ??
    "https://app.sandbox.midtrans.com/snap/snap.js",
};
