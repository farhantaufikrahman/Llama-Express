import { api } from '../../lib/api/client';
import type { TokenResponse, User } from '../../lib/api/types';

export interface CustomerRegisterPayload {
  fullName: string;
  email: string;
  phone: string;
  password: string;
}

export interface CourierRegisterPayload extends CustomerRegisterPayload {
  vehicleType: 'MOTOR' | 'CAR';
  plateNumber: string;
  licenseNo?: string;
}

export const authApi = {
  login: (email: string, password: string) =>
    api<TokenResponse>('/api/v1/auth/login', { method: 'POST', body: { email, password }, auth: false }),
  registerCustomer: (body: CustomerRegisterPayload) =>
    api<TokenResponse>('/api/v1/auth/register', { method: 'POST', body, auth: false }),
  registerCourier: (body: CourierRegisterPayload) =>
    api<TokenResponse>('/api/v1/auth/register/courier', { method: 'POST', body, auth: false }),
  logout: (refreshToken: string) =>
    api<void>('/api/v1/auth/logout', { method: 'POST', body: { refreshToken }, auth: false }),
  me: () => api<User>('/api/v1/users/me'),
};