"use client";

import { useState } from "react";
import { toast } from "sonner";
import { DetailList } from "@/components/common/DetailList";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { SimpleSelect } from "@/components/common/SimpleSelect";
import { UserPicker } from "@/components/common/UserPicker";
import { useActiveBranches, useBulkUpdate } from "@/features/core/admin-api";
import type { DirUser } from "@/features/core/types";
import { ApiError } from "@/lib/api";

type BulkProps = {
  seasonId: number;
  ids: number[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDone: () => void;
};

function updatedToast(res: { updated: number; skippedSelf: number }) {
  toast.success(
    `${res.updated} kayıt güncellendi.${res.skippedSelf > 0 ? " Onaylayıcının kendi kaydı atlandı." : ""}`,
  );
}

function BranchBody({ seasonId, ids, onClose, onDone }: Omit<BulkProps, "open" | "onOpenChange"> & { onClose: () => void }) {
  const branches = useActiveBranches();
  const update = useBulkUpdate(seasonId);
  const [branchId, setBranchId] = useState("");
  const [error, setError] = useState<string>();
  const [pending, setPending] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!branchId) {
      setError("Şube seçin.");
      return;
    }
    setPending(true);
    try {
      updatedToast(await update.mutateAsync({ membershipIds: ids, branchId: Number(branchId) }));
      onDone();
      onClose();
    } catch {
      // Hata global toast ile gösterilir.
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4" noValidate>
      <FormField label="Yeni şube" htmlFor="bulk-branch" required error={error}>
        <SimpleSelect
          id="bulk-branch"
          value={branchId}
          onChange={(v) => {
            setBranchId(v);
            setError(undefined);
          }}
          options={(branches.data ?? []).map((b) => ({ value: String(b.id), label: b.name }))}
          placeholder="Şube seçin"
          invalid={!!error}
        />
      </FormField>
      <FormActions submitLabel="Şubeyi Değiştir" submitting={pending} onCancel={onClose} />
    </form>
  );
}

export function BulkBranchDialog({ open, onOpenChange, ...rest }: BulkProps) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title={`${rest.ids.length} kişinin şubesini değiştir`}>
      <BranchBody {...rest} onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}

function ApproverBody({ seasonId, ids, onClose, onDone }: Omit<BulkProps, "open" | "onOpenChange"> & { onClose: () => void }) {
  const update = useBulkUpdate(seasonId);
  const [approver, setApprover] = useState<DirUser[]>([]);
  const [pending, setPending] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    try {
      updatedToast(await update.mutateAsync({ membershipIds: ids, printApproverId: approver[0]?.id }));
      onDone();
      onClose();
    } catch {
      // Hata global toast ile gösterilir.
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <FormField
        label="Fotokopi onaylayıcısı"
        htmlFor="bulk-approver"
        required
        hint="Değişiklik yeni taleplere uygulanır."
      >
        <UserPicker
          id="bulk-approver"
          multiple={false}
          value={approver}
          onChange={setApprover}
          lookup={{ seasonId }}
        />
      </FormField>
      <FormActions
        submitLabel="Onaylayıcı Ata"
        submitting={pending}
        disabled={approver.length === 0}
        onCancel={onClose}
      />
    </form>
  );
}

export function BulkApproverDialog({ open, onOpenChange, ...rest }: BulkProps) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title={`${rest.ids.length} kişiye onaylayıcı ata`}>
      <ApproverBody {...rest} onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}

type RemoveProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  description: string;
  onRemove: () => Promise<unknown>;
};

function RemoveBody({ description, onRemove, onClose }: Omit<RemoveProps, "open" | "onOpenChange"> & { onClose: () => void }) {
  const [details, setDetails] = useState<string[]>([]);
  const [pending, setPending] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    setDetails([]);
    try {
      await onRemove();
      onClose();
    } catch (err) {
      if (err instanceof ApiError) setDetails(err.details);
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <p className="text-sm">{description}</p>
      <DetailList items={details} />
      <FormActions submitLabel="Yıldan Çıkar" destructive submitting={pending} onCancel={onClose} />
    </form>
  );
}

export function RemoveMembershipsDialog({ open, onOpenChange, ...rest }: RemoveProps) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title="Yıldan çıkarılsın mı?">
      <RemoveBody {...rest} onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
