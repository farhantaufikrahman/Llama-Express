import { z } from "zod";

const rupiahField = z
  .string()
  .trim()
  .regex(/^\d{1,10}$/, "Angka bulat, tanpa titik/koma");

export const tariffSchema = z
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

export type TariffFormValues = z.infer<typeof tariffSchema>;

/** Nilai <input type="datetime-local"> (tanpa zona waktu) dibaca sebagai waktu lokal → ISO UTC */
export function localInputToIso(value: string): string | undefined {
  return value ? new Date(value).toISOString() : undefined;
}
