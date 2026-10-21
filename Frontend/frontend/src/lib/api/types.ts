export type Role = "CUSTOMER" | "COURIER" | "ADMIN";

export interface User {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  role: Role;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  expiresIn: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
