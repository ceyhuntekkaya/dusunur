"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useApiForm } from "@/lib/form";
import { useDeleteSpace, useUpdateSpace } from "../api";
import { spaceFormSchema } from "../schemas";
import type { SpaceRow } from "../types";

function Body({ space, onClose }: { space: SpaceRow; onClose: () => void }) {
  const router = useRouter();
  const update = useUpdateSpace(space.id);
  const del = useDeleteSpace(space.id);
  const [confirming, setConfirming] = useState(false);
  const form = useApiForm(spaceFormSchema, {
    defaultValues: { name: space.name, description: space.description ?? "" },
  });
  const { register, formState } = form;

  const onSubmit = form.submit(async (v) => {
    await update.mutateAsync({ name: v.name, description: v.description || undefined });
    toast.success("Alan güncellendi.");
    onClose();
  });

  return (
    <div className="space-y-6">
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <FormField label="Ad" htmlFor="set-name" required error={formState.errors.name?.message}>
          <Input id="set-name" className="h-11 md:h-10" aria-invalid={!!formState.errors.name} {...register("name")} />
        </FormField>
        <FormField label="Açıklama" htmlFor="set-desc" error={formState.errors.description?.message}>
          <Textarea id="set-desc" rows={3} aria-invalid={!!formState.errors.description} {...register("description")} />
        </FormField>
        <FormActions submitLabel="Kaydet" submitting={formState.isSubmitting} onCancel={onClose} />
      </form>

      <div className="space-y-3 rounded-xl border border-destructive/40 p-4">
        <h3 className="text-sm font-semibold text-destructive">Tehlikeli bölge</h3>
        <p className="text-[13px] text-muted-foreground">
          Alan yalnızca içinde klasör ve dosya yoksa silinebilir.
        </p>
        <Button variant="destructive" className="h-11 md:h-10" onClick={() => setConfirming(true)}>
          Alanı Sil
        </Button>
      </div>

      <ConfirmDialog
        open={confirming}
        onOpenChange={setConfirming}
        title="Alanı sil"
        description={`'${space.name}' alanı kalıcı olarak silinecek.`}
        confirmLabel="Alanı Sil"
        destructive
        onConfirm={async () => {
          await del.mutateAsync();
          toast.success("Alan silindi.");
          router.replace("/dokumanlar");
        }}
      />
    </div>
  );
}

type Props = { space: SpaceRow; open: boolean; onOpenChange: (o: boolean) => void };

export function SpaceSettingsDialog({ space, open, onOpenChange }: Props) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title="Alan Ayarları">
      <Body space={space} onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
