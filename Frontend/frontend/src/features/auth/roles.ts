import type { Role } from '../../lib/api/types';

/** Halaman awal tiap role — dipakai setelah login dan saat role salah membuka halaman */
export function homeFor(role: Role): string {
  switch (role) {
    case 'CUSTOMER':
      return '/orders';
    case 'COURIER':
      return '/courier';
    case 'ADMIN':
      return '/admin';
  }
}