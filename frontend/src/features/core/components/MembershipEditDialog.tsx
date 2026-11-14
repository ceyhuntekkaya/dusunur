"use client";

import { Controller } from "react-hook-form";
import { toast } from "sonner";
import { FormActions } from "@/components/common/FormActions";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { useUpdateMembership } from "@/features/core/admin-api";
import { MembershipFields, membershipErrors } from "@/features/core/components/MembershipFields";
import { formToMembership, membershipEditSchema, membershipToForm } from "@/features/core/schemas";
import type { MembershipRow } from "@/features/core/types";
import { useApiForm } from "@/lib/form";

type Props = { seasonId: number; row: MembershipRow | null; onOpenChange: (open: boolean) => void };

function Body({ seasonId, row, onClose }: { seasonId: number; row: MembershipRow; onClose: () => void }) {
  const update = useUpdateMembership(seasonId);
  const form = useApiForm(membershipEditSchema, { defaultValues: { membership: membershipToForm(row) } });
  const { control, formState } = form;

  const onSubmit = form.submit(async (v) => {
    await update.mutateAsync({ membershipId: row.id, body: formToMembership(v.membership) });
    toast.success("Yıl ataması güncellendi.");
    onClose();
  });

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <Controller
        control={control}
        name="membership"
        render={({ field }) => (
          <MembershipFields
            idPrefix="me"
            value={field.value}
            onChange={field.onChange}
            errors={membershipErrors(formState.errors)}
            seasonId={seasonId}
            excludeUserId={row.userId}
            currentBranch={{ id: row.branchId, name: row.branchName }}
          />
        )}
      />
      <FormActions submitLabel="Kaydet" submitting={formState.isSubmitting} onCancel={onClose} />
    </form>
  );
}

export function MembershipEditDialog({ seasonId, row, onOpenChange }: Props) {
  return (
    <ResponsiveDialog
      open={!!row}
      onOpenChange={onOpenChange}
      title={row ? `${row.fullName} – atama bilgileri` : "Atama bilgileri"}
    >
      {row && <Body seasonId={seasonId} row={row} onClose={() => onOpenChange(false)} />}
    </ResponsiveDialog>
  );
}
