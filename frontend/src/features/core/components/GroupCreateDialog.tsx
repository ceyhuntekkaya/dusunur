"use client";

import { Controller } from "react-hook-form";
import { toast } from "sonner";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { UserPicker } from "@/components/common/UserPicker";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useCreateGroup } from "@/features/core/admin-api";
import { groupCreateSchema } from "@/features/core/schemas";
import type { GroupDetail } from "@/features/core/types";
import { useApiForm } from "@/lib/form";
import { useMe } from "@/lib/me";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated: (group: GroupDetail) => void;
};

function Form({ onCreated, onClose }: { onCreated: Props["onCreated"]; onClose: () => void }) {
  const isMember = !!useMe().data?.permissions.member;
  const create = useCreateGroup();
  const form = useApiForm(groupCreateSchema, { defaultValues: { name: "", description: "", members: [] } });
  const { register, formState, control } = form;

  const onSubmit = form.submit(async (v) => {
    const group = await create.mutateAsync({
      name: v.name,
      description: v.description || undefined,
      memberIds: v.members.map((m) => m.id),
    });
    toast.success("Grup oluşturuldu. Grup sohbeti de açıldı.");
    onClose();
    onCreated(group);
  });

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <FormField label="Ad" htmlFor="g-name" required error={formState.errors.name?.message}>
        <Input
          id="g-name"
          className="h-11 md:h-10"
          placeholder="Örn. Matematik Zümresi"
          aria-invalid={!!formState.errors.name}
          {...register("name")}
        />
      </FormField>
      <FormField label="Açıklama" htmlFor="g-desc" error={formState.errors.description?.message}>
        <Textarea
          id="g-desc"
          rows={3}
          aria-invalid={!!formState.errors.description}
          {...register("description")}
        />
      </FormField>
      <FormField label="Üyeler" htmlFor="g-members" error={formState.errors.members?.message}>
        <Controller
          control={control}
          name="members"
          render={({ field }) => (
            <UserPicker
              id="g-members"
              value={field.value}
              onChange={field.onChange}
              lookup={isMember ? undefined : {}}
            />
          )}
        />
      </FormField>
      <FormActions submitLabel="Grup Oluştur" submitting={formState.isSubmitting} onCancel={onClose} />
    </form>
  );
}

export function GroupCreateDialog({ open, onOpenChange, onCreated }: Props) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title="Grup Oluştur">
      <Form onCreated={onCreated} onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
