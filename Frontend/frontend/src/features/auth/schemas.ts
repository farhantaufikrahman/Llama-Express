import { z } from 'zod';

/** Sama dengan @Pattern di backend: 08xx / 628xx / +628xx, total 10–14 digit setelah awalan */
export const PHONE_REGEX = /^(\+62|62|0)8[0-9]{8,12}$/;
/** Plat Indonesia: 1–2 huruf, 1–4 angka, 0–3 huruf. Spasi opsional (backend menormalkan: "D 1234 ABC" → "D1234ABC") */
export const PLATE_REGEX = /^[A-Za-z]{1,2}\s?\d{1,4}\s?[A-Za-z]{0,3}$/;

export const fullNameField = z.string().trim().min(1, 'Nama wajib diisi').max(100, 'Maksimal 100 karakter');
export const emailField = z.string().trim().min(1, 'Email wajib diisi').max(150).email('Format email tidak valid');
export const phoneField = z.string().trim().regex(PHONE_REGEX, 'Format nomor HP tidak valid (contoh: 081234567890)');
// bcrypt hanya membaca 72 byte pertama — batas atas ini mencegah "password panjang" yang sebagian diam-diam diabaikan
export const passwordField = z.string().min(8, 'Password minimal 8 karakter').max(72, 'Password maksimal 72 karakter');

const passwordsMatch = (v: { password: string; confirmPassword: string }) => v.password === v.confirmPassword;
const mismatch = { path: ['confirmPassword'], message: 'Konfirmasi password tidak sama' };

const accountShape = {
  fullName: fullNameField,
  email: emailField,
  phone: phoneField,
  password: passwordField,
  confirmPassword: z.string(),
};

export const customerRegisterSchema = z.object(accountShape).refine(passwordsMatch, mismatch);

export const courierRegisterSchema = z
  .object({
    ...accountShape,
    vehicleType: z.enum(['MOTOR', 'CAR']),
    plateNumber: z.string().trim().max(15).regex(PLATE_REGEX, 'Format plat tidak valid (contoh: D 1234 ABC)'),
    licenseNo: z.string().trim().max(30, 'Maksimal 30 karakter'),
  })
  .refine(passwordsMatch, mismatch);

export type CustomerRegisterValues = z.infer<typeof customerRegisterSchema>;
export type CourierRegisterValues = z.infer<typeof courierRegisterSchema>;