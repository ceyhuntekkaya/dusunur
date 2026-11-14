"use client";

import { useState } from "react";
import { toast } from "sonner";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { SearchInput } from "@/components/common/SearchInput";
import { SimpleSelect } from "@/components/common/SimpleSelect";
import { UserAvatar } from "@/components/common/UserAvatar";
import { Checkbox } from "@/components/ui/checkbox";
import { Skeleton } from "@/components/ui/skeleton";
import { useActiveBranches, useBatchAssign, useUnassignedUsers } from "@/features/core/admin-api";

type Props = { seasonId: number; open: boolean; onOpenChange: (open: boolean) => void };

function Body({ seasonId, onClose }: { seasonId: number; onClose: () => void }) {
  const [q, setQ] = useState("");
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [branchId, setBranchId] = useState("");
  const [branchError, setBranchError] = useState<string>();
  const [pending, setPending] = useState(false);
  const users = useUnassignedUsers(seasonId, q);
  const branches = useActiveBranches();
  const assign = useBatchAssign(seasonId);

  const list = users.data ?? [];
  const allSelected = list.length > 0 && list.every((u) => selected.has(u.id));

  function toggle(id: number, on: boolean) {
    const next = new Set(selected);
    if (on) next.add(id);
    else next.delete(id);
    setSelected(next);
  }

  function toggleAll(on: boolean) {
    const next = new Set(selected);
    for (const u of list) {
      if (on) next.add(u.id);
      else next.delete(u.id);
    }
    setSelected(next);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!branchId) {
      setBranchError("Şube seçin.");
      return;
    }
    setPending(true);
    try {
      const res = await assign.mutateAsync({ userIds: [...selected], branchId: Number(branchId) });
      toast.success(`${res.created} personel atandı.`);
      onClose();
    } catch {
      // Hata global toast ile gösterilir.
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4" noValidate>
      <SearchInput label="Personel ara" placeholder="Ad veya soyad" value={q} onChange={setQ} />
      <div className="max-h-72 overflow-y-auto rounded-lg border">
        {users.isPending ? (
          <div className="space-y-2 p-3">
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-full" />
          </div>
        ) : list.length === 0 ? (
          <p className="p-4 text-center text-sm text-muted-foreground">Eşleşen personel bulunamadı.</p>
        ) : (
          <ul>
            <li className="flex min-h-11 items-center gap-3 border-b px-3">
              <Checkbox
                id="assign-all"
                checked={allSelected}
                onCheckedChange={(v) => toggleAll(v === true)}
                aria-label="Listedekilerin tümünü seç"
              />
              <label htmlFor="assign-all" className="flex-1 text-sm font-medium">
                Tümünü seç / kaldır
              </label>
            </li>
            {list.map((u) => (
              <li key={u.id} className="flex min-h-11 items-center gap-3 px-3">
                <Checkbox
                  id={`assign-${u.id}`}
                  checked={selected.has(u.id)}
                  onCheckedChange={(v) => toggle(u.id, v === true)}
                />
                <label htmlFor={`assign-${u.id}`} className="flex flex-1 items-center gap-3 py-1.5 text-sm">
                  <UserAvatar userId={u.id} name={u.fullName} size="sm" />
                  <span className="truncate">{u.fullName}</span>
                </label>
              </li>
            ))}
          </ul>
        )}
      </div>
      <FormField label="Şube" htmlFor="assign-branch" required error={branchError}>
        <SimpleSelect
          id="assign-branch"
          value={branchId}
          onChange={(v) => {
            setBranchId(v);
            setBranchError(undefined);
          }}
          options={(branches.data ?? []).map((b) => ({ value: String(b.id), label: b.name }))}
          placeholder="Şube seçin"
          invalid={!!branchError}
        />
      </FormField>
      <FormActions
        submitLabel={selected.size > 0 ? `Seçilenleri Ata (${selected.size})` : "Seçilenleri Ata"}
        submitting={pending}
        disabled={selected.size === 0}
        onCancel={onClose}
      />
    </form>
  );
}

export function AssignUnassignedDialog({ seasonId, open, onOpenChange }: Props) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title="Atanmamış Personel">
      <Body seasonId={seasonId} onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
