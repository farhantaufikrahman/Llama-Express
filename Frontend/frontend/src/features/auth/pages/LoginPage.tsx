import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useLocation, useNavigate } from "react-router";
import { Logo } from "../../../components/Logo";
import { Button } from "../../../components/Button";
import { TextField } from "../../../components/TextField";
import { isApiError } from "../../../lib/api/errors";
import { useAuth } from "../AuthContext";
import { homeFor } from "../roles";
import { Link } from "react-router";
import { Alert } from "../../../components/Alert";
import { flash } from "../../../lib/flash";

const schema = z.object({
  email: z
    .string()
    .trim()
    .min(1, "Email wajib diisi")
    .email("Format email tidak valid"),
  password: z.string().min(1, "Password wajib diisi"),
});
type FormValues = z.infer<typeof schema>;

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [serverError, setServerError] = useState<string | null>(null);
  const [notice] = useState(() => flash.consume());

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const onSubmit = handleSubmit(async ({ email, password }) => {
    setServerError(null);
    try {
      const user = await login(email, password);
      const from = (location.state as { from?: string } | null)?.from;
      await navigate(from ?? homeFor(user.role), { replace: true });
    } catch (e) {
      if (isApiError(e) && e.status === 401)
        setServerError("Email atau password salah");
      else if (isApiError(e))
        setServerError(e.message); // 403 ACCOUNT_SUSPENDED, 429 RATE_LIMITED, dst.
      else
        setServerError(
          "Tidak bisa terhubung ke server. Cek koneksi atau apakah backend berjalan.",
        );
    }
  });

  return (
    <main className="grid min-h-dvh place-items-center px-4 py-10">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex justify-center">
          <Logo size={44} />
        </div>
        <form
          onSubmit={onSubmit}
          noValidate
          className="flex flex-col gap-4 rounded-2xl bg-white p-6 shadow-sm"
        >
          <div>
            <h1 className="text-2xl font-extrabold tracking-tight">Masuk</h1>
            {notice && <Alert kind="success">{notice}</Alert>}
            <p className="mt-1 text-sm text-ink-muted">
              Customer, kurir, dan admin masuk dari sini.
            </p>
          </div>

          {serverError && (
            <div
              role="alert"
              className="rounded-xl bg-danger/10 px-3.5 py-3 text-sm font-medium text-danger"
            >
              {serverError}
            </div>
          )}

          <TextField
            label="Email"
            type="email"
            autoComplete="email"
            error={errors.email?.message}
            {...register("email")}
          />
          <TextField
            label="Password"
            type="password"
            autoComplete="current-password"
            error={errors.password?.message}
            {...register("password")}
          />

          <Button type="submit" disabled={isSubmitting} className="mt-2 w-full">
            {isSubmitting ? "Memproses…" : "Masuk"}
          </Button>
        </form>
        <p className="mt-5 text-center text-sm text-ink-muted">
          Belum punya akun?{" "}
          <Link
            to="/register"
            className="font-bold text-brand-blue hover:underline"
          >
            Daftar
          </Link>
        </p>
      </div>
    </main>
  );
}
