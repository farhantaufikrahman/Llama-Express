const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
if (!apiBaseUrl) {
  throw new Error('VITE_API_BASE_URL belum di-set. Lihat .env.example');
}

export const env = {
  apiBaseUrl: apiBaseUrl.replace(/\/$/, ''),
  // Opsional di level aplikasi: yang tidak membayar online tetap bisa memakai aplikasi tanpa key ini
  midtransClientKey: import.meta.env.VITE_MIDTRANS_CLIENT_KEY ?? '',
  midtransSnapUrl: import.meta.env.VITE_MIDTRANS_SNAP_URL ?? 'https://app.sandbox.midtrans.com/snap/snap.js',
} as const;