import type { UseFormRegisterReturn } from 'react-hook-form';

interface Option<V extends string> {
  value: V;
  title: string;
  hint?: string;
}

/** Radio button bergaya kartu — fieldset+legend supaya screen reader membacakan judul grupnya */
export function ChoiceGroup<V extends string>({ legend, options, registration, columns = 2 }: {
  legend: string;
  options: Option<V>[];
  registration: UseFormRegisterReturn;
  columns?: 2 | 3;
}) {
  return (
    <fieldset className="flex flex-col gap-1.5">
      <legend className="mb-1.5 text-xs font-semibold text-ink-muted">{legend}</legend>
      <div className={`grid gap-2 ${columns === 3 ? 'grid-cols-3' : 'grid-cols-2'}`}>
        {options.map((o) => (
          <label key={o.value}
                 className="flex cursor-pointer flex-col gap-0.5 rounded-xl border-[1.5px] border-line px-3.5 py-3
                            has-[:checked]:border-navy-900 has-[:checked]:bg-cream has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-brand-blue">
            <span className="flex items-center gap-2">
              <input type="radio" value={o.value} {...registration} className="accent-navy-900" />
              <span className="text-sm font-bold">{o.title}</span>
            </span>
            {o.hint && <span className="pl-6 text-xs text-ink-muted">{o.hint}</span>}
          </label>
        ))}
      </div>
    </fieldset>
  );
}