"use client";

import Link from "next/link";
import { CircleCheck, Loader2 } from "lucide-react";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FormField } from "@/components/common/FormField";
import { useForgotPassword } from "@/features/core/api";
import { useApiForm } from "@/lib/form";
import { emailSchema } from "@/lib/validation";

const schema = z.object({ email: emailSchema });

export default function ForgotPasswordPage() {
  const forgot = useForgotPassword();
  const form = useApiForm(schema, { defaultValues: { email: "" } });
  const { register, formState } = form;

  const onSubmit = form.submit((values) => forgot.mutateAsync(values));

  if (forgot.data) {
    return (
      <div className="space-y-4 text-center">
        <CircleCheck className="mx-auto size-12 text-success" aria-hidden />
        <p className="text-[15px]">{forgot.data.message}</p>
        <Link href="/giris" className="inline-block text-sm text-primary hover:underline">
          Giriş sayfasına dön
        </Link>
      </div>
    );
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <h1 className="text-base font-semibold">Şifremi unuttum</h1>
      <FormField label="E-posta" htmlFor="email" error={formState.errors.email?.message}>
        <Input
          id="email"
          type="email"
          autoComplete="username"
          placeholder="ornek@okul.com"
          autoFocus
          className="h-11 md:h-10"
          aria-invalid={!!formState.errors.email}
          {...register("email")}
        />
      </FormField>
      <Button type="submit" className="h-11 w-full md:h-10" disabled={formState.isSubmitting}>
        {formState.isSubmitting && <Loader2 className="animate-spin" aria-hidden />}
        Bağlantı Gönder
      </Button>
      <div className="text-center">
        <Link href="/giris" className="text-sm text-primary hover:underline">
          Giriş sayfasına dön
        </Link>
      </div>
    </form>
  );
}
