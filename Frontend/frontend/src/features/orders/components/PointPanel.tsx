import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { addressApi, addressKeys } from '../../addresses/api';
import { searchPlaces, type Place } from '../../map/geocoding';
import type { Location } from '../types';

interface Props {
  kind: 'pickup' | 'dropoff';
  value: Location | null;
  active: boolean;
  error?: string;
  addressError?: string;
  onAddressChange: (text: string) => void;
  onActivate: () => void;
  onChoose: (place: Location) => void;
}

export function PointPanel({ kind, value, active, error, addressError, onAddressChange, onActivate, onChoose }: Props) {
  const title = kind === 'pickup' ? 'Jemput dari' : 'Antar ke';
  const dot = kind === 'pickup' ? 'size-3 rounded-full bg-brand-blue' : 'size-3 rounded-[3px] bg-gold-400';

  const [query, setQuery] = useState('');
  const [results, setResults] = useState<Place[] | null>(null);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  const { data: saved = [] } = useQuery({
    queryKey: addressKeys.all,
    queryFn: ({ signal }) => addressApi.list(signal),
  });

  // Nominatim: cari saat tombol ditekan / Enter — BUKAN autocomplete per ketikan (aturan pemakaian)
  const runSearch = async () => {
    const q = query.trim();
    if (q.length < 3) return;
    setSearching(true);
    setSearchError(null);
    try {
      setResults(await searchPlaces(q));
    } catch (e) {
      setSearchError(e instanceof Error ? e.message : 'Pencarian gagal');
    } finally {
      setSearching(false);
    }
  };

  return (
    <div
      className={`flex flex-col gap-3 rounded-2xl border-[1.5px] bg-white p-4 ${
        active ? 'border-navy-900' : 'border-transparent'
      }`}
    >
      <div className="flex items-center justify-between gap-2">
        <span className="flex items-center gap-2 text-sm font-extrabold">
          <span className={dot} />
          {title}
        </span>
        <button
          type="button"
          onClick={onActivate}
          aria-pressed={active}
          className={`rounded-lg px-2.5 py-1.5 text-xs font-bold ${
            active ? 'bg-navy-900 text-gold-400' : 'bg-cream text-navy-900'
          }`}
        >
          {active ? 'Klik di peta…' : 'Pilih di peta'}
        </button>
      </div>

      {saved.length > 0 && (
        <select
          aria-label={`${title}: alamat tersimpan`}
          className="h-11 rounded-xl border-[1.5px] border-line bg-white px-3 text-sm"
          value=""
          onChange={(e) => {
            const a = saved.find((s) => String(s.id) === e.target.value);
            if (a) onChoose({ address: a.address, lat: Number(a.lat), lng: Number(a.lng) });
          }}
        >
          <option value="">Pakai alamat tersimpan…</option>
          {saved.map((a) => (
            <option key={a.id} value={a.id}>
              {a.label} — {a.address}
            </option>
          ))}
        </select>
      )}

      <div className="flex gap-2">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault(); // jangan submit form order
              void runSearch();
            }
          }}
          placeholder="Cari tempat, mis. Gedung Sate"
          aria-label={`Cari lokasi ${title.toLowerCase()}`}
          className="h-11 min-w-0 flex-1 rounded-xl border-[1.5px] border-line px-3 text-sm outline-none focus:border-brand-blue"
        />
        <button
          type="button"
          onClick={() => void runSearch()}
          disabled={searching || query.trim().length < 3}
          className="rounded-xl bg-cream px-3.5 text-sm font-bold text-navy-900 disabled:opacity-50"
        >
          {searching ? '…' : 'Cari'}
        </button>
      </div>
      {searchError && <p className="text-xs font-medium text-danger">{searchError}</p>}

      {results && (
        <ul className="flex flex-col divide-y divide-line rounded-xl border border-line">
          {results.length === 0 && <li className="p-3 text-sm text-ink-muted">Tidak ditemukan</li>}
          {results.map((r) => (
            <li key={`${r.lat},${r.lng}`}>
              <button
                type="button"
                className="w-full p-3 text-left text-sm hover:bg-cream"
                onClick={() => {
                  onChoose({ address: r.label, lat: r.lat, lng: r.lng });
                  setResults(null);
                  setQuery('');
                }}
              >
                {r.label}
              </button>
            </li>
          ))}
        </ul>
      )}

      {value ? (
        <div className="flex flex-col gap-1">
          <label className="text-xs font-semibold text-ink-muted" htmlFor={`${kind}-address`}>
            Detail alamat (bisa diubah)
          </label>
          <textarea
            id={`${kind}-address`}
            rows={2}
            value={value.address}
            onChange={(e) => onAddressChange(e.target.value)}
            aria-invalid={addressError ? true : undefined}
            className={`rounded-xl border-[1.5px] px-3 py-2 text-sm outline-none focus:border-brand-blue ${
              addressError ? 'border-danger' : 'border-line'
            }`}
          />
          {addressError && <p className="text-xs font-medium text-danger">{addressError}</p>}
        </div>
      ) : (
        <p className={`text-sm ${error ? 'font-medium text-danger' : 'text-ink-muted'}`}>
          {error ?? 'Belum dipilih — klik "Pilih di peta", cari tempat, atau pakai alamat tersimpan.'}
        </p>
      )}
    </div>
  );
}