import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  keepPreviousData,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { z } from "zod";
import { Button } from "../../../components/Button";
import { TextField } from "../../../components/TextField";
import { Alert } from "../../../components/Alert";
import { ChoiceGroup } from "../../../components/ChoiceGroup";
import { applyServerErrors } from "../../../lib/forms";
import { useDebouncedValue } from "../../../lib/hooks";
import { phoneField } from "../../auth/schemas";
import { PickMap, type LatLng, type MapMarker } from "../../map/PickMap";
import { reverseGeocode, round6 } from "../../map/geocoding";
import { orderApi, orderKeys } from "../api";
import { PointPanel } from "../components/PointPanel";
import { QuoteCard } from "../components/QuoteCard";
import type { Location, QuoteRequest } from "../types";

const location = z.object({
  address: z
    .string()
    .trim()
    .min(1, "Alamat wajib diisi")
    .max(255, "Maksimal 255 karakter"),
  lat: z.number(),
  lng: z.number(),
});

/** "1,5" atau "1.5" → 1.5; kosong → undefined (berat opsional) */
const parseWeight = (v: string) =>
  v.trim() === "" ? undefined : Number(v.replace(",", "."));

const schema = z.object({
  pickup: location
    .nullable()
    .refine((v) => v !== null, "Tentukan titik jemput"),
  dropoff: location
    .nullable()
    .refine((v) => v !== null, "Tentukan titik antar"),
  serviceType: z.enum(["INSTANT", "SAME_DAY"]),
  vehicleType: z.enum(["MOTOR", "CAR"]),
  weightKg: z.string().refine((v) => {
    const n = parseWeight(v);
    return n === undefined || (n >= 0.1 && n <= 9999);
  }, "Berat 0,1 – 9999 kg"),
  itemCategory: z.enum(["DOCUMENT", "FOOD", "GOODS"]),
  itemDesc: z.string().trim().max(255, "Maksimal 255 karakter"),
  recipientName: z
    .string()
    .trim()
    .min(1, "Nama penerima wajib diisi")
    .max(100, "Maksimal 100 karakter"),
  recipientPhone: phoneField,
  notes: z.string().trim().max(500, "Maksimal 500 karakter"),
  paymentMethod: z.enum(["CASH", "MIDTRANS"]),
});
type PointKind = "pickup" | "dropoff";
type OrderFormInput = z.input<typeof schema>; // nilai saat user mengisi (boleh null)
type OrderFormValues = z.output<typeof schema>; // nilai setelah lolos validasi (pasti terisi)

/** Titik dianggap ada HANYA kalau koordinatnya angka valid — pertahanan terakhir sebelum data masuk ke Leaflet */
const isPoint = (v: Location | null | undefined): v is Location =>
  !!v && Number.isFinite(v.lat) && Number.isFinite(v.lng);

