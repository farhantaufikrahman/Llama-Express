import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/client';

interface CourierProfile {
  fullName: string;
  vehicleType: 'MOTOR' | 'CAR';
  plateNumber: string;
  verificationStatus: 'PENDING' | 'VERIFIED' | 'REJECTED';
  online: boolean;
  ratingAvg: number | null;
  ratingCount: number;
}

export default function CourierHomePage() {
  const { data, isPending, isError, error } = useQuery({
    queryKey: ['courier', 'me'],
    queryFn: ({ signal }) => api<CourierProfile>('/api/v1/courier/me', { signal }),
  });

  if (isPending) return <p className="text-ink-muted">Memuat…</p>;
  if (isError) return <p role="alert" className="text-danger">{error.message}</p>;

  return (
    <section className="flex flex-col gap-4">
      <h1 className="text-2xl font-extrabold tracking-tight">Halo, {data.fullName}</h1>
      <div className="grid gap-3 sm:grid-cols-3">
        <Stat label="Kendaraan" value={`${data.vehicleType} · ${data.plateNumber}`} />
        <Stat label="Verifikasi" value={data.verificationStatus} />
        <Stat label="Status" value={data.online ? 'Online' : 'Offline'} />
      </div>
    </section>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl bg-white p-4">
      <p className="text-xs font-semibold text-ink-muted">{label}</p>
      <p className="mt-1 text-lg font-extrabold">{value}</p>
    </div>
  );
}