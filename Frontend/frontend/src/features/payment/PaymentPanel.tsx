import { useEffect, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Alert } from '../../components/Alert';
import { Button } from '../../components/Button';
import { isApiError } from '../../lib/api/errors';
import { formatDateTime, formatRupiah } from '../../lib/format';
import { orderKeys } from '../orders/api';
import type { Order } from '../orders/types';
import { paymentApi, type Payment } from './api';
import { loadSnap } from './loadSnap';

type Message = { kind: 'success' | 'error' | 'info'; text: string };
type Phase = 'idle' | 'opening' | 'verifying';

const messageFor = (p: Payment): Message => {
  switch (p.status) {
    case 'PAID':
      return { kind: 'success', text: 'Pembayaran berhasil! Kami sedang mencarikan kurir.' };
    case 'PENDING':
      return {
        kind: 'info',
        text: `Menunggu pembayaran${p.expiresAt ? ` — selesaikan sebelum ${formatDateTime(p.expiresAt)}` : ''}. Status diperbarui setelah kamu membayar.`,
      };
    case 'EXPIRED':
      return { kind: 'error', text: 'Waktu pembayaran habis. Klik Bayar untuk membuat tagihan baru.' };
    default:
      return { kind: 'error', text: 'Pembayaran gagal. Silakan coba lagi.' };
  }
};

export function PaymentPanel({ order, autoOpen = false }: { order: Order; autoOpen?: boolean }) {
  const queryClient = useQueryClient();
  const [phase, setPhase] = useState<Phase>('idle');
  const [message, setMessage] = useState<Message | null>(null);
  const autoOpened = useRef(false);

  const refreshOrder = () => queryClient.invalidateQueries({ queryKey: orderKeys.all });

  const showError = (e: unknown) =>
    setMessage({ kind: 'error', text: isApiError(e) || e instanceof Error ? e.message : 'Terjadi kesalahan' });

  /** Minta BACKEND memastikan status ke Midtrans — callback browser tidak pernah dipercaya */
  const verify = async () => {
    setPhase('verifying');
    try {
      const payment = await paymentApi.sync(order.code);
      setMessage(messageFor(payment));
      await refreshOrder();
    } catch (e) {
      showError(e);
    } finally {
      setPhase('idle');
    }
  };

  const pay = async () => {
    setMessage(null);
    setPhase('opening');
    try {
      const [payment, snap] = await Promise.all([paymentApi.create(order.code), loadSnap()]); // paralel
      setPhase('idle');
      snap.pay(payment.snapToken, {
        onSuccess: () => void verify(),
        onPending: () => void verify(),   // mis. VA dibuat → tampilkan batas waktu dari backend
        onError: () => setMessage({ kind: 'error', text: 'Pembayaran ditolak. Coba lagi atau pilih metode lain.' }),
        onClose: () => setMessage({ kind: 'info', text: 'Pembayaran belum selesai. Klik Bayar untuk melanjutkan.' }),
      });
    } catch (e) {
      setPhase('idle');
      showError(e);
      if (isApiError(e) && (e.code === 'ALREADY_PAID' || e.code === 'ORDER_NOT_PAYABLE')) {
        void refreshOrder(); // status di layar sudah basi → ambil yang terbaru
      }
    }
  };

  // Datang dari halaman "Buat order" dengan Bayar online → buka popup otomatis SEKALI
  // (useRef mencegah popup terbuka dua kali karena StrictMode menjalankan efek dua kali di development)
  useEffect(() => {
    if (autoOpen && !autoOpened.current) {
      autoOpened.current = true;
      void pay();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- hanya saat pertama kali tampil
  }, [autoOpen]);

  const busy = phase !== 'idle';

  return (
    <section className="flex flex-col gap-3 rounded-2xl border-[1.5px] border-gold-400 bg-white p-5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-lg font-extrabold">Pembayaran online</h2>
          <p className="text-sm text-ink-muted">Order dicarikan kurir setelah pembayaran terkonfirmasi.</p>
        </div>
        <p className="text-2xl font-extrabold">{formatRupiah(order.priceTotal)}</p>
      </div>

      {message && <Alert kind={message.kind}>{message.text}</Alert>}

      <div className="flex flex-wrap gap-2">
        <Button onClick={() => void pay()} disabled={busy}>
          {phase === 'opening' ? 'Membuka pembayaran…' : 'Bayar sekarang'}
        </Button>
        <Button variant="secondary" onClick={() => void verify()} disabled={busy}>
          {phase === 'verifying' ? 'Memeriksa…' : 'Saya sudah bayar — cek status'}
        </Button>
      </div>
    </section>
  );
}