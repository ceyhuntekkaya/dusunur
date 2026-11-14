"use client";

import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useApiForm } from "@/lib/form";
import { useCreateSpace } from "../api";
import { spaceFormSchema } from "../schemas";

function Form({ onClose }: { onClose: () => void }) {
  const router = useRouter();
  const create = useCreateSpace();
  const form = useApiForm(spaceFormSchema, { defaultValues: { name: "", description: "" } });
  const { register, formState } = form;

  const onSubmit = form.submit(async (v) => {
    const space = await create.mutateAsync({ name: v.name, description: v.description || undefined });
    toast.success("Alan oluşturuldu. Kimlerin erişeceğini belirlemek için 'Yetkiler'e tıklayın.");
    onClose();
    router.push(`/dokumanlar/${space.id}?yetkiler=1`);
  });

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <FormField label="Ad" htmlFor="space-name" required error={formState.errors.name?.message}>
        <Input
          id="space-name"
          className="h-11 md:h-10"
          autoFocus
          aria-invalid={!!formState.errors.name}
          {...register("name")}
        />
      </FormField>
      <FormField label="Açıklama" htmlFor="space-desc" error={formState.errors.description?.message}>
        <Textarea
          id="space-desc"
          rows={3}
          aria-invalid={!!formState.errors.description}
          {...register("description")}
        />
      </FormField>
      <FormActions submitLabel="Oluştur" submitting={formState.isSubmitting} onCancel={onClose} />
    </form>
  );
}

export function SpaceCreateDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (o: boolean) => void }) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title="Alan Oluştur">
      <Form onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
