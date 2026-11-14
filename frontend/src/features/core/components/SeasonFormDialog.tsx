"use client";

import { toast } from "sonner";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { Input } from "@/components/ui/input";
import { useSaveSeason } from "@/features/core/admin-api";
import { seasonFormSchema } from "@/features/core/schemas";
import type { SeasonRow } from "@/features/core/types";
import { useApiForm } from "@/lib/form";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  season?: SeasonRow;
  suggestedStartYear?: number;
  onCreated?: (season: SeasonRow) => void;
};

function SeasonForm({
  season,
  suggestedStartYear = new Date().getFullYear(),
  onCreated,
  onClose,
}: Omit<Props, "open" | "onOpenChange"> & { onClose: () => void }) {
  const save = useSaveSeason();
  const y = suggestedStartYear;
  const form = useApiForm(seasonFormSchema, {
    defaultValues: season
      ? { name: season.name, startDate: season.startDate, endDate: season.endDate }
      : { name: `${y}-${y + 1}`, startDate: `${y}-09-01`, endDate: `${y + 1}-08-31` },
  });
  const { register, formState } = form;

  const onSubmit = form.submit(async (values) => {
    const saved = await save.mutateAsync({ id: season?.id, body: values });
    if (season) {
      toast.success("Yıl bilgileri güncellendi.");
    } else {
      toast.success(`${saved.name} oluşturuldu. Şimdi personel atayabilirsiniz.`);
      onCreated?.(saved);
    }
    onClose();
  });

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <FormField label="Yıl adı" htmlFor="s-name" required error={formState.errors.name?.message}>
        <Input
          id="s-name"
          className="h-11 md:h-10"
          placeholder="2027-2028"
          aria-invalid={!!formState.errors.name}
          {...register("name")}
        />
      </FormField>
      <FormField label="Başlangıç" htmlFor="s-start" required error={formState.errors.startDate?.message}>
        <Input
          id="s-start"
          type="date"
          className="h-11 md:h-10"
          aria-invalid={!!formState.errors.startDate}
          {...register("startDate")}
        />
      </FormField>
      <FormField label="Bitiş" htmlFor="s-end" required error={formState.errors.endDate?.message}>
        <Input
          id="s-end"
          type="date"
          className="h-11 md:h-10"
          aria-invalid={!!formState.errors.endDate}
          {...register("endDate")}
        />
      </FormField>
      <FormActions submitLabel="Kaydet" submitting={formState.isSubmitting} onCancel={onClose} />
    </form>
  );
}

export function SeasonFormDialog({ open, onOpenChange, ...rest }: Props) {
  return (
    <ResponsiveDialog
      open={open}
      onOpenChange={onOpenChange}
      title={rest.season ? "Yılı Düzenle" : "Yeni Yıl Oluştur"}
    >
      <SeasonForm {...rest} onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
