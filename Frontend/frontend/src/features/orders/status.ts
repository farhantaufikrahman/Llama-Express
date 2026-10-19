import type { OrderStatus } from './types';

const LIVE = 'bg-[#E8EFFE] text-[#1D4ED8]';
const WAIT = 'bg-[#FFF4D6] text-[#7A5300]';
const PICK = 'bg-[#EEE8FD] text-[#5B32C7]';
const DONE = 'bg-[#E7F4EC] text-[#15803D]';
const STOP = 'bg-[#FBEAEA] text-[#B42318]';

export const ORDER_STATUS: Record<OrderStatus, { label: string; tone: string }> = {
  CREATED: { label: 'Menunggu pembayaran', tone: WAIT },
  SEARCHING_COURIER: { label: 'Mencari kurir', tone: WAIT },
  ASSIGNED: { label: 'Kurir ditemukan', tone: PICK },
  PICKING_UP: { label: 'Kurir menuju jemput', tone: PICK },
  PICKED_UP: { label: 'Paket diambil', tone: LIVE },
  IN_TRANSIT: { label: 'Dalam perjalanan', tone: LIVE },
  DELIVERED: { label: 'Terkirim', tone: DONE },
  CANCELLED: { label: 'Dibatalkan', tone: STOP },
  FAILED: { label: 'Gagal', tone: STOP },
};

/** Order yang masih "bergerak" → halaman detail perlu menyegarkan diri */
export const ACTIVE_STATUSES: ReadonlySet<OrderStatus> = new Set([
  'CREATED', 'SEARCHING_COURIER', 'ASSIGNED', 'PICKING_UP', 'PICKED_UP', 'IN_TRANSIT',
]);

/** Customer boleh MENCOBA batal di status ini; keputusan akhir tetap di state machine backend (409 kalau ditolak) */
export const CANCELLABLE_STATUSES: ReadonlySet<OrderStatus> = new Set(['CREATED', 'SEARCHING_COURIER', 'ASSIGNED']);