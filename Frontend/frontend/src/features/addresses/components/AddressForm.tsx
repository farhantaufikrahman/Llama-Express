import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '../../../components/Button';
import { TextField } from '../../../components/TextField';
import { PickMap, type LatLng } from '../../map/PickMap';
import { reverseGeocode, round6 } from '../../map/geocoding';
import type { Address, AddressInput } from '../api';

// Koordinat disimpan sebagai string di form (nilai input HTML selalu string), dikonversi ke number saat submit
const coordinate = (max: number, name: string) =>
  z
    .string()
    .trim()
    .min(1, `${name} wajib diisi`)
    .refine((v) => Number.isFinite(Number(v)) && Math.abs(Number(v)) <= max, `${name} harus antara -${max} dan ${max}`);

const schema = z.object({
  label: z.string().trim().min(1, 'Label wajib diisi').max(50, 'Maksimal 50 karakter'),
  address: z.string().trim().min(1, 'Alamat wajib diisi').max(255, 'Maksimal 255 karakter'),
  lat: coordinate(90, 'Latitude'),
  lng: coordinate(180, 'Longitude'),
  notes: z.string().trim().max(255, 'Maksimal 255 karakter'),
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

  const {
    register,
    handleSubmit,
    setValue,
    getValues,
    watch,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      label: initial?.label ?? '',
      address: initial?.address ?? '',
      lat: initial ? String(initial.lat) : '',
      lng: initial ? String(initial.lng) : '',
      notes: initial?.notes ?? '',
    },
  });

  // Titik di peta mengikuti isi field latitude/longitude (diketik manual pun ikut berpindah)
  const lat = watch('lat');
  const lng = watch('lng');
  const hasPoint = lat.trim() !== '' && lng.trim() !== '' && Number.isFinite(Number(lat)) && Number.isFinite(Number(lng));

  /** Dipakai klik peta DAN tombol GPS: isi koordinat, lalu isi alamat otomatis kalau masih kosong */
  const setPoint = async ({ lat: la, lng: ln }: LatLng) => {
    const point = { lat: round6(la), lng: round6(ln) };
    setValue('lat', String(point.lat), { shouldValidate: true, shouldDirty: true });
    setValue('lng', String(point.lng), { shouldValidate: true, shouldDirty: true });

    if (getValues('address').trim() === '') {
      const name = await reverseGeocode(point.lat, point.lng);
      // Abaikan hasil yang terlambat kalau titik sudah dipindah lagi atau alamat sudah diketik manual
      const stillSamePoint = getValues('lat') === String(point.lat) && getValues('lng') === String(point.lng);
      if (name && stillSamePoint && getValues('address').trim() === '') {
        setValue('address', name, { shouldValidate: true, shouldDirty: true });
      }
    }
  };

  // Ambil lokasi dari GPS perangkat (hanya jalan di HTTPS atau localhost)
  const fillFromGps = () => {
    if (!('geolocation' in navigator)) {
      setGeoError('Browser tidak mendukung lokasi');
      return;
    }
    setLocating(true);
    setGeoError(null);
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setLocating(false);
        void setPoint({ lat: coords.latitude, lng: coords.longitude });
      },
      () => {
        setGeoError('Izin lokasi ditolak atau lokasi tidak tersedia');
        setLocating(false);
      },
      { enableHighAccuracy: true, timeout: 10_000 },
    );
  };

  const submit = handleSubmit((v) =>
    onSubmit({
      label: v.label,
      address: v.address,
      lat: Number(v.lat),
      lng: Number(v.lng),
      notes: v.notes || undefined,
    }),
  );

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4 rounded-2xl bg-white p-5">
      <h2 className="text-lg font-extrabold">{initial ? 'Ubah alamat' : 'Alamat baru'}</h2>

      <TextField label="Label" placeholder="Rumah, Kantor, …" error={errors.label?.message} {...register('label')} />

      <div className="flex flex-col gap-1.5">
        <PickMap
          className="h-56"
          onPick={setPoint}
          markers={hasPoint ? [{ id: 'point', kind: 'point', position: { lat: Number(lat), lng: Number(lng) }, title: 'Lokasi' }] : []}
        />
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="text-xs text-ink-muted">Klik peta untuk menandai lokasi.</p>
          <button
            type="button"
            onClick={fillFromGps}
            disabled={locating}
            className="text-sm font-bold text-brand-blue hover:underline disabled:opacity-60"
          >
            {locating ? 'Mencari lokasi…' : '📍 Pakai lokasi saya sekarang'}
          </button>
        </div>
        {geoError && <p className="text-xs font-medium text-danger">{geoError}</p>}
      </div>

      <TextField label="Alamat lengkap" error={errors.address?.message} {...register('address')} />

      <div className="grid grid-cols-2 gap-3">
        <TextField label="Latitude" inputMode="decimal" error={errors.lat?.message} {...register('lat')} />
        <TextField label="Longitude" inputMode="decimal" error={errors.lng?.message} {...register('lng')} />
      </div>

      <TextField label="Catatan untuk kurir (opsional)" error={errors.notes?.message} {...register('notes')} />

      <div className="flex gap-2">
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Menyimpan…' : 'Simpan'}
        </Button>
        <Button type="button" variant="secondary" onClick={onCancel}>
          Batal
        </Button>
      </div>
    </form>
  );
}