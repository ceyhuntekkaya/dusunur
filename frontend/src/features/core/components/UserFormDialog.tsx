"use client";

import { Controller } from "react-hook-form";
import { toast } from "sonner";
import { CheckboxField } from "@/components/common/CheckboxField";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { Input } from "@/components/ui/input";
import { useCreateUser, useUpdateUser } from "@/features/core/admin-api";
import { MembershipFields, membershipErrors } from "@/features/core/components/MembershipFields";
import { formToMembership, membershipToForm, userFormSchema } from "@/features/core/schemas";
import type { AdminUserRow } from "@/features/core/types";
import { useApiForm } from "@/lib/form";
import { useMe } from "@/lib/me";

type Props = { open: boolean; onOpenChange: (open: boolean) => void; user?: AdminUserRow };

function displayPhone(phone?: string): string {
  if (!phone) return "";
  return phone.startsWith("+90") ? `0${phone.slice(3)}` : phone;
}

function UserForm({ user, onClose }: { user?: AdminUserRow; onClose: () => void }) {
  const season = useMe().data?.season;
  const create = useCreateUser();
  const update = useUpdateUser();
  const hasMembership = !!user?.membership;

  const form = useApiForm(userFormSchema, {
    defaultValues: {
      firstName: user?.firstName ?? "",
      lastName: user?.lastName ?? "",
      email: user?.email ?? "",
      phone: displayPhone(user?.phone),
      discipline: user?.discipline ?? "",
      systemAdmin: user?.systemAdmin ?? false,
      assign: user ? hasMembership : true,
      membership: membershipToForm(user?.membership),
    },
  });
  const { register, formState, control, watch } = form;
  const assign = watch("assign");

  const onSubmit = form.submit(async (v) => {
    const body = {
      firstName: v.firstName,
      lastName: v.lastName,
      email: v.email,
      phone: v.phone || undefined,
      discipline: v.discipline || undefined,
      systemAdmin: v.systemAdmin,
      membership: season && v.assign ? formToMembership(v.membership) : undefined,
    };
    if (user) {
      await update.mutateAsync({ id: user.id, body });
      toast.success("Personel bilgileri güncellendi.");
    } else {
      const res = await create.mutateAsync(body);
      if (res.mailSent) {
        toast.success(`Personel eklendi. Şifre belirleme bağlantısı ${v.email} adresine gönderildi.`);
      } else {
        toast.warning(
          "Personel eklendi ancak e-posta gönderilemedi. Daha sonra 'Şifre bağlantısı gönder' ile tekrar deneyin.",
        );
      }
    }
    onClose();
  });

  const err = formState.errors;

  return (
    <form onSubmit={onSubmit} className="space-y-6" noValidate>
      <section className="space-y-4">
        <h3 className="text-sm font-semibold">Kişisel bilgiler</h3>
        <FormField label="Ad" htmlFor="u-first" required error={err.firstName?.message}>
          <Input
            id="u-first"
            className="h-11 md:h-10"
            autoComplete="off"
            aria-invalid={!!err.firstName}
            {...register("firstName")}
          />
        </FormField>
        <FormField label="Soyad" htmlFor="u-last" required error={err.lastName?.message}>
          <Input
            id="u-last"
            className="h-11 md:h-10"
            autoComplete="off"
            aria-invalid={!!err.lastName}
            {...register("lastName")}
          />
        </FormField>
        <FormField label="E-posta" htmlFor="u-email" required error={err.email?.message}>
          <Input
            id="u-email"
            type="email"
            inputMode="email"
            className="h-11 md:h-10"
            placeholder="ad.soyad@okul.k12.tr"
            autoComplete="off"
            aria-invalid={!!err.email}
            {...register("email")}
          />
        </FormField>
        <FormField label="Telefon" htmlFor="u-phone" error={err.phone?.message}>
          <Input
            id="u-phone"
            type="tel"
            inputMode="tel"
            className="h-11 md:h-10"
            placeholder="05XX XXX XX XX"
            autoComplete="off"
            aria-invalid={!!err.phone}
            {...register("phone")}
          />
        </FormField>
        <FormField label="Branş" htmlFor="u-discipline" error={err.discipline?.message}>
          <Input
            id="u-discipline"
            className="h-11 md:h-10"
            placeholder="Örn. Matematik"
            aria-invalid={!!err.discipline}
            {...register("discipline")}
          />
        </FormField>
        <Controller
          control={control}
          name="systemAdmin"
          render={({ field }) => (
            <CheckboxField
              id="u-admin"
              checked={field.value}
              onCheckedChange={field.onChange}
              label="Sistem yöneticisi"
              hint="Yönetim ekranlarının tamamına erişir."
            />
          )}
        />
      </section>

      <section className="space-y-4">
        {season ? (
          <>
            <h3 className="text-sm font-semibold">{season.name} bilgileri</h3>
            {(!user || !hasMembership) && (
              <Controller
                control={control}
                name="assign"
                render={({ field }) => (
                  <CheckboxField
                    id="u-assign"
                    checked={field.value}
                    onCheckedChange={field.onChange}
                    label="Bu yıla şimdi ata"
                  />
                )}
              />
            )}
            {assign && (
              <Controller
                control={control}
                name="membership"
                render={({ field }) => (
                  <MembershipFields
                    idPrefix="u-m"
                    value={field.value}
                    onChange={field.onChange}
                    errors={membershipErrors(err)}
                    seasonId={season.id}
                    excludeUserId={user?.id}
                    currentBranch={
                      user?.membership
                        ? { id: user.membership.branchId, name: user.membership.branchName }
                        : undefined
                    }
                  />
                )}
              />
            )}
          </>
        ) : (
          <div className="rounded-lg bg-info-bg px-3 py-2.5 text-sm text-primary">
            Aktif eğitim-öğretim yılı yok; atamayı Yıllar ekranından yapabilirsiniz.
          </div>
        )}
      </section>

      <FormActions submitLabel="Kaydet" submitting={formState.isSubmitting} onCancel={onClose} />
    </form>
  );
}

export function UserFormDialog({ open, onOpenChange, user }: Props) {
  return (
    <ResponsiveDialog
      open={open}
      onOpenChange={onOpenChange}
      variant="sheet"
      title={user ? "Personeli Düzenle" : "Personel Ekle"}
    >
      <UserForm user={user} onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
