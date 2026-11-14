"use client";

import { useState } from "react";
import { toast } from "sonner";
import { FormActions } from "@/components/common/FormActions";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { UserPicker } from "@/components/common/UserPicker";
import { useAddGroupMembers } from "@/features/core/admin-api";
import type { DirUser } from "@/features/core/types";
import { useMe } from "@/lib/me";

type Props = {
  groupId: number;
  existingIds: number[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

function Body({ groupId, existingIds, onClose }: Omit<Props, "open" | "onOpenChange"> & { onClose: () => void }) {
  const isMember = !!useMe().data?.permissions.member;
  const add = useAddGroupMembers(groupId);
  const [users, setUsers] = useState<DirUser[]>([]);
  const [pending, setPending] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    try {
      await add.mutateAsync(users.map((u) => u.id));
      toast.success(`${users.length} üye eklendi.`);
      onClose();
    } catch {
      // Hata global toast ile gösterilir.
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <UserPicker
        value={users}
        onChange={setUsers}
        excludeIds={existingIds}
        lookup={isMember ? undefined : {}}
      />
      <FormActions
        submitLabel="Üye Ekle"
        submitting={pending}
        disabled={users.length === 0}
        onCancel={onClose}
      />
    </form>
  );
}

export function GroupAddMembersDialog({ open, onOpenChange, ...rest }: Props) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title="Üye Ekle">
      <Body {...rest} onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