export default function NewOrderPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [active, setActive] = useState<PointKind>("pickup");
  const [banner, setBanner] = useState<string | null>(null);

  const form = useForm<OrderFormInput, unknown, OrderFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      pickup: null,
      dropoff: null,
      serviceType: "INSTANT",
      vehicleType: "MOTOR",
      weightKg: "",
      itemCategory: "DOCUMENT",
      itemDesc: "",
      recipientName: "",
      recipientPhone: "",
      notes: "",
      paymentMethod: "CASH",
    },
  });
  const {
    register,
    setValue,
    getValues,
    control,
    formState: { errors, isSubmitting },
  } = form;
  // useWatch (bukan watch): berlangganan nilai lewat hook → aman untuk React Compiler (komponen tetap di-memoize)
  const [pickup, dropoff, serviceType, vehicleType, weightKg] = useWatch({
    control,
    name: ["pickup", "dropoff", "serviceType", "vehicleType", "weightKg"],
  });

  // ---------- Memilih titik (klik peta / hasil cari / alamat tersimpan) ----------
  const setPoint = async (kind: PointKind, pos: LatLng, address?: string) => {
    const point = { lat: round6(pos.lat), lng: round6(pos.lng) };
    setValue(
      kind,
      { address: address ?? "", ...point },
      { shouldValidate: true, shouldDirty: true },
    );

    // Alur alami: setelah jemput dipilih, langsung pindah ke titik antar
    if (kind === "pickup" && !isPoint(getValues("dropoff")))
      setActive("dropoff");

    if (!address) {
      const name = await reverseGeocode(point.lat, point.lng);
      const current = getValues(kind);
      // Abaikan hasil yang "terlambat" kalau titik sudah dipindah lagi atau alamat sudah diketik manual
      if (
        name &&
        current &&
        current.lat === point.lat &&
        current.lng === point.lng &&
        current.address === ""
      ) {
        setValue(`${kind}.address`, name, { shouldValidate: true });
      }
    }
  };

  // ---------- Harga real-time ----------
  const debouncedWeight = useDebouncedValue(weightKg, 400);
  const quoteRequest: QuoteRequest | null =
    isPoint(pickup) && isPoint(dropoff)
      ? {
          serviceType,
          vehicleType,
          // backend mewajibkan address terisi; selama reverse geocode berjalan, pakai teks sementara
          pickup: { ...pickup, address: pickup.address || "Titik jemput" },
          dropoff: { ...dropoff, address: dropoff.address || "Titik antar" },
          weightKg: parseWeight(debouncedWeight),
        }
      : null;

  const quote = useQuery({
    queryKey: orderKeys.quote(quoteRequest),
    queryFn: ({ signal }) => orderApi.quote(quoteRequest!, signal),
    enabled: quoteRequest !== null && !Number.isNaN(quoteRequest.weightKg),
    placeholderData: keepPreviousData, // harga lama tetap tampil (redup) selama harga baru dihitung
    staleTime: 60_000,
  });

  // ---------- Submit ----------
  const onSubmit = form.handleSubmit(async (v) => {
    setBanner(null);
    try {
      const order = await orderApi.create({
        serviceType: v.serviceType,
        vehicleType: v.vehicleType,
        itemCategory: v.itemCategory,
        pickup: v.pickup as Location,
        dropoff: v.dropoff as Location,
        weightKg: parseWeight(v.weightKg),
        itemDesc: v.itemDesc || undefined,
        notes: v.notes || undefined,
        recipientName: v.recipientName,
        recipientPhone: v.recipientPhone,
        paymentMethod: v.paymentMethod,
      });
      await queryClient.invalidateQueries({ queryKey: orderKeys.all });
      await navigate(`/orders/${order.code}`, {
        replace: true,
        state: { autoPay: order.paymentMethod === "MIDTRANS" }, // bayar online → popup langsung terbuka
      });
    } catch (e) {
      setBanner(
        applyServerErrors(e, form.setError, [
          "recipientName",
          "recipientPhone",
          "itemDesc",
          "notes",
          "weightKg",
        ]),
      );
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  });

  const markers: MapMarker[] = [
    ...(isPoint(pickup)
      ? [
          {
            id: "pickup",
            kind: "pickup" as const,
            position: pickup,
            title: "Titik jemput",
          },
        ]
      : []),
    ...(isPoint(dropoff)
      ? [
          {
            id: "dropoff",
            kind: "dropoff" as const,
            position: dropoff,
            title: "Titik antar",
          },
        ]
      : []),
  ];

  return (
    <form onSubmit={onSubmit} noValidate className="flex flex-col gap-5">
      <h1 className="text-2xl font-extrabold tracking-tight">Kirim paket</h1>
      {banner && <Alert>{banner}</Alert>}

      <div className="grid gap-4 lg:grid-cols-[1fr_380px]">
        <div className="flex flex-col gap-2">
          <PickMap
            markers={markers}
            showRoute
            className="h-80 lg:h-[520px]"
            onPick={(pos) => void setPoint(active, pos)}
          />
          <p className="text-xs text-ink-muted">
            Klik peta untuk menaruh titik{" "}
            <b>{active === "pickup" ? "jemput" : "antar"}</b>.
          </p>
        </div>

        <div className="flex flex-col gap-3">
          {(["pickup", "dropoff"] as const).map((kind) => {
            const point = kind === "pickup" ? pickup : dropoff;
            return (
              <PointPanel
                key={kind}
                kind={kind}
                value={isPoint(point) ? point : null}
                active={active === kind}
                error={errors[kind]?.message}
                addressError={errors[kind]?.address?.message}
                onAddressChange={(text) =>
                  setValue(`${kind}.address`, text, {
                    shouldValidate: true,
                    shouldDirty: true,
                  })
                }
                onActivate={() => setActive(kind)}
                onChoose={(place) => void setPoint(kind, place, place.address)}
              />
            );
          })}
          <QuoteCard query={quote} ready={quoteRequest !== null} />
        </div>
      </div>

      <section className="grid gap-4 rounded-2xl bg-white p-5 md:grid-cols-2">
        <ChoiceGroup
          legend="Layanan"
          registration={register("serviceType")}
          options={[
            { value: "INSTANT", title: "Instant", hint: "Langsung dijemput" },
            {
              value: "SAME_DAY",
              title: "Same Day",
              hint: "Sampai hari ini, lebih hemat",
            },
          ]}
        />
        <ChoiceGroup
          legend="Kendaraan"
          registration={register("vehicleType")}
          options={[
            { value: "MOTOR", title: "Motor", hint: "Maks. 20 kg" },
            { value: "CAR", title: "Mobil", hint: "Maks. 200 kg" },
          ]}
        />
        <ChoiceGroup
          legend="Jenis barang"
          columns={3}
          registration={register("itemCategory")}
          options={[
            { value: "DOCUMENT", title: "Dokumen" },
            { value: "FOOD", title: "Makanan" },
            { value: "GOODS", title: "Barang" },
          ]}
        />
        <div className="grid grid-cols-[1fr_120px] gap-3">
          <TextField
            label="Deskripsi barang (opsional)"
            error={errors.itemDesc?.message}
            {...register("itemDesc")}
          />
          <TextField
            label="Berat (kg)"
            inputMode="decimal"
            placeholder="1,5"
            error={errors.weightKg?.message}
            {...register("weightKg")}
          />
        </div>
      </section>

      <section className="grid gap-4 rounded-2xl bg-white p-5 md:grid-cols-2">
        <TextField
          label="Nama penerima"
          autoComplete="off"
          error={errors.recipientName?.message}
          {...register("recipientName")}
        />
        <TextField
          label="HP penerima"
          type="tel"
          placeholder="081234567890"
          error={errors.recipientPhone?.message}
          {...register("recipientPhone")}
        />
        <div className="md:col-span-2">
          <TextField
            label="Catatan untuk kurir (opsional)"
            placeholder="Titip di resepsionis"
            error={errors.notes?.message}
            {...register("notes")}
          />
        </div>
        <div className="md:col-span-2">
          <ChoiceGroup
            legend="Pembayaran"
            registration={register("paymentMethod")}
            options={[
              { value: "CASH", title: "Tunai", hint: "Bayar ke kurir" },
              {
                value: "MIDTRANS",
                title: "Bayar online",
                hint: "VA, e-wallet, kartu",
              },
            ]}
          />
        </div>
      </section>

      <div className="sticky bottom-0 -mx-4 border-t border-line bg-cream/95 px-4 py-3 backdrop-blur">
        <Button
          type="submit"
          disabled={isSubmitting || quote.isError}
          className="w-full md:w-auto"
        >
          {isSubmitting ? "Membuat order…" : "Buat order"}
        </Button>
      </div>
    </form>
  );
}
