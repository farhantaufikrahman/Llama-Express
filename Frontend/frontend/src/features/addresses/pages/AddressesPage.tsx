import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '../../../components/Button';
import { Alert } from '../../../components/Alert';
import { isApiError } from '../../../lib/api/errors';
import { addressApi, addressKeys, type Address, type AddressInput } from '../api';
import { AddressForm } from '../components/AddressForm';

type Editing = { mode: 'create' } | { mode: 'edit'; address: Address } | null;

export default function AddressesPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Editing>(null);
  const [error, setError] = useState<string | null>(null);

  const { data: addresses = [], isPending } = useQuery({
    queryKey: addressKeys.all,
    queryFn: ({ signal }) => addressApi.list(signal),
  });

  const onDone = () => {
    setEditing(null);
    setError(null);
    return queryClient.invalidateQueries({ queryKey: addressKeys.all }); // daftar diambil ulang otomatis
  };
  const onFail = (e: unknown) => setError(isApiError(e) ? e.message : 'Gagal menyimpan alamat');

  const save = useMutation({
    mutationFn: ({ id, input }: { id?: number; input: AddressInput }) =>
      id ? addressApi.update(id, input) : addressApi.create(input),
    onSuccess: onDone,
    onError: onFail,
  });

  const remove = useMutation({ mutationFn: addressApi.remove, onSuccess: onDone, onError: onFail });

  return (
    <section className="flex max-w-xl flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-extrabold tracking-tight">Alamat tersimpan</h1>
        {!editing && (
          <Button onClick={() => setEditing({ mode: 'create' })} disabled={addresses.length >= 10}>
            + Tambah
          </Button>
        )}
      </div>

      {error && <Alert>{error}</Alert>}
      {addresses.length >= 10 && !editing && <Alert kind="info">Maksimal 10 alamat. Hapus salah satu untuk menambah.</Alert>}

      {editing && (
        <AddressForm
          initial={editing.mode === 'edit' ? editing.address : undefined}
          submitting={save.isPending}
          onCancel={() => setEditing(null)}
          onSubmit={(input) => save.mutate({ id: editing.mode === 'edit' ? editing.address.id : undefined, input })}
        />
      )}

      {isPending && <p className="text-ink-muted">Memuat…</p>}
      {!isPending && addresses.length === 0 && !editing && (
        <p className="text-ink-muted">Belum ada alamat. Simpan alamat yang sering dipakai supaya order lebih cepat.</p>
      )}

      <ul className="flex flex-col gap-3">
        {addresses.map((a) => (
          <li key={a.id} className="flex items-start justify-between gap-3 rounded-2xl bg-white p-4">
            <div className="min-w-0">
              <p className="font-bold">{a.label}</p>
              <p className="text-sm text-ink-muted">{a.address}</p>
              {a.notes && <p className="mt-1 text-xs text-ink-muted">“{a.notes}”</p>}
            </div>
            <div className="flex shrink-0 gap-1">
              <button onClick={() => setEditing({ mode: 'edit', address: a })}
                      className="rounded-lg px-2.5 py-1.5 text-sm font-bold text-navy-900 hover:bg-cream">Ubah</button>
              <button
                onClick={() => window.confirm(`Hapus alamat "${a.label}"?`) && remove.mutate(a.id)}
                disabled={remove.isPending}
                className="rounded-lg px-2.5 py-1.5 text-sm font-bold text-danger hover:bg-danger/10"
              >
                Hapus
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}