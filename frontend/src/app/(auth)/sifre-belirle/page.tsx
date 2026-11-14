"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Check, Circle, Loader2, TriangleAlert } from "lucide-react";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { FormField } from "@/components/common/FormField";
import { usePasswordToken, useSetPassword } from "@/features/core/api";
import type { PasswordTokenInfo } from "@/features/core/types";
import { ApiError } from "@/lib/api";
import { useApiForm } from "@/lib/form";
import { passwordSchema } from "@/lib/validation";
import { cn } from "@/lib/utils";

const schema = z
  .object({ newPassword: passwordSchema, confirmPassword: z.string() })
  .refine((v) => v.newPassword === v.confirmPassword, {
    path: ["confirmPassword"],
    message: "Şifreler aynı değil.",
  });

const INVALID_TOKEN_MESSAGE =
  'Bu bağlantı geçersiz veya süresi dolmuş. "Şifremi unuttum" sayfasından yeni bir bağlantı isteyin.';

function InvalidToken({ message }: { message: string }) {
  const router = useRouter();
  return (
    <div className="space-y-4 text-center">
      <TriangleAlert className="mx-auto size-12 text-warning" aria-hidden />
      <h1 className="text-base font-semibold">Bağlantı geçersiz veya süresi dolmuş</h1>
      <p className="text-[15px] text-muted-foreground">{message}</p>
      <Button className="h-11 w-full md:h-10" onClick={() => router.push("/sifremi-unuttum")}>
        Yeni bağlantı iste
      </Button>
    </div>
  );
}

function SetPasswordForm({ token, info }: { token: string; info: PasswordTokenInfo }) {
  const router = useRouter();
  const setPassword = useSetPassword();
  const form = useApiForm(schema, { defaultValues: { newPassword: "", confirmPassword: "" } });
  const { register, formState } = form;
  const value = form.watch("newPassword") ?? "";

  const rules = [
    { label: "En az 8 karakter", ok: value.length >= 8 },
    { label: "En az bir harf", ok: /\p{L}/u.test(value) },
    { label: "En az bir rakam", ok: /\d/.test(value) },
  ];

  const onSubmit = form.submit(async (values) => {
    await setPassword.mutateAsync({ token, newPassword: values.newPassword });
    toast.success("Şifreniz kaydedildi. Şimdi giriş yapabilirsiniz.");
    router.replace(`/giris?email=${encodeURIComponent(info.email)}`);
  });

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <h1 className="text-base font-semibold">
        {info.purpose === "SETUP"
          ? `Hoş geldiniz ${info.firstName}! Hesabınız için bir şifre belirleyin.`
          : "Yeni şifrenizi belirleyin."}
      </h1>

      <FormField label="Yeni şifre" htmlFor="newPassword" error={formState.errors.newPassword?.message}>
        <Input
          id="newPassword"
          type="password"
          autoComplete="new-password"
          autoFocus
          className="h-11 md:h-10"
          aria-invalid={!!formState.errors.newPassword}
          {...register("newPassword")}
        />
      </FormField>

      <FormField
        label="Yeni şifre (tekrar)"
        htmlFor="confirmPassword"
        error={formState.errors.confirmPassword?.message}
      >
        <Input
          id="confirmPassword"
          type="password"
          autoComplete="new-password"
          className="h-11 md:h-10"
          aria-invalid={!!formState.errors.confirmPassword}
          {...register("confirmPassword")}
        />
      </FormField>

      <ul className="space-y-1 text-[13px]">
        {rules.map((r) => (
          <li
            key={r.label}
            className={cn("flex items-center gap-2", r.ok ? "text-success" : "text-muted-foreground")}
          >
            {r.ok ? <Check className="size-4" aria-hidden /> : <Circle className="size-4" aria-hidden />}
            {r.label}
          </li>
        ))}
      </ul>

      <Button type="submit" className="h-11 w-full md:h-10" disabled={formState.isSubmitting}>
        {formState.isSubmitting && <Loader2 className="animate-spin" aria-hidden />}
        Şifremi Kaydet
      </Button>
    </form>
  );
}

export default function SetPasswordPage() {
  const token = useSearchParams().get("token");
  const query = usePasswordToken(token);

  if (!token) return <InvalidToken message={INVALID_TOKEN_MESSAGE} />;
  if (query.isPending) {
    return (
      <div className="space-y-4" aria-busy="true" aria-label="Yükleniyor">
        <Skeleton className="h-6 w-3/4" />
        <Skeleton className="h-11 w-full" />
        <Skeleton className="h-11 w-full" />
      </div>
    );
  }
  if (query.isError) {
    const message = query.error instanceof ApiError ? query.error.message : INVALID_TOKEN_MESSAGE;
    return <InvalidToken message={message} />;
  }
  return <SetPasswordForm token={token} info={query.data} />;
}
