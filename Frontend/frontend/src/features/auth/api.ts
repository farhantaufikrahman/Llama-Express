import { api } from '../../lib/api/client';
import type { TokenResponse, User } from '../../lib/api/types';

export const authApi = {
  login: (email: string, password: string) =>
    api<TokenResponse>('/api/v1/auth/login', { method: 'POST', body: { email, password }, auth: false }),
  logout: (refreshToken: string) =>
    api<void>('/api/v1/auth/logout', { method: 'POST', body: { refreshToken }, auth: false }),
  me: () => api<User>('/api/v1/users/me'),
};