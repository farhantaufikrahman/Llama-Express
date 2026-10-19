import { api } from '../../lib/api/client';

export interface Address {
  id: number;
  label: string;
  address: string;
  lat: number;
  lng: number;
  notes: string | null;
}

export type AddressInput = Omit<Address, 'id' | 'notes'> & { notes?: string };

export const addressApi = {
  list: (signal?: AbortSignal) => api<Address[]>('/api/v1/addresses', { signal }),
  create: (body: AddressInput) => api<Address>('/api/v1/addresses', { method: 'POST', body }),
  update: (id: number, body: AddressInput) => api<Address>(`/api/v1/addresses/${id}`, { method: 'PUT', body }),
  remove: (id: number) => api<void>(`/api/v1/addresses/${id}`, { method: 'DELETE' }),
};

/** Query key terpusat — dipakai juga di F3 saat memilih alamat untuk order */
export const addressKeys = { all: ['addresses'] as const };