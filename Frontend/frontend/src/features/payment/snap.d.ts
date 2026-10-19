export {};

declare global {
  interface SnapResult {
    order_id: string;
    transaction_status: string;
    status_code: string;
    payment_type?: string;
  }

  interface SnapCallbacks {
    onSuccess?: (result: SnapResult) => void;
    onPending?: (result: SnapResult) => void;
    onError?: (result: SnapResult) => void;
    onClose?: () => void;
  }

  interface Window {
    snap?: { pay: (token: string, callbacks?: SnapCallbacks) => void };
  }
}