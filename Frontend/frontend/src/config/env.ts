const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
if (!apiBaseUrl) {
  // Fail fast — sama seperti @Validated di JwtProperties backend
  throw new Error('VITE_API_BASE_URL belum di-set. Lihat .env.example');
}

export const env = {
  apiBaseUrl: apiBaseUrl.replace(/\/$/, ''),
} as const;