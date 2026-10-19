import { api } from '../../lib/api/client';

export type PaymentTxStatus = 'PENDING' | 'PAID' | 'FAILED' | 'EXPIRED' | 'REFUNDED';

export interface Payment {
  orderCode: string;
  merchantOrderId: string;
  status: PaymentTxStatus;
  amount: number;
  snapToken: string;
  redirectUrl: string;
  expiresAt: string | null;
  paidAt: string | null;
}

export const paymentApi = {
  /** Idempotent: kalau transaksi PENDING yang masih berlaku ada, backend mengembalikan token yang SAMA */
  create: (code: string) => api<Payment>(`/api/v1/orders/${encodeURIComponent(code)}/payment`, { method: 'POST' }),
  /** Backend bertanya langsung ke Status API Midtrans (pakai Server Key) — sumber kebenaran status */
  sync: (code: string) => api<Payment>(`/api/v1/orders/${encodeURIComponent(code)}/payment/sync`, { method: 'POST' }),
};