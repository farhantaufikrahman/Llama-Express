import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { z } from "zod";
import { Button } from "../../../components/Button";
import { TextField } from "../../../components/TextField";
import { Alert } from "../../../components/Alert";
import { applyServerErrors } from "../../../lib/forms";
import { flash } from "../../../lib/flash";
import { useAuth } from "../../auth/AuthContext";
import { fullNameField, passwordField, phoneField } from "../../auth/schemas";
import { accountApi } from "../api";

export default function AccountPage() {
  return (
    <section className="flex max-w-xl flex-col gap-6">
      <h1 className="text-2xl font-extrabold tracking-tight">Akun saya</h1>
      <ProfileForm />
      <ChangePasswordForm />
    </section>
  );
}

const profileSchema = z.object({ fullName: fullNameField, phone: phoneField });
type ProfileValues = z.infer<typeof profileSchema>;

function ProfileForm() {
  const { user, updateUser } = useAuth();
  const [banner, setBanner] = useState<{
    kind: "success" | "error";
    text: string;
  } | null>(null);
  const form = useForm<ProfileValues>({
    resolver: zodResolver(profileSchema),
    values: { fullName: user?.fullName ?? "", phone: user?.phone ?? "" }, // ikut berubah kalau user berubah
  });

  const mutation = useMutation({
    mutationFn: accountApi.updateProfile,
    onSuccess: (updated) => {
      updateUser(updated); // nama di header langsung ikut berubah
      setBanner({ kind: "success", text: "Profil tersimpan" });
    },
    onError: (e) => {
      const msg = applyServerErrors(e, form.setError, ["fullName", "phone"]);
      if (msg) setBanner({ kind: "error", text: msg });
    },
  });

  const onSubmit = form.handleSubmit((values) => {
    setBanner(null);
    mutation.mutate(values);
  });

  return (
    <form
      onSubmit={onSubmit}
      noValidate
      className="flex flex-col gap-4 rounded-2xl bg-white p-5"
    >
      <h2 className="text-lg font-extrabold">Profil</h2>
      {banner && <Alert kind={banner.kind}>{banner.text}</Alert>}
      <TextField label="Email" value={user?.email ?? ""} disabled readOnly />
      <TextField
        label="Nama lengkap"
        error={form.formState.errors.fullName?.message}
        {...form.register("fullName")}
      />
      <TextField
        label="Nomor HP"
        type="tel"
        error={form.formState.errors.phone?.message}
        {...form.register("phone")}
      />
      <Button
        type="submit"
        disabled={mutation.isPending || !form.formState.isDirty}
        className="self-start"
      >
        {mutation.isPending ? "Menyimpan…" : "Simpan profil"}
      </Button>
    </form>
  );
}

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, "Wajib diisi"),
    newPassword: passwordField,
    confirmPassword: z.string(),
  })
  .refine((v) => v.newPassword === v.confirmPassword, {
    path: ["confirmPassword"],
    message: "Konfirmasi password tidak sama",
  })
  .refine((v) => v.newPassword !== v.currentPassword, {
    path: ["newPassword"],
    message: "Password baru harus berbeda",
  });
type PasswordValues = z.infer<typeof passwordSchema>;

function ChangePasswordForm() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [banner, setBanner] = useState<string | null>(null);
  const form = useForm<PasswordValues>({
    resolver: zodResolver(passwordSchema),
  });

  const mutation = useMutation({
    mutationFn: accountApi.changePassword,
    onSuccess: async () => {
      // Backend mencabut SEMUA refresh token (termasuk sesi ini) → jujur ke pengguna: minta login ulang
      flash.set(
        "Password berhasil diganti. Silakan masuk dengan password baru.",
      );
      await logout();
      await navigate("/login", { replace: true });
    },
    onError: (e) =>
      setBanner(
        applyServerErrors(e, form.setError, ["currentPassword", "newPassword"]),
      ),
  });

  const onSubmit = form.handleSubmit(({ currentPassword, newPassword }) => {
    setBanner(null);
    mutation.mutate({ currentPassword, newPassword });
  });

  return (
    <form
      onSubmit={onSubmit}
      noValidate
      className="flex flex-col gap-4 rounded-2xl bg-white p-5"
    >
      <div>
        <h2 className="text-lg font-extrabold">Ganti password</h2>
        <p className="text-sm text-ink-muted">
          Setelah diganti, kamu akan keluar dari semua perangkat.
        </p>
      </div>
      {banner && <Alert>{banner}</Alert>}
      <TextField
        label="Password saat ini"
        type="password"
        autoComplete="current-password"
        error={form.formState.errors.currentPassword?.message}
        {...form.register("currentPassword")}
      />
      <TextField
        label="Password baru"
        type="password"
        autoComplete="new-password"
        error={form.formState.errors.newPassword?.message}
        {...form.register("newPassword")}
      />
      <TextField
        label="Ulangi password baru"
        type="password"
        autoComplete="new-password"
        error={form.formState.errors.confirmPassword?.message}
        {...form.register("confirmPassword")}
      />
      <Button
        type="submit"
        variant="secondary"
        disabled={mutation.isPending}
        className="self-start"
      >
        {mutation.isPending ? "Memproses…" : "Ganti password"}
      </Button>
    </form>
  );
}
