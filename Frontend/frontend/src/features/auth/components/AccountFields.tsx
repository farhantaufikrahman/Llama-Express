import { useFormContext } from 'react-hook-form';
import { TextField } from '../../../components/TextField';
import type { CustomerRegisterValues } from '../schemas';

/** Dipakai form customer DAN kurir lewat FormProvider — tidak perlu oper register/errors lewat props */
export function AccountFields() {
  const { register, formState: { errors } } = useFormContext<CustomerRegisterValues>();
  return (
    <>
      <TextField label="Nama lengkap" autoComplete="name" error={errors.fullName?.message} {...register('fullName')} />
      <TextField label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
      <TextField label="Nomor HP" type="tel" inputMode="tel" autoComplete="tel" placeholder="081234567890"
                 error={errors.phone?.message} {...register('phone')} />
      <TextField label="Password" type="password" autoComplete="new-password" error={errors.password?.message} {...register('password')} />
      <TextField label="Ulangi password" type="password" autoComplete="new-password"
                 error={errors.confirmPassword?.message} {...register('confirmPassword')} />
    </>
  );
}