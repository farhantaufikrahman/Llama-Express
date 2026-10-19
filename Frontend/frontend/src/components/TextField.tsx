import { useId, type ComponentProps } from 'react';

type Props = ComponentProps<'input'> & { label: string; error?: string };

/** React 19: `ref` adalah prop biasa → {...register('email')} dari react-hook-form langsung bekerja */
export function TextField({ label, error, className = '', ...inputProps }: Props) {
  const id = useId();
  const errorId = `${id}-error`;
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-xs font-semibold text-ink-muted">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={`h-12 rounded-xl border-[1.5px] bg-white px-3.5 text-[15px] outline-none transition
          focus:border-brand-blue ${error ? 'border-danger' : 'border-line'} ${className}`}
        {...inputProps}
      />
      {error && (
        <p id={errorId} className="text-xs font-medium text-danger">
          {error}
        </p>
      )}
    </div>
  );
}