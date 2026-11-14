"use client";

import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { FormField } from "@/components/common/FormField";
import { PasswordRules } from "@/components/common/PasswordRules";
import { useChangePassword } from "@/features/core/api";
import { useApiForm } from "@/lib/form";
import { passwordSchema } from "@/lib/validation";

const schema = z
  .object({
    currentPassword: z.string().min(1, "Mevcut şifrenizi girin."),
    newPassword: passwordSchema,
    confirmPassword: z.string(),
  })
  .refine((v) => v.newPassword === v.confirmPassword, {
    path: ["confirmPassword"],
    message: "Şifreler aynı değil.",
  });

export function ChangePasswordCard() {
  const change = useChangePassword();
  const form = useApiForm(schema, {
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
  });
  const { register, formState } = form;
  const value = form.watch("newPassword") ?? "";

  const onSubmit = form.submit(async (values) => {
    await change.mutateAsync({ currentPassword: values.currentPassword, newPassword: values.newPassword });
    toast.success("Şifreniz değiştirildi. Diğer cihazlardaki oturumlarınız kapatıldı.");
    form.reset();
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base font-semibold">Şifre değiştir</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} className="max-w-md space-y-4" noValidate>
          <FormField
            label="Mevcut şifre"
            htmlFor="currentPassword"
            required
            error={formState.errors.currentPassword?.message}
          >
            <Input
              id="currentPassword"
              type="password"
              autoComplete="current-password"
              className="h-11 md:h-10"
              aria-invalid={!!formState.errors.currentPassword}
              {...register("currentPassword")}
            />
          </FormField>
          <FormField label="Yeni şifre" htmlFor="newPassword" required error={formState.errors.newPassword?.message}>
            <Input
              id="newPassword"
              type="password"
              autoComplete="new-password"
              className="h-11 md:h-10"
              aria-invalid={!!formState.errors.newPassword}
              {...register("newPassword")}
            />
          </FormField>
          <FormField
            label="Yeni şifre (tekrar)"
            htmlFor="confirmPassword"
            required
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
          <PasswordRules value={value} />
          <Button type="submit" className="h-11 w-full md:h-10 md:w-auto" disabled={formState.isSubmitting}>
            {formState.isSubmitting && <Loader2 className="animate-spin" aria-hidden />}
            Şifreyi Değiştir
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
