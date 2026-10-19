import { env } from "../../config/env";
import { ApiError } from "./errors";
import { tokenStore } from "./tokenStore";
import type { TokenResponse } from "./types";

type Method = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

interface RequestOptions {
  method?: Method;
  body?: unknown; // objek → JSON; FormData → multipart (upload foto bukti)
  auth?: boolean; // default true: sertakan Bearer token
  signal?: AbortSignal; // TanStack Query membatalkan request yang sudah tidak dibutuhkan
}

/** Satu-satunya pintu ke backend — padanan RestClient/WebClient di Spring atau http.Client di Go */
export async function api<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const useAuth = options.auth !== false;
  let response = await send(
    path,
    options,
    useAuth ? tokenStore.getAccess() : null,
  );

  // Access token kedaluwarsa (15 menit) → refresh SEKALI, lalu ulangi request yang sama
  if (response.status === 401 && useAuth && tokenStore.getRefresh()) {
    const refreshed = await refreshSession();
    if (refreshed) {
      response = await send(path, options, tokenStore.getAccess());
    }
  }
  return parse<T>(response);
}

function send(path: string, options: RequestOptions, token: string | null) {
  const headers = new Headers({
    Accept: "application/json",
    "X-Request-Id": crypto.randomUUID(),
  });
  let body: BodyInit | undefined;
  if (options.body instanceof FormData) {
    body = options.body; // JANGAN set Content-Type — browser menambahkan boundary multipart sendiri
  } else if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(options.body);
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  return fetch(`${env.apiBaseUrl}${path}`, {
    method: options.method ?? "GET",
    headers,
    body,
    signal: options.signal,
  });
}

async function parse<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }
  const isJson = (response.headers.get("Content-Type") ?? "").includes("json");
  const payload = isJson ? await response.json() : null;
  if (!response.ok) {
    throw new ApiError(response.status, payload);
  }
  return payload as T;
}

/**
 * SINGLE-FLIGHT REFRESH — sangat penting dengan backend kita.
 * Kalau 3 request gagal 401 bersamaan dan masing-masing me-refresh sendiri, request ke-2 dan ke-3
 * memakai refresh token yang SUDAH dirotasi → backend menganggapnya pencurian (reuse detection)
 * → SEMUA sesi user dicabut. Jadi semua pemanggil menunggu SATU promise refresh yang sama.
 */
let refreshInFlight: Promise<boolean> | null = null;

export function refreshSession(): Promise<boolean> {
  refreshInFlight ??= doRefresh().finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

async function doRefresh(): Promise<boolean> {
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) {
    return false;
  }
  try {
    const tokens = await api<TokenResponse>("/api/v1/auth/refresh", {
      method: "POST",
      body: { refreshToken },
      auth: false,
    });
    tokenStore.set(tokens);
    return true;
  } catch {
    tokenStore.clear();
    tokenStore.notifySessionExpired(); // AuthProvider mendengar ini → kembali ke halaman login
    return false;
  }
}

export async function ensureFreshAccessToken(): Promise<string | null> {
  if (!tokenStore.accessExpiresWithin(30)) {
    return tokenStore.getAccess();
  }
  return (await refreshSession()) ? tokenStore.getAccess() : null;
}
