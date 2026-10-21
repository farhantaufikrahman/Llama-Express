import { env } from '../../config/env';

type Snap = NonNullable<Window['snap']>;
let loading: Promise<Snap> | null = null;

/**
 * Script pihak ketiga dimuat SAAT DIBUTUHKAN (bukan di index.html) dan hanya SEKALI.
 * Customer yang bayar tunai, kurir, dan admin tidak pernah mengunduh Snap.js.
 * Pola single-flight yang sama dengan refresh token di client.ts.
 */
export function loadSnap(): Promise<Snap> {
  if (window.snap) return Promise.resolve(window.snap);
  if (!env.midtransClientKey) {
    return Promise.reject(new Error('VITE_MIDTRANS_CLIENT_KEY belum di-set (lihat .env.example)'));
  }

  loading ??= new Promise<Snap>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = env.midtransSnapUrl;
    script.dataset.clientKey = env.midtransClientKey; // → atribut data-client-key yang dibaca Snap.js
    script.async = true;
    script.onload = () => (window.snap ? resolve(window.snap) : reject(new Error('Midtrans Snap tidak tersedia')));
    script.onerror = () => {
      loading = null; // boleh dicoba lagi nanti
      script.remove();
      reject(new Error('Gagal memuat Midtrans. Cek koneksi internet atau matikan ad-blocker.'));
    };
    document.head.appendChild(script);
  });
  return loading;
}