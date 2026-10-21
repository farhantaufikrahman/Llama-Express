export function Tabs<V extends string>({
  value,
  options,
  onChange,
  label,
}: {
  value: V;
  options: { value: V; label: string }[];
  onChange: (v: V) => void;
  label: string;
}) {
  return (
    <div
      role="tablist"
      aria-label={label}
      className="inline-flex gap-1 rounded-xl bg-white p-1"
    >
      {options.map((o) => (
        <button
          key={o.value}
          role="tab"
          aria-selected={o.value === value}
          onClick={() => onChange(o.value)}
          className={`rounded-lg px-3.5 py-2 text-sm font-bold ${o.value === value ? "bg-navy-900 text-gold-400" : "text-ink-muted hover:text-navy-900"}`}
        >
          {o.label}
        </button>
      ))}
    </div>
  );
}
