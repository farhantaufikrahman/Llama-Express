import { HttpResponse } from "msw";
import type { PageResponse, TokenResponse, User } from "../lib/api/types";
import { env } from "./env";

export const API = env.apiBaseUrl;
export const REFRESH_PATH = "/api/v1/auth/refresh";

const b64url = (o: object) =>
  btoa(JSON.stringify(o))
    .replace(/=+$/, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");

/** JWT palsu (tanda tangan tidak valid) — cukup untuk kode yang hanya membaca klaim `exp` */
export function fakeJwt(
  claims: Record<string, unknown> = {},
  ttlSeconds = 900,
): string {
  const now = Math.floor(Date.now() / 1000);
  return `${b64url({ alg: "HS256", typ: "JWT" })}.${b64url({ sub: "1", iat: now, exp: now + ttlSeconds, ...claims })}.test`;
}

export const customer: User = {
  id: 1,
  fullName: "Budi Santoso",
  email: "budi@mail.com",
  phone: "081234567890",
  role: "CUSTOMER",
};

/** Bentuk respons /auth/login & /auth/refresh — mengikuti tipe TokenResponse (tanpa data user) */
export function tokens(
  accessToken: string,
  refreshToken: string,
): TokenResponse {
  return { accessToken, refreshToken, tokenType: "Bearer", expiresIn: 900 };
}

/** Error berformat RFC 9457 ProblemDetail, sama seperti GlobalExceptionHandler backend */
export function problem(status: number, code: string, detail: string = code) {
  return HttpResponse.json(
    { type: "about:blank", title: "Error", status, detail, code },
    { status, headers: { "Content-Type": "application/problem+json" } },
  );
}

export function pageOf<T>(
  content: T[],
  opts: { page?: number; size?: number; totalElements?: number } = {},
): PageResponse<T> {
  const size = opts.size ?? 20;
  const totalElements = opts.totalElements ?? content.length;
  return {
    content,
    page: opts.page ?? 0,
    size,
    totalElements,
    totalPages: Math.ceil(totalElements / size),
  };
}
