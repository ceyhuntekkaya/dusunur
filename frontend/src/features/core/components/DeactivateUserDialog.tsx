"use client";

import { useState } from "react";
import { toast } from "sonner";
import { ErrorState } from "@/components/common/ErrorState";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { UserPicker } from "@/components/common/UserPicker";
import { Skeleton } from "@/components/ui/skeleton";
import { useDeactivateUser, useDeactivationCheck } from "@/features/core/admin-api";
import type { AdminUserRow, DirUser } from "@/features/core/types";
import { FormActions } from "@/components/common/FormActions";
import { useMe } from "@/lib/me";

type Props = { user: AdminUserRow | null; onOpenChange: (open: boolean) => void };

function Body({ user, onClose }: { user: AdminUserRow; onClose: () => void }) {
  const season = useMe().data?.season;
  const check = useDeactivationCheck(user.id);
  const deactivate = useDeactivateUser();
  const [successor, setSuccessor] = useState<DirUser[]>([]);
  const [pending, setPending] = useState(false);

  if (check.isPending) return <Skeleton className="h-24 w-full" />;
  if (check.isError) return <ErrorState error={check.error} onRetry={() => void check.refetch()} />;

  const { items, successorRequired } = check.data;

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    try {
      await deactivate.mutateAsync({ id: user.id, successorUserId: successor[0]?.id });
      toast.success("Personel pasifleştirildi.");
      onClose();
    } catch {
      // Hata global toast ile gösterilir; pencere açık kalır.
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <p className="text-sm">
        {user.fullName} pasifleştirilecek. Sisteme giriş yapamayacak ve açık oturumları kapatılacak. Tüm
        gruplardan çıkarılacak.
      </p>
      {items.length > 0 && (
        <div className="rounded-lg bg-warning-bg px-3 py-2.5 text-sm text-warning">
          <p className="font-medium">Devredilecek sorumluluklar:</p>
          <ul className="mt-1 list-disc pl-5">
            {items.map((i) => (
              <li key={i.label}>
                {i.count} {i.label}
              </li>
            ))}
          </ul>
        </div>
      )}
      {successorRequired && (
        <FormField label="Devralacak personel" htmlFor="successor" required>
          <UserPicker
            id="successor"
            multiple={false}
            value={successor}
            onChange={setSuccessor}
            lookup={{ seasonId: season?.id }}
            excludeIds={[user.id]}
          />
        </FormField>
      )}
      <FormActions
        submitLabel="Pasifleştir"
        destructive
        submitting={pending}
        disabled={successorRequired && successor.length === 0}
        onCancel={onClose}
      />
    </form>
  );
}

export function DeactivateUserDialog({ user, onOpenChange }: Props) {
  return (
    <ResponsiveDialog open={!!user} onOpenChange={onOpenChange} title="Personel pasifleştirilsin mi?">
      {user && <Body user={user} onClose={() => onOpenChange(false)} />}
    </ResponsiveDialog>
  );
}
