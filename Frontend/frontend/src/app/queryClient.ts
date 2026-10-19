import { QueryClient } from '@tanstack/react-query';
import { isApiError } from '../lib/api/errors';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      // Jangan ulangi error 4xx (400/401/403/404 tidak akan berubah kalau diulang) — hanya error jaringan/5xx
      retry: (failureCount, error) => !(isApiError(error) && error.status < 500) && failureCount < 2,
    },
  },
});