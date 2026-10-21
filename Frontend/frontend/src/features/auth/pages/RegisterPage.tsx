import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useNavigate } from "react-router";
import { Logo } from "../../../components/Logo";
import { Button } from "../../../components/Button";
import { TextField } from "../../../components/TextField";
import { Alert } from "../../../components/Alert";
import { applyServerErrors } from "../../../lib/forms";
import { useAuth } from "../AuthContext";
import { homeFor } from "../roles";
import { AccountFields } from "../components/AccountFields";
import {
  courierRegisterSchema,
  customerRegisterSchema,
  type CourierRegisterValues,
  type CustomerRegisterValues,
} from "../schemas";

type Mode = "customer" | "courier";

export default function RegisterPage({ mode }: { mode: Mode }) {
  return (
    <main className="grid min-h-dvh place-items-center px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-6 flex justify-center">
          <Logo size={44} />
        </div>
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <h1 className="text-2xl font-extrabold tracking-tight">Buat akun</h1>
          <nav
            className="mt-4 grid grid-cols-2 gap-1 rounded-xl bg-cream p-1 text-sm font-bold"
            aria-label="Jenis akun"
          >
            <ModeTab to="/register" active={mode === "customer"}>
              Customer
            </ModeTab>
            <ModeTab to="/register/courier" active={mode === "courier"}>
              Kurir
            </ModeTab>
          </nav>
          <div className="mt-5">
            {mode === "customer" ? <CustomerForm /> : <CourierForm />}
          </div>
        </div>
        <p className="mt-5 text-center text-sm text-ink-muted">
          Sudah punya akun?{" "}
          <Link
            to="/login"
            className="font-bold text-brand-blue hover:underline"
          >
            Masuk
          </Link>
        </p>
      </div>
    </main>
  );
}

function ModeTab({
  to,
  active,
  children,
}: {
  to: string;
  active: boolean;
  children: string;
}) {
  return (
    <Link
      to={to}
      replace
      aria-current={active ? "page" : undefined}
      className={`rounded-lg py-2 text-center ${active ? "bg-white text-navy-900 shadow-sm" : "text-ink-muted"}`}
    >
      {children}
    </Link>
  );
}

const ACCOUNT_FIELDS = ["fullName", "email", "phone", "password"] as const;

function CustomerForm() {
  const { register: registerAccount } = useAuth();
  const navigate = useNavigate();
  const [banner, setBanner] = useState<string | null>(null);
  const form = useForm<CustomerRegisterValues>({
    resolver: zodResolver(customerRegisterSchema),
  });

  const onSubmit = form.handleSubmit(
    async ({ confirmPassword: _ignored, ...payload }) => {
      setBanner(null);
      try {
        const user = await registerAccount({ kind: "customer", payload });
        await navigate(homeFor(user.role), { replace: true });
      } catch (e) {
        setBanner(applyServerErrors(e, form.setError, ACCOUNT_FIELDS));
      }
    },
  );

  return (
    <FormProvider {...form}>
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
        {banner && <Alert>{banner}</Alert>}
        <AccountFields />
        <Button
          type="submit"
          disabled={form.formState.isSubmitting}
          className="mt-1 w-full"
        >
          {form.formState.isSubmitting
            ? "Mendaftarkan…"
            : "Daftar sebagai customer"}
        </Button>
      </form>
    </FormProvider>
  );
}

function CourierForm() {
  const { register: registerAccount } = useAuth();
  const navigate = useNavigate();
  const [banner, setBanner] = useState<string | null>(null);
  const form = useForm<CourierRegisterValues>({
    resolver: zodResolver(courierRegisterSchema),
    defaultValues: { vehicleType: "MOTOR", licenseNo: "" },
  });
  const {
    register,
    formState: { errors, isSubmitting },
  } = form;

  const onSubmit = form.handleSubmit(
    async ({ confirmPassword: _ignored, licenseNo, ...rest }) => {
      setBanner(null);
      try {
        const user = await registerAccount({
          kind: "courier",
          payload: { ...rest, licenseNo: licenseNo || undefined }, // string kosong → tidak dikirim
        });
        await navigate(homeFor(user.role), { replace: true });
      } catch (e) {
        setBanner(
          applyServerErrors(e, form.setError, [
            ...ACCOUNT_FIELDS,
            "plateNumber",
            "vehicleType",
            "licenseNo",
          ]),
        );
      }
    },
  );

  return (
    <FormProvider {...form}>
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
        {banner && <Alert>{banner}</Alert>}
        <AccountFields />

        <fieldset className="flex flex-col gap-1.5">
          <legend className="mb-1.5 text-xs font-semibold text-ink-muted">
            Kendaraan
          </legend>
          <div className="grid grid-cols-2 gap-2">
            {(["MOTOR", "CAR"] as const).map((v) => (
              <label
                key={v}
                className="flex cursor-pointer items-center gap-2 rounded-xl border-[1.5px] border-line px-3.5 py-3
                                      has-[:checked]:border-navy-900 has-[:checked]:bg-cream"
              >
                <input
                  type="radio"
                  value={v}
                  {...register("vehicleType")}
                  className="accent-navy-900"
                />
                <span className="text-sm font-bold">
                  {v === "MOTOR" ? "Motor" : "Mobil"}
                </span>
              </label>
            ))}
          </div>
        </fieldset>

        <TextField
          label="Nomor plat"
          placeholder="D 1234 ABC"
          autoCapitalize="characters"
          error={errors.plateNumber?.message}
          {...register("plateNumber")}
        />
        <TextField
          label="Nomor SIM (opsional)"
          error={errors.licenseNo?.message}
          {...register("licenseNo")}
        />

        <Button type="submit" disabled={isSubmitting} className="mt-1 w-full">
          {isSubmitting ? "Mendaftarkan…" : "Daftar sebagai kurir"}
        </Button>
        <p className="text-xs text-ink-muted">
          Akun kurir perlu diverifikasi admin sebelum bisa menerima order.
        </p>
      </form>
    </FormProvider>
  );
}
