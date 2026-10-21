import type { TokenResponse } from "./types";

/**
 * Penyimpanan token:
 * - access token  → MEMORI saja (hilang saat reload; tidak bisa dicuri skrip XSS dari storage)
 * - refresh token → localStorage (bertahan antar reload; dirotasi tiap refresh oleh backend)
 */
const REFRESH_KEY = "llama.refreshToken";

let accessToken: string | null = null;
const expiredListeners = new Set<() => void>();

function readRefresh(): string | null {
  try {
    return localStorage.getItem(REFRESH_KEY);
  } catch {
    return null; // mode privat / storage diblokir
  }
}

function writeRefresh(value: string | null) {
  try {
    if (value) localStorage.setItem(REFRESH_KEY, value);
    else localStorage.removeItem(REFRESH_KEY);
  } catch {
    /* storage tidak tersedia — sesi hanya bertahan sampai reload */
  }
}

/** Baca klaim `exp` dari JWT (tanpa verifikasi tanda tangan — itu tugas backend) → epoch ms */
function expiryOf(token: string): number | null {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const { exp } = JSON.parse(atob(payload)) as { exp?: number };
    return typeof exp === "number" ? exp * 1000 : null;
  } catch {
    return null;
  }
}

type TokenPair = Pick<TokenResponse, "accessToken" | "refreshToken">;

/** Simpan token baru. Menerima `set(access, refresh)` maupun `set({ accessToken, refreshToken })`. */
function set(access: string, refresh: string): void;
function set(tokens: TokenPair): void;
function set(accessOrTokens: string | TokenPair, refresh?: string): void {
  const pair: TokenPair =
    typeof accessOrTokens === "string"
      ? { accessToken: accessOrTokens, refreshToken: refresh ?? "" }
      : accessOrTokens;
  if (!pair.accessToken || !pair.refreshToken) {
    throw new Error("tokenStore.set: accessToken & refreshToken wajib diisi"); // gagal keras, jangan diam-diam simpan undefined
  }
  accessToken = pair.accessToken;
  writeRefresh(pair.refreshToken);
}

function clear() {
  accessToken = null;
  writeRefresh(null);
}

export const tokenStore = {
  getAccess: (): string | null => accessToken,
  getRefresh: (): string | null => readRefresh(),

  set,

  /** Hapus semua token (logout, atau setelah refresh gagal) — tidak memicu listener */
  clear,

  /** Dipanggil client.ts setelah refresh gagal (client sudah memanggil clear()) → kabari AuthProvider */
  notifySessionExpired() {
    expiredListeners.forEach((listener) => listener());
  },

  /** Berlangganan event "sesi berakhir". Mengembalikan fungsi unsubscribe (cleanup useEffect). */
  onSessionExpired(listener: () => void): () => void {
    expiredListeners.add(listener);
    return () => {
      expiredListeners.delete(listener); // kurung kurawal → return void, bukan boolean dari Set.delete
    };
  },

  /** true jika access token tidak ada / tak terbaca / kedaluwarsa dalam `ms` ke depan */
  accessExpiresWithin(ms: number): boolean {
    if (!accessToken) return true;
    const exp = expiryOf(accessToken);
    return exp === null || exp - Date.now() <= ms;
  },
};
