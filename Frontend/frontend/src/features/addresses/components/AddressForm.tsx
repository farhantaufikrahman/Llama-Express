import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '../../../components/Button';
import { TextField } from '../../../components/TextField';
import type { Address, AddressInput } from '../api';

// Koordinat disimpan sebagai string di form (input HTML selalu string), dikonversi ke number saat submit
const coordinate = (max: number, name: string) =>
  z.string().trim().min(1, `${name} wajib diisi`)
    .refine((v) => Number.isFinite(Number(v)) && Math.abs(Number(v)) <= max, `${name} harus antara -${max} dan ${max}`);

const schema = z.object({
  label: z.string().trim().min(1, 'Label wajib diisi').max(50),
  address: z.string().trim().min(1, 'Alamat wajib diisi').max(255),
  lat: coordinate(90, 'Latitude'),
  lng: coordinate(180, 'Longitude'),
  notes: z.string().trim().max(255),
});
type Values = z.infer<typeof schema>;

interface Props {
  initial?: Address;
  submitting: boolean;
  onSubmit: (input: AddressInput) => void;
  onCancel: () => void;
}

export function AddressForm({ initial, submitting, onSubmit, onCancel }: Props) {
  const [locating, setLocating] = useState(false);
  const [geoError, setGeoError] = useState<string | null>(null);
  const { register, handleSubmit, setValue, formState: { errors } } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      label: initial?.label ?? '',
      address: initial?.address ?? '',
      lat: initial ? String(initial.lat) : '',
      lng: initial ? String(initial.lng) : '',
      notes: initial?.notes ?? '',
    },
  });

  // Sementara (sampai peta Leaflet di F3): isi koordinat dari GPS perangkat
  const useMyLocation = () => {
    if (!('geolocation' in navigator)) {
      setGeoError('Browser tidak mendukung lokasi');
      return;
    }
    setLocating(true);
    setGeoError(null);
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setValue('lat', coords.latitude.toFixed(6), { shouldValidate: true });
        setValue('lng', coords.longitude.toFixed(6), { shouldValidate: true });
        setLocating(false);
      },
      () => {
        setGeoError('Izin lokasi ditolak atau lokasi tidak tersedia');
        setLocating(false);
      },
      { enableHighAccuracy: true, timeout: 10_000 },
    );
  };

  const submit = handleSubmit((v) =>
    onSubmit({ label: v.label, address: v.address, lat: Number(v.lat), lng: Number(v.lng), notes: v.notes || undefined }),
  );

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4 rounded-2xl bg-white p-5">
      <h2 className="text-lg font-extrabold">{initial ? 'Ubah alamat' : 'Alamat baru'}</h2>
      <TextField label="Label" placeholder="Rumah, Kantor, …" error={errors.label?.message} {...register('label')} />
      <TextField label="Alamat lengkap" error={errors.address?.message} {...register('address')} />
      <div className="grid grid-cols-2 gap-3">
        <TextField label="Latitude" inputMode="decimal" error={errors.lat?.message} {...register('lat')} />
        <TextField label="Longitude" inputMode="decimal" error={errors.lng?.message} {...register('lng')} />
      </div>
      <button type="button" onClick={useMyLocation} disabled={locating}
              className="self-start text-sm font-bold text-brand-blue hover:underline disabled:opacity-60">
        {locating ? 'Mencari lokasi…' : '📍 Pakai lokasi saya sekarang'}
      </button>
      {geoError && <p className="text-xs font-medium text-danger">{geoError}</p>}
      <TextField label="Catatan untuk kurir (opsional)" error={errors.notes?.message} {...register('notes')} />
      <div className="flex gap-2">
        <Button type="submit" disabled={submitting}>{submitting ? 'Menyimpan…' : 'Simpan'}</Button>
        <Button type="button" variant="secondary" onClick={onCancel}>Batal</Button>
      </div>
    </form>
  );
}