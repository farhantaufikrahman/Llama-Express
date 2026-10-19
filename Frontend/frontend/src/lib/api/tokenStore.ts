import type { TokenResponse } from './types';

/**
 * Access token: HANYA di memori (hilang saat reload, tidak bisa dicuri dari storage).
 * Refresh token: di localStorage supaya sesi bertahan setelah reload.
 *
 * Kompromi yang disadari: localStorage bisa dibaca kalau ada celah XSS. Mitigasinya ada di backend:
 * refresh token rotation + reuse detection (langkah 4). Opsi yang lebih kuat adalah cookie HttpOnly —
 * tapi itu WAJIB disertai proteksi CSRF (ingat temuan SonarQube di langkah 13).
 */
const REFRESH_KEY = 'llama.refreshToken';
let accessToken: string | null = null;
const expiredListeners = new Set<() => void>();

function safeStorage<T>(fn: () => T, fallback: T): T {
  try {
    return fn();
  } catch {
    return fallback; // mode private / storage diblokir → tetap jalan tanpa persistensi
  }
}

export const tokenStore = {
  getAccess: () => accessToken,
  getRefresh: () => safeStorage(() => localStorage.getItem(REFRESH_KEY), null),
  set(tokens: Pick<TokenResponse, 'accessToken' | 'refreshToken'>) {
    accessToken = tokens.accessToken;
    safeStorage(() => localStorage.setItem(REFRESH_KEY, tokens.refreshToken), undefined);
  },
  clear() {
    accessToken = null;
    safeStorage(() => localStorage.removeItem(REFRESH_KEY), undefined);
  },
  onSessionExpired(listener: () => void) {
    expiredListeners.add(listener);
    return () => expiredListeners.delete(listener);
  },
  notifySessionExpired() {
    expiredListeners.forEach((l) => l());
  },
};