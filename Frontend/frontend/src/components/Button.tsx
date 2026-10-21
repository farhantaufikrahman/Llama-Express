import type { ComponentProps } from 'react';

type Variant = 'primary' | 'secondary' | 'danger';

const styles: Record<Variant, string> = {
  primary: 'bg-navy-900 text-gold-400 hover:bg-navy-800',
  secondary: 'border-[1.5px] border-line bg-white text-navy-900 hover:bg-cream',
  danger: 'bg-danger text-white hover:opacity-90',
};

export function Button({ variant = 'primary', className = '', ...props }: ComponentProps<'button'> & { variant?: Variant }) {
  return (
    <button
      {...props}
      className={`inline-flex h-12 items-center justify-center rounded-xl px-5 text-[15px] font-bold transition
        disabled:cursor-not-allowed disabled:opacity-60 ${styles[variant]} ${className}`}
    />
  );
}