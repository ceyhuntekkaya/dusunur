"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { Eye, EyeOff, Loader2, TriangleAlert } from "lucide-react";
import { z } from "zod";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FormField } from "@/components/common/FormField";
import { useLogin } from "@/features/core/api";
import type { MeResponse } from "@/features/core/types";
import { ApiError } from "@/lib/api";
import { useApiForm } from "@/lib/form";
import { useMe } from "@/lib/me";

const schema = z.object({
  email: z.string().trim().min(1, "E-posta adresinizi girin."),
  password: z.string().min(1, "Şifrenizi girin."),
});

const REASONS: Record<string, string> = {
  UNAUTHENTICATED: "Oturumunuz sona erdi. Lütfen tekrar giriş yapın.",
  AUTH_ACCOUNT_PASSIVE: "Hesabınız pasif durumda. Okul yönetimiyle iletişime geçin.",
  AUTH_NO_ACTIVE_SEASON:
    "Şu anda aktif bir eğitim-öğretim yılı bulunmuyor. Okul yönetimiyle iletişime geçin.",
  AUTH_NO_SEASON_MEMBERSHIP:
    "Aktif eğitim-öğretim yılına atanmamışsınız. Okul yönetimiyle iletişime geçin.",
};

export default function LoginPage() {
  const router = useRouter();
  const params = useSearchParams();
  const queryClient = useQueryClient();
  const me = useMe();
  const login = useLogin();
  const [showPassword, setShowPassword] = useState(false);

  const reason = REASONS[params.get("neden") ?? ""];
  const next = params.get("next");

  function destination(user: MeResponse) {
    if (next && next.startsWith("/") && !next.startsWith("//")) return next;
    return user.permissions.member ? "/akis" : "/yonetim/personel";
  }

  useEffect(() => {
    if (me.data) router.replace(destination(me.data));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [me.data]);

  const form = useApiForm(schema, {
    defaultValues: { email: params.get("email") ?? "", password: "" },
  });
  const { register, formState } = form;

  const onSubmit = form.submit(async (values) => {
    const user = await login.mutateAsync({ email: values.email, password: values.password });
    queryClient.setQueryData(["me"], user);
    router.replace(destination(user));
  });

  const loginError = login.error instanceof ApiError ? login.error.message : undefined;

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      {reason && !loginError && (
        <Alert className="border-warning/30 bg-warning-bg text-warning">
          <TriangleAlert aria-hidden />
          <AlertDescription className="text-warning">{reason}</AlertDescription>
        </Alert>
      )}
      {loginError && (
        <Alert variant="destructive" className="border-destructive/30 bg-destructive/5">
          <AlertDescription>{loginError}</AlertDescription>
        </Alert>
      )}

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

      <FormField label="Şifre" htmlFor="password" error={formState.errors.password?.message}>
        <div className="relative">
          <Input
            id="password"
            type={showPassword ? "text" : "password"}
            autoComplete="current-password"
            className="h-11 pr-11 md:h-10 md:pr-11"
            aria-invalid={!!formState.errors.password}
            {...register("password")}
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            aria-label={showPassword ? "Şifreyi gizle" : "Şifreyi göster"}
            className="absolute top-0 right-0 flex size-11 items-center justify-center text-muted-foreground hover:text-foreground md:size-10"
          >
            {showPassword ? <EyeOff className="size-5" /> : <Eye className="size-5" />}
          </button>
        </div>
      </FormField>

      <Button type="submit" className="h-11 w-full md:h-10" disabled={formState.isSubmitting}>
        {formState.isSubmitting && <Loader2 className="animate-spin" aria-hidden />}
        Giriş Yap
      </Button>

      <div className="text-center">
        <Link href="/sifremi-unuttum" className="text-sm text-primary hover:underline">
          Şifremi unuttum
        </Link>
      </div>
    </form>
  );
}
