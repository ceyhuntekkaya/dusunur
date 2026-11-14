"use client";

import { useState } from "react";
import { UserRoundPlus } from "lucide-react";
import { Controller } from "react-hook-form";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { UserPicker } from "@/components/common/UserPicker";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useApiForm } from "@/lib/form";
import { useMe } from "@/lib/me";
import { useCreateDelegation, useDelegations, useDeleteDelegation } from "../api";
import { formatDateRange } from "../labels";
import { delegationSchema } from "../schemas";
import type { DelegationDto } from "../types";

function DelegateDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (o: boolean) => void }) {
  const me = useMe().data;
  const create = useCreateDelegation();
  const form = useApiForm(delegationSchema, { defaultValues: { delegate: [], startDate: "", endDate: "" } });
  const { control, register, formState } = form;

  const onSubmit = form.submit(async (v) => {
    const dto = await create.mutateAsync({ delegateId: v.delegate[0].id, startDate: v.startDate, endDate: v.endDate });
    toast.success(
      `Vekil tanımlandı. ${formatDateRange(dto.startDate, dto.endDate)} arasında gelen talepler ${dto.delegate.fullName} kişisine gidecek.`,
    );
    onOpenChange(false);
  });

  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title="Vekil tanımla">
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <FormField label="Vekil" htmlFor="delegate" required error={formState.errors.delegate?.message}>
          <Controller
            control={control}
            name="delegate"
            render={({ field }) => (
              <UserPicker
                id="delegate"
                multiple={false}
                value={field.value}
                onChange={field.onChange}
                excludeIds={me ? [me.id] : []}
                invalid={!!formState.errors.delegate}
              />
            )}
          />
        </FormField>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Başlangıç" htmlFor="delegation-start" required error={formState.errors.startDate?.message}>
            <Input
              id="delegation-start"
              type="date"
              className="h-11 md:h-10"
              aria-invalid={!!formState.errors.startDate}
              {...register("startDate")}
            />
          </FormField>
          <FormField label="Bitiş" htmlFor="delegation-end" required error={formState.errors.endDate?.message}>
            <Input
              id="delegation-end"
              type="date"
              className="h-11 md:h-10"
              aria-invalid={!!formState.errors.endDate}
              {...register("endDate")}
            />
          </FormField>
        </div>
        <FormActions submitLabel="Vekil Tanımla" submitting={formState.isSubmitting} onCancel={() => onOpenChange(false)} />
      </form>
    </ResponsiveDialog>
  );
}

export function DelegationCard() {
  const delegations = useDelegations();
  const remove = useDeleteDelegation();
  const [dialog, setDialog] = useState(false);
  const [toDelete, setToDelete] = useState<DelegationDto | null>(null);
  const list = delegations.data ?? [];

  return (
    <section className="mb-4 rounded-xl border bg-card p-4">
      {delegations.isPending ? (
        <Skeleton className="h-10 w-full" />
      ) : list.length === 0 ? (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-[15px] text-muted-foreground">
            İzinde olacağınız günler için bir vekil tanımlayabilirsiniz.
          </p>
          <Button className="h-11 md:h-10" onClick={() => setDialog(true)}>
            <UserRoundPlus aria-hidden />
            Vekil Tanımla
          </Button>
        </div>
      ) : (
        <div className="space-y-2">
          <ul className="space-y-1">
            {list.map((d) => (
              <li key={d.id} className="flex min-h-11 items-center justify-between gap-3">
                <span className="text-[15px]">
                  {d.delegate.fullName} · {formatDateRange(d.startDate, d.endDate)}
                </span>
                <Button variant="ghost" className="h-11 text-destructive md:h-10" onClick={() => setToDelete(d)}>
                  Sil
                </Button>
              </li>
            ))}
          </ul>
          <Button variant="outline" className="h-11 md:h-10" onClick={() => setDialog(true)}>
            <UserRoundPlus aria-hidden />
            Vekil Tanımla
          </Button>
        </div>
      )}

      {dialog && <DelegateDialog open onOpenChange={setDialog} />}
      {toDelete && (
        <ConfirmDialog
          open
          onOpenChange={(o) => !o && setToDelete(null)}
          title="Vekâleti sil"
          description={`${toDelete.delegate.fullName} kişisinin vekâleti silinecek. Bu aralıkta gelen talepler size gelir.`}
          confirmLabel="Sil"
          destructive
          onConfirm={() => remove.mutateAsync(toDelete.id)}
        />
      )}
    </section>
  );
}
