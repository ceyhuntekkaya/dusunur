"use client";

import { toast } from "sonner";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { Input } from "@/components/ui/input";
import { useSaveBranch } from "@/features/core/admin-api";
import { branchFormSchema } from "@/features/core/schemas";
import type { BranchRow } from "@/features/core/types";
import { useApiForm } from "@/lib/form";

type Props = { open: boolean; onOpenChange: (open: boolean) => void; branch?: BranchRow };

function BranchForm({ branch, onClose }: { branch?: BranchRow; onClose: () => void }) {
  const save = useSaveBranch();
  const form = useApiForm(branchFormSchema, { defaultValues: { name: branch?.name ?? "" } });
  const { register, formState } = form;

  const onSubmit = form.submit(async (values) => {
    await save.mutateAsync({ id: branch?.id, name: values.name });
    toast.success(branch ? "Şube güncellendi." : "Şube eklendi.");
    onClose();
  });

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <FormField label="Şube adı" htmlFor="branch-name" required error={formState.errors.name?.message}>
        <Input
          id="branch-name"
          className="h-11 md:h-10"
          placeholder="Örn. Merkez Kampüs"
          autoFocus
          aria-invalid={!!formState.errors.name}
          {...register("name")}
        />
      </FormField>
      <FormActions submitLabel="Kaydet" submitting={formState.isSubmitting} onCancel={onClose} />
    </form>
  );
}

export function BranchFormDialog({ open, onOpenChange, branch }: Props) {
  return (
    <ResponsiveDialog
      open={open}
      onOpenChange={onOpenChange}
      title={branch ? "Şubeyi Yeniden Adlandır" : "Şube Ekle"}
    >
      <BranchForm branch={branch} onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
