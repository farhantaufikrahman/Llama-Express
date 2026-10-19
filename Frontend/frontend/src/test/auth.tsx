import type { AuthContextValue } from "../features/auth/AuthContext";
import type { Role, User } from "../lib/api/types";
import { customer } from "./fixtures";

export const userWithRole = (role: Role): User => ({ ...customer, role });

export function fakeAuth(
  overrides: Partial<AuthContextValue> = {},
): AuthContextValue {
  return {
    status: "authenticated",
    user: customer,
    login: jest.fn(),
    register: jest.fn(),
    updateUser: jest.fn(),
    logout: jest.fn(),
    ...overrides,
  } as AuthContextValue;
}
