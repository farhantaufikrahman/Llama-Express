import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert } from "../../../components/Alert";
import { Button } from "../../../components/Button";
import { Pagination } from "../../../components/Pagination";
import { Tabs } from "../../../components/Tabs";
import { isApiError } from "../../../lib/api/errors";
import { adminApi, adminKeys } from "../api";
import type { VerificationStatus } from "../types";

export default function AdminCouriersPage() {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<VerificationStatus>("PENDING");
  const [page, setPage] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const { data, isPending } = useQuery({
    queryKey: adminKeys.couriers(status, page),
    queryFn: ({ signal }) => adminApi.couriers(status, page, signal),
  });

  const decide = useMutation({
    mutationFn: ({ id, approve }: { id: number; approve: boolean }) =>
      approve ? adminApi.verifyCourier(id) : adminApi.rejectCourier(id),
    onSuccess: () => {
      setError(null);
      void queryClient.invalidateQueries({ queryKey: adminKeys.all }); // daftar + badge dashboard ikut segar
    },
    onError: (e) => setError(isApiError(e) ? e.message : "Gagal memproses"),
  });

  return (
    <section className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-[26px] font-extrabold tracking-tight">Kurir</h1>
        <Tabs
          label="Status verifikasi"
          value={status}
          onChange={(s) => {
            setStatus(s);
            setPage(0);
          }}
          options={[
            { value: "PENDING", label: "Menunggu" },
            { value: "VERIFIED", label: "Terverifikasi" },
            { value: "REJECTED", label: "Ditolak" },
          ]}
        />
      </div>
      {error && <Alert>{error}</Alert>}
      {isPending && <p className="text-ink-muted">Memuat…</p>}
      {data?.content.length === 0 && (
        <p className="text-ink-muted">Tidak ada kurir dengan status ini.</p>
      )}

      <ul className="grid gap-3 md:grid-cols-2">
        {data?.content.map((c) => (
          <li
            key={c.userId}
            className="flex flex-col gap-3 rounded-2xl bg-white p-4"
          >
            <div>
              <p className="font-bold">{c.fullName}</p>
              <p className="text-sm text-ink-muted">{c.phone}</p>
              <p className="mt-1 text-sm">
                <b>{c.vehicleType === "MOTOR" ? "Motor" : "Mobil"}</b> ·{" "}
                <span className="font-mono">{c.plateNumber}</span>
                {c.verificationStatus === "VERIFIED" &&
                  ` · ${c.online ? "🟢 online" : "offline"}`}
              </p>
            </div>
            <div className="flex gap-2">
              {c.verificationStatus !== "VERIFIED" && (
                <Button
                  className="h-10"
                  disabled={decide.isPending}
                  onClick={() => decide.mutate({ id: c.userId, approve: true })}
                >
                  Setujui
                </Button>
              )}
              {c.verificationStatus !== "REJECTED" && (
                <Button
                  className="h-10"
                  variant="secondary"
                  disabled={decide.isPending}
                  onClick={() =>
                    decide.mutate({ id: c.userId, approve: false })
                  }
                >
                  Tolak
                </Button>
              )}
            </div>
          </li>
        ))}
      </ul>
      {data && (
        <Pagination
          page={data.page}
          size={data.size}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          onChange={setPage}
        />
      )}
    </section>
  );
}
