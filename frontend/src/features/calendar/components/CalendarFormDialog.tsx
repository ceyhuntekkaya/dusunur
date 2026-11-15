"use client";

import { useEffect } from "react";
import { toast } from "sonner";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { SimpleSelect } from "@/components/common/SimpleSelect";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useActiveBranches } from "@/features/core/admin-api";
import { useApiForm } from "@/lib/form";
import { useCreateCalendar, useCreateResource, useUpdateCalendar } from "../api";
import { calendarFormSchema } from "../schemas";
import type { CalendarDto } from "../types";

const NO_BRANCH = "none";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  kind: "personal" | "resource";
  calendar?: CalendarDto;
};

function Form({ kind, calendar, onClose }: { kind: Props["kind"]; calendar?: CalendarDto; onClose: () => void }) {
  const resource = kind === "resource";
  const branches = useActiveBranches();
  const create = useCreateCalendar();
  const createResource = useCreateResource();
  const update = useUpdateCalendar();
  const initialBranch = branches.data?.find((b) => b.name === calendar?.branchName)?.id;
  const form = useApiForm(calendarFormSchema, {
    defaultValues: {
      name: calendar?.name ?? "",
      description: calendar?.description ?? "",
      branchId: initialBranch ? String(initialBranch) : NO_BRANCH,
    },
  });
  const { register, formState, watch, setValue } = form;
  const branchPending = resource && !!calendar?.branchName && branches.isPending;

  useEffect(() => {
    if (initialBranch && !form.getFieldState("branchId").isDirty) setValue("branchId", String(initialBranch));
  }, [initialBranch, form, setValue]);

  const onSubmit = form.submit(async (v) => {
    const body = {
      name: v.name,
      description: v.description || undefined,
      ...(resource && v.branchId !== NO_BRANCH ? { branchId: Number(v.branchId) } : {}),
    };
    if (calendar) await update.mutateAsync({ id: calendar.id, ...body });
    else if (resource) await createResource.mutateAsync(body);
    else await create.mutateAsync(body);
    toast.success(
      resource ? (calendar ? "Salon güncellendi." : "Salon eklendi.") : calendar ? "Takvim güncellendi." : "Takvim oluşturuldu.",
    );
    onClose();
  });

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <FormField label="Ad" htmlFor="cal-name" required error={formState.errors.name?.message}>
        <Input
          id="cal-name"
          className="h-11 md:h-10"
          placeholder={resource ? "Ör. Konferans Salonu" : undefined}
          autoFocus
          aria-invalid={!!formState.errors.name}
          {...register("name")}
        />
      </FormField>
      <FormField label="Açıklama" htmlFor="cal-desc" error={formState.errors.description?.message}>
        <Textarea
          id="cal-desc"
          rows={3}
          placeholder={resource ? "Kapasite, ekipman vb." : undefined}
          aria-invalid={!!formState.errors.description}
          {...register("description")}
        />
      </FormField>
      {resource && (
        <FormField label="Şube" htmlFor="cal-branch">
          <SimpleSelect
            id="cal-branch"
            value={watch("branchId")}
            onChange={(v) => setValue("branchId", v || NO_BRANCH)}
            options={[
              { value: NO_BRANCH, label: "Şube yok" },
              ...(branches.data ?? []).map((b) => ({ value: String(b.id), label: b.name })),
            ]}
          />
        </FormField>
      )}
      <FormActions submitLabel="Kaydet" submitting={formState.isSubmitting} disabled={branchPending} onCancel={onClose} />
    </form>
  );
}

export function CalendarFormDialog({ open, onOpenChange, kind, calendar }: Props) {
  const title =
    kind === "resource"
      ? calendar
        ? "Salonu Düzenle"
        : "Salon Ekle"
      : calendar
        ? "Takvimi Yeniden Adlandır"
        : "Takvim Oluştur";
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title={title}>
      <Form kind={kind} calendar={calendar} onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
