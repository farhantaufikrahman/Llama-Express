export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly requestId?: string;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, body: Record<string, unknown> | null) {
    const detail = typeof body?.detail === 'string' ? body.detail : `Request gagal (HTTP ${status})`;
    super(detail);
    this.name = 'ApiError';
    this.status = status;
    this.code = typeof body?.code === 'string' ? body.code : 'UNKNOWN';
    this.requestId = typeof body?.requestId === 'string' ? body.requestId : undefined;
    this.fieldErrors = (body?.errors as Record<string, string> | undefined) ?? {};
  }
}

export function isApiError(e: unknown): e is ApiError {
  return e instanceof ApiError;
}