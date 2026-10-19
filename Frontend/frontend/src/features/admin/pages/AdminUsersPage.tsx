import { useState } from "react";
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { Alert } from "../../../components/Alert";
import { ConfirmDialog } from "../../../components/ConfirmDialog";
import { Pagination } from "../../../components/Pagination";
import { Tabs } from "../../../components/Tabs";
import { formatDateTime } from "../../../lib/format";
import { adminApi, adminKeys } from "../api";
import type { AdminUser } from "../types";

type Role = "CUSTOMER" | "COURIER";

export default function AdminUsersPage() {
  const queryClient = useQueryClient();
  const [role, setRole] = useState<Role>("CUSTOMER");
  const [input, setInput] = useState("");
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const [target, setTarget] = useState<AdminUser | null>(null);

  const { data, isPending, isFetching } = useQuery({
    queryKey: adminKeys.users(role, q, page),
    queryFn: ({ signal }) => adminApi.users(role, q, page, signal),
    placeholderData: keepPreviousData,
  });

  const toggle = useMutation({
    mutationFn: (u: AdminUser) =>
      u.status === "ACTIVE"
        ? adminApi.suspendUser(u.id)
        : adminApi.activateUser(u.id),
    onSuccess: () => {
      setTarget(null);
      void queryClient.invalidateQueries({ queryKey: adminKeys.all });
    },
  });

  return (
    <section className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-[26px] font-extrabold tracking-tight">Pengguna</h1>
        <Tabs
          label="Jenis pengguna"
          value={role}
          onChange={(r) => {
            setRole(r);
            setPage(0);
          }}
          options={[
            { value: "CUSTOMER", label: "Customer" },
            { value: "COURIER", label: "Kurir" },
          ]}
        />
      </div>

      <form
        className="flex gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          setQ(input.trim());
          setPage(0);
        }}
      >
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Cari nama, email, atau no. HP"
          aria-label="Cari pengguna"
          className="h-11 min-w-0 flex-1 rounded-xl border-[1.5px] border-line bg-white px-3 text-sm"
        />
        <button className="rounded-xl bg-navy-900 px-4 text-sm font-bold text-gold-400">
          Cari
        </button>
      </form>

      <div
        className={`overflow-x-auto rounded-2xl bg-white p-4 ${isFetching ? "opacity-70" : ""}`}
      >
        <table className="w-full min-w-[720px] text-sm">
          <thead>
            <tr className="text-left text-xs uppercase tracking-wider text-ink-muted">
              {["Nama", "Email", "HP", "Bergabung", "Status", ""].map((h) => (
                <th key={h} className="px-2 py-2.5">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {isPending && (
              <tr>
                <td colSpan={6} className="p-4 text-ink-muted">
                  Memuat…
                </td>
              </tr>
            )}
            {data?.content.length === 0 && (
              <tr>
                <td colSpan={6} className="p-4 text-ink-muted">
                  Tidak ditemukan.
                </td>
              </tr>
            )}
            {data?.content.map((u) => (
              <tr key={u.id} className="border-t border-line">
                <td className="px-2 py-3 font-bold">{u.fullName}</td>
                <td className="px-2 py-3">{u.email}</td>
                <td className="px-2 py-3">{u.phone}</td>
                <td className="px-2 py-3 text-ink-muted">
                  {formatDateTime(u.createdAt)}
                </td>
                <td className="px-2 py-3">
                  <span
                    className={`rounded-full px-2.5 py-1 text-xs font-bold ${u.status === "ACTIVE" ? "bg-[#E7F4EC] text-[#15803D]" : "bg-[#FBEAEA] text-[#B42318]"}`}
                  >
                    {u.status === "ACTIVE" ? "Aktif" : "Dibekukan"}
                  </span>
                </td>
                <td className="px-2 py-3 text-right">
                  <button
                    onClick={() => setTarget(u)}
                    className={`rounded-lg px-3 py-1.5 text-sm font-bold ${u.status === "ACTIVE" ? "text-danger hover:bg-danger/10" : "text-success hover:bg-success/10"}`}
                  >
                    {u.status === "ACTIVE" ? "Bekukan" : "Aktifkan"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {data && (
          <Pagination
            page={data.page}
            size={data.size}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onChange={setPage}
          />
        )}
      </div>

      {toggle.isError && <Alert>{toggle.error.message}</Alert>}

      <ConfirmDialog
        open={target !== null}
        danger={target?.status === "ACTIVE"}
        title={
          target?.status === "ACTIVE"
            ? `Bekukan ${target.fullName}?`
            : `Aktifkan kembali ${target?.fullName}?`
        }
        description={
          target?.status === "ACTIVE"
            ? "Pengguna langsung keluar dari semua perangkat dan tidak bisa login. Kurir yang sedang online akan di-offline-kan."
            : "Pengguna bisa login kembali."
        }
        confirmLabel={target?.status === "ACTIVE" ? "Bekukan" : "Aktifkan"}
        busy={toggle.isPending}
        onConfirm={() => target && toggle.mutate(target)}
        onClose={() => setTarget(null)}
      />
    </section>
  );
}
