import { createContext, useContext } from 'react';
import type { User } from '../../lib/api/types';
import type { CourierRegisterPayload, CustomerRegisterPayload } from './api';

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous';

export interface AuthContextValue {
  status: AuthStatus;
  user: User | null;
  login: (email: string, password: string) => Promise<User>;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth harus dipakai di dalam <AuthProvider>');
  }
  return ctx;
}

export interface AuthContextValue {
  status: AuthStatus;
  user: User | null;
  login: (email: string, password: string) => Promise<User>;
  register: (
    request: { kind: 'customer'; payload: CustomerRegisterPayload } | { kind: 'courier'; payload: CourierRegisterPayload },
  ) => Promise<User>;
  updateUser: (user: User) => void;
  logout: () => Promise<void>;
}