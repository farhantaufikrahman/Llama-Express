import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { formatRupiah } from "../../../lib/format";
import { courierApi, courierKeys } from "../api";

/** "2026-10" → ["2026-10-01", "2026-10-31"] */
function monthRange(month: string): [string, string] {
  const [y, m] = month.split("-").map(Number);
  const last = new Date(y, m, 0).getDate(); // hari ke-0 bulan berikutnya = hari terakhir bulan ini
  return [`${month}-01`, `${month}-${String(last).padStart(2, "0")}`];
}

const thisMonth = () =>
  new Date()
    .toLocaleDateString("sv-SE", { timeZone: "Asia/Jakarta" })
    .slice(0, 7); // "YYYY-MM"

export default function CourierEarningsPage() {
  const [month, setMonth] = useState(thisMonth);
  const [from, to] = monthRange(month);
  const { data, isPending, isError, error } = useQuery({
    queryKey: courierKeys.earnings(from, to),
    queryFn: ({ signal }) => courierApi.earnings(from, to, signal),
  });

  return (
    <section className="flex max-w-xl flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-extrabold tracking-tight">Penghasilan</h1>
        <input
          type="month"
          value={month}
          max={thisMonth()}
          onChange={(e) => e.target.value && setMonth(e.target.value)}
          aria-label="Pilih bulan"
          className="h-11 rounded-xl border-[1.5px] border-line bg-white px-3 text-sm"
        />
      </div>
      {isPending && <p className="text-ink-muted">Memuat…</p>}
      {isError && (
        <p role="alert" className="text-danger">
          {error.message}
        </p>
      )}
      {data && (
        <>
          <div className="rounded-2xl bg-navy-900 p-5 text-cream">
            <p className="text-xs font-semibold text-[#C9D1E3]">
              Penghasilan kamu · {data.deliveredCount} pengantaran
            </p>
            <p className="text-3xl font-extrabold text-gold-400">
              {formatRupiah(data.courierEarning)}
            </p>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Stat
              label="Tunai sudah dipegang"
              value={formatRupiah(data.cashCollected)}
            />
            <Stat
              label={
                data.netPayout >= 0
                  ? "Akan ditransfer ke kamu"
                  : "Perlu disetor ke platform"
              }
              value={formatRupiah(Math.abs(data.netPayout))}
              danger={data.netPayout < 0}
            />
          </div>
          <div className="overflow-hidden rounded-2xl bg-white">
            <table className="w-full text-sm">
              <thead className="bg-cream text-left text-xs uppercase tracking-wide text-ink-muted">
                <tr>
                  <th className="p-3">Tanggal</th>
                  <th className="p-3 text-right">Order</th>
                  <th className="p-3 text-right">Penghasilan</th>
                </tr>
              </thead>
              <tbody>
                {data.days.length === 0 && (
                  <tr>
                    <td colSpan={3} className="p-4 text-center text-ink-muted">
                      Belum ada pengantaran di bulan ini.
                    </td>
                  </tr>
                )}
                {data.days.map((d) => (
                  <tr key={d.date} className="border-t border-line">
                    <td className="p-3">
                      {new Date(`${d.date}T00:00:00`).toLocaleDateString(
                        "id-ID",
                        { weekday: "short", day: "numeric", month: "short" },
                      )}
                    </td>
                    <td className="p-3 text-right">{d.deliveredCount}</td>
                    <td className="p-3 text-right font-bold">
                      {formatRupiah(d.courierEarning)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  );
}

function Stat({
  label,
  value,
  danger = false,
}: {
  label: string;
  value: string;
  danger?: boolean;
}) {
  return (
    <div className="rounded-2xl bg-white p-4">
      <p className="text-xs font-semibold text-ink-muted">{label}</p>
      <p
        className={`mt-1 text-lg font-extrabold ${danger ? "text-danger" : ""}`}
      >
        {value}
      </p>
    </div>
  );
}
