const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
if (!apiBaseUrl) {
  throw new Error("VITE_API_BASE_URL belum di-set. Lihat .env.example");
}

export const env = {
  apiBaseUrl: apiBaseUrl.replace(/\/$/, ""),
  wsUrl: `${apiBaseUrl.replace(/\/$/, "").replace(/^http/, "ws")}/ws`,
  midtransClientKey: import.meta.env.VITE_MIDTRANS_CLIENT_KEY ?? "",
  midtransSnapUrl:
    import.meta.env.VITE_MIDTRANS_SNAP_URL ??
    "https://app.sandbox.midtrans.com/snap/snap.js",
} as const;
