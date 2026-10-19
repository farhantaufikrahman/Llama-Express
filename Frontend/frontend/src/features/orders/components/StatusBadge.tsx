import { ORDER_STATUS } from '../status';
import type { OrderStatus } from '../types';

export function StatusBadge({ status }: { status: OrderStatus }) {
  const s = ORDER_STATUS[status];
  return <span className={`whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-bold ${s.tone}`}>{s.label}</span>;
}