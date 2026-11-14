"use client";

import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { Textarea } from "@/components/ui/textarea";
import { useApiForm } from "@/lib/form";
import { usePrintTransition } from "../api";
import { rejectSchema } from "../schemas";

type Props = { open: boolean; onOpenChange: (open: boolean) => void; requestId: number; onDone?: () => void };

export function RejectDialog({ open, onOpenChange, requestId, onDone }: Props) {
  const transition = usePrintTransition();
  const form = useApiForm(rejectSchema, { defaultValues: { reason: "" } });
  const { register, formState } = form;

  const onSubmit = form.submit(async (v) => {
    await transition.mutateAsync({ id: requestId, action: "reject", reason: v.reason });
    onOpenChange(false);
    onDone?.();
  });

  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title="Talebi reddet">
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <FormField
          label="Red gerekçesi"
          htmlFor="reject-reason"
          required
          hint="Talep sahibi bu gerekçeyi görecek."
          error={formState.errors.reason?.message}
        >
          <Textarea
            id="reject-reason"
            rows={4}
            maxLength={500}
            aria-invalid={!!formState.errors.reason}
            {...register("reason")}
          />
        </FormField>
        <FormActions
          submitLabel="Reddet"
          destructive
          submitting={formState.isSubmitting}
          onCancel={() => onOpenChange(false)}
        />
      </form>
    </ResponsiveDialog>
  );
}
