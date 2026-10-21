export function Logo({ size = 36, withText = true, light = false }: { size?: number; withText?: boolean; light?: boolean }) {
  return (
    <span className="inline-flex items-center gap-2.5">
      <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true">
        <rect width="64" height="64" rx="16" fill="#1E2F55" />
        <path d="M30 37 C21 37 13 31 10 19 C16 25 21 26 26 26 C20 22 17 16 16 10 C22 17 28 21 34 23 Z" fill="#F5B841" />
        <path d="M24 53 C24 43 27 35 33 29 L35 18 L39 22 L45 20 C50 22 52 26 50 30 L44 31 C42 35 42 43 45 53 Z" fill="#F7F6F2" />
      </svg>
      {withText && (
        <span className={`text-lg font-extrabold tracking-tight ${light ? 'text-cream' : 'text-navy-900'}`}>Llama Express</span>
      )}
    </span>
  );
}