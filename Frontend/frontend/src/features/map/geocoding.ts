/**
 * Nominatim (OpenStreetMap) — gratis dengan aturan: ≤ 1 request/detik, tanpa autocomplete per ketikan.
 * Pencarian dijalankan saat tombol "Cari" ditekan; reverse geocode hanya saat peta diklik.
 */
const BASE = 'https://nominatim.openstreetmap.org';

export interface Place {
  label: string;
  lat: number;
  lng: number;
}

export async function reverseGeocode(lat: number, lng: number, signal?: AbortSignal): Promise<string | null> {
  try {
    const res = await fetch(`${BASE}/reverse?format=jsonv2&lat=${lat}&lon=${lng}&zoom=18&accept-language=id`, { signal });
    if (!res.ok) return null;
    const data: { display_name?: unknown } = await res.json();
    return typeof data.display_name === 'string' ? data.display_name.slice(0, 255) : null; // kolom address maks 255
  } catch {
    return null; // gagal reverse geocode bukan masalah fatal — customer tinggal mengetik alamatnya
  }
}

export async function searchPlaces(query: string, signal?: AbortSignal): Promise<Place[]> {
  const url = `${BASE}/search?format=jsonv2&countrycodes=id&limit=5&accept-language=id&q=${encodeURIComponent(query)}`;
  const res = await fetch(url, { signal });
  if (!res.ok) throw new Error('Pencarian lokasi gagal, coba lagi');
  const data: Array<{ display_name: string; lat: string; lon: string }> = await res.json();
  return data.map((d) => ({ label: d.display_name.slice(0, 255), lat: Number(d.lat), lng: Number(d.lon) }));
}

/** Kolom lat/lng di Oracle: NUMBER(9,6) → 6 digit desimal (±11 cm, lebih dari cukup) */
export const round6 = (n: number) => Math.round(n * 1e6) / 1e6;