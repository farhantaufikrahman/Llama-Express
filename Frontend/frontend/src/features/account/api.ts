import { api } from '../../lib/api/client';
import type { User } from '../../lib/api/types';

export const accountApi = {
  updateProfile: (body: { fullName?: string; phone?: string }) =>
    api<User>('/api/v1/users/me', { method: 'PATCH', body }),
  changePassword: (body: { currentPassword: string; newPassword: string }) =>
    api<void>('/api/v1/users/me/password', { method: 'POST', body }),
};