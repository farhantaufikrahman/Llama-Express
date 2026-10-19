import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';
import { isApiError } from './api/errors';

/** Kode error bisnis dari backend → field form yang relevan */
const CODE_TO_FIELD: Record<string, string> = {
  EMAIL_TAKEN: 'email',
  PHONE_TAKEN: 'phone',
  PLATE_TAKEN: 'plateNumber',
  INVALID_CURRENT_PASSWORD: 'currentPassword',
};

/**
 * Pasang error dari backend ke field yang tepat (di bawah input-nya).
 * @returns pesan untuk banner kalau error tidak bisa dipetakan ke field mana pun, atau null.
 */
export function applyServerErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: readonly string[],
): string | null {
  if (!isApiError(error)) {
    return 'Tidak bisa terhubung ke server. Cek koneksi atau apakah backend berjalan.';
  }
  let applied = false;
  // 400 VALIDATION_ERROR: { errors: { phone: "Format nomor HP tidak valid", ... } }
  for (const [field, message] of Object.entries(error.fieldErrors)) {
    if (fields.includes(field)) {
      setError(field as Path<T>, { type: 'server', message });
      applied = true;
    }
  }
  const mapped = CODE_TO_FIELD[error.code];
  if (mapped && fields.includes(mapped)) {
    setError(mapped as Path<T>, { type: 'server', message: error.message });
    applied = true;
  }
  return applied ? null : error.message;
}