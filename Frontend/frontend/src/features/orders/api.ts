import { api } from '../../lib/api/client';
import type { PageResponse } from '../../lib/api/types';
import type { CreateOrderRequest, Order, OrderSummary, Quote, QuoteRequest } from './types';

export const orderApi = {
  quote: (body: QuoteRequest, signal?: AbortSignal) =>
    api<Quote>('/api/v1/orders/quote', { method: 'POST', body, signal }),
  create: (body: CreateOrderRequest) => api<Order>('/api/v1/orders', { method: 'POST', body }),
  list: (signal?: AbortSignal) => api<PageResponse<OrderSummary>>('/api/v1/orders?page=0&size=20', { signal }),
  detail: (code: string, signal?: AbortSignal) => api<Order>(`/api/v1/orders/${encodeURIComponent(code)}`, { signal }),
  cancel: (code: string, reason: string) =>
    api<Order>(`/api/v1/orders/${encodeURIComponent(code)}/cancel`, { method: 'POST', body: { reason } }),
};

/** Semua query key order di satu tempat → invalidasi konsisten (invalidate orderKeys.all = semua ikut segar) */
export const orderKeys = {
  all: ['orders'] as const,
  list: () => [...orderKeys.all, 'list'] as const,
  detail: (code: string) => [...orderKeys.all, 'detail', code] as const,
  quote: (req: QuoteRequest | null) => ['quote', req] as const,
};