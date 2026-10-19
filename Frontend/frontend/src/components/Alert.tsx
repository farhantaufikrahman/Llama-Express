import type { ReactNode } from 'react';

const tone = {
  error: 'bg-danger/10 text-danger',
  success: 'bg-success/10 text-success',
  info: 'bg-brand-blue/10 text-brand-blue',
};

export function Alert({ kind = 'error', children }: { kind?: keyof typeof tone; children: ReactNode }) {
  return (
    <div role={kind === 'error' ? 'alert' : 'status'} className={`rounded-xl px-3.5 py-3 text-sm font-medium ${tone[kind]}`}>
      {children}
    </div>
  );
}