import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { Alert } from "../../../components/Alert";
import { Button } from "../../../components/Button";
import { ChoiceGroup } from "../../../components/ChoiceGroup";
import { TextField } from "../../../components/TextField";
import { formatDateTime, formatRupiah } from "../../../lib/format";
import { applyServerErrors } from "../../../lib/forms";
import { adminApi, adminKeys } from "../api";
import type { Tariff } from "../types";

const rupiahField = z
  .string()
  .trim()
  .regex(/^\d{1,10}$/, "Angka bulat, tanpa titik/koma");
const schema = z
  .object({
    serviceType: z.enum(["INSTANT", "SAME_DAY"]),
    vehicleType: z.enum(["MOTOR", "CAR"]),
    baseFare: rupiahField,
    perKmFare: rupiahField,
    minFare: rupiahField,
    activeFrom: z.string(), // datetime-local; kosong = mulai sekarang
  })
  .refine((v) => Number(v.minFare) >= Number(v.baseFare), {
    path: ["minFare"],
    message: "Tarif minimum tidak boleh < tarif dasar",
  });
type Values = z.infer<typeof schema>;

const label = (t: Pick<Tariff, "serviceType" | "vehicleType">) =>
  `${t.serviceType === "INSTANT" ? "Instant" : "Same Day"} · ${t.vehicleType === "MOTOR" ? "Motor" : "Mobil"}`;

function stateOf(t: Tariff): { text: string; tone: string } {
  if (t.active) return { text: "Berlaku", tone: "bg-[#E7F4EC] text-[#15803D]" };
  if (new Date(t.activeFrom).getTime() > Date.now())
    return { text: "Terjadwal", tone: "bg-[#E8EFFE] text-[#1D4ED8]" };
  return { text: "Riwayat", tone: "bg-cream text-ink-muted" };
}

export default function AdminTariffsPage() {
  const queryClient = useQueryClient();
  const [banner, setBanner] = useState<{
    kind: "success" | "error";
    text: string;
  } | null>(null);
  const { data: tariffs = [], isPending } = useQuery({
    queryKey: adminKeys.tariffs,
    queryFn: ({ signal }) => adminApi.tariffs(signal),
  });

  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      serviceType: "INSTANT",
      vehicleType: "MOTOR",
      baseFare: "",
      perKmFare: "",
      minFare: "",
      activeFrom: "",
    },
  });

  const create = useMutation({
    mutationFn: (v: Values) =>
      adminApi.createTariff({
        serviceType: v.serviceType,
        vehicleType: v.vehicleType,
        baseFare: Number(v.baseFare),
        perKmFare: Number(v.perKmFare),
        minFare: Number(v.minFare),
        // datetime-local tidak punya zona waktu → new Date() membacanya sebagai waktu LOKAL browser → ISO UTC
        activeFrom: v.activeFrom
          ? new Date(v.activeFrom).toISOString()
          : undefined,
      }),
    onSuccess: (t) => {
      setBanner({
        kind: "success",
        text: `Tarif ${label(t)} disimpan (${stateOf(t).text.toLowerCase()}).`,
      });
      form.reset({
        ...form.getValues(),
        baseFare: "",
        perKmFare: "",
        minFare: "",
        activeFrom: "",
      });
      void queryClient.invalidateQueries({ queryKey: adminKeys.tariffs });
    },
    onError: (e) => {
      const msg = applyServerErrors(e, form.setError, [
        "baseFare",
        "perKmFare",
        "minFare",
        "activeFrom",
      ]);
      if (msg) setBanner({ kind: "error", text: msg }); // TARIFF_OVERLAP, ACTIVE_FROM_IN_PAST, …
    },
  });

  const { errors } = form.formState;
  return (
    <section className="flex flex-col gap-5">
      <h1 className="text-[26px] font-extrabold tracking-tight">Tarif</h1>

      <form
        onSubmit={form.handleSubmit((v) => {
          setBanner(null);
          create.mutate(v);
        })}
        noValidate
        className="grid gap-4 rounded-2xl bg-white p-5 md:grid-cols-2"
      >
        <h2 className="text-lg font-extrabold md:col-span-2">Tarif baru</h2>
        {banner && (
          <div className="md:col-span-2">
            <Alert kind={banner.kind}>{banner.text}</Alert>
          </div>
        )}
        <ChoiceGroup
          legend="Layanan"
          registration={form.register("serviceType")}
          options={[
            { value: "INSTANT", title: "Instant" },
            { value: "SAME_DAY", title: "Same Day" },
          ]}
        />
        <ChoiceGroup
          legend="Kendaraan"
          registration={form.register("vehicleType")}
          options={[
            { value: "MOTOR", title: "Motor" },
            { value: "CAR", title: "Mobil" },
          ]}
        />
        <div className="grid grid-cols-3 gap-3 md:col-span-2">
          <TextField
            label="Tarif dasar (Rp)"
            inputMode="numeric"
            error={errors.baseFare?.message}
            {...form.register("baseFare")}
          />
          <TextField
            label="Per km (Rp)"
            inputMode="numeric"
            error={errors.perKmFare?.message}
            {...form.register("perKmFare")}
          />
          <TextField
            label="Minimum (Rp)"
            inputMode="numeric"
            error={errors.minFare?.message}
            {...form.register("minFare")}
          />
        </div>
        <TextField
          label="Mulai berlaku (kosongkan = sekarang)"
          type="datetime-local"
          error={errors.activeFrom?.message}
          {...form.register("activeFrom")}
        />
        <p className="self-end text-xs text-ink-muted md:pb-3">
          Tarif lama otomatis ditutup saat tarif baru berlaku. Tarif tidak bisa
          berlaku mundur — harga order lama tidak berubah.
        </p>
        <Button
          type="submit"
          disabled={create.isPending}
          className="md:col-span-2 md:justify-self-start"
        >
          {create.isPending ? "Menyimpan…" : "Simpan tarif"}
        </Button>
      </form>

      <div className="overflow-x-auto rounded-2xl bg-white p-4">
        <table className="w-full min-w-[760px] text-sm">
          <thead>
            <tr className="text-left text-xs uppercase tracking-wider text-ink-muted">
              {[
                "Layanan",
                "Dasar",
                "Per km",
                "Minimum",
                "Berlaku",
                "Status",
              ].map((h) => (
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
            {tariffs.map((t) => {
              const s = stateOf(t);
              return (
                <tr
                  key={t.id}
                  className={`border-t border-line ${s.text === "Riwayat" ? "text-ink-muted" : ""}`}
                >
                  <td className="px-2 py-3 font-bold">{label(t)}</td>
                  <td className="px-2 py-3">{formatRupiah(t.baseFare)}</td>
                  <td className="px-2 py-3">{formatRupiah(t.perKmFare)}</td>
                  <td className="px-2 py-3">{formatRupiah(t.minFare)}</td>
                  <td className="px-2 py-3">
                    {formatDateTime(t.activeFrom)} →{" "}
                    {t.activeTo ? formatDateTime(t.activeTo) : "sekarang"}
                  </td>
                  <td className="px-2 py-3">
                    <span
                      className={`rounded-full px-2.5 py-1 text-xs font-bold ${s.tone}`}
                    >
                      {s.text}
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
