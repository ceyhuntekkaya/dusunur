"use client";

import { useState } from "react";
import { toast } from "sonner";
import { DetailList } from "@/components/common/DetailList";
import { ErrorState } from "@/components/common/ErrorState";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { SimpleSelect } from "@/components/common/SimpleSelect";
import { UserAvatar } from "@/components/common/UserAvatar";
import { Checkbox } from "@/components/ui/checkbox";
import { Skeleton } from "@/components/ui/skeleton";
import { useAllSeasonMemberships, useCopyMemberships } from "@/features/core/admin-api";
import type { SeasonRow } from "@/features/core/types";
import { ApiError } from "@/lib/api";

type Props = {
  target: SeasonRow;
  seasons: SeasonRow[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

function Body({ target, seasons, onClose }: { target: SeasonRow; seasons: SeasonRow[]; onClose: () => void }) {
  const sources = seasons.filter((s) => s.id !== target.id);
  const older = sources.filter((s) => s.name < target.name);
  const [sourceId, setSourceId] = useState<string>(String((older[0] ?? sources[0])?.id ?? ""));
  const [unchecked, setUnchecked] = useState<Set<number>>(new Set());
  const [details, setDetails] = useState<string[]>([]);
  const [pending, setPending] = useState(false);
  const rows = useAllSeasonMemberships(sourceId ? Number(sourceId) : undefined);
  const copy = useCopyMemberships(target.id);

  const all = rows.data ?? [];
  const active = all.filter((r) => r.userStatus === "ACTIVE");
  const passiveCount = all.length - active.length;
  const selectedIds = active.filter((r) => !unchecked.has(r.userId)).map((r) => r.userId);

  function changeSource(v: string) {
    setSourceId(v);
    setUnchecked(new Set());
    setDetails([]);
  }

  function toggle(userId: number, on: boolean) {
    const next = new Set(unchecked);
    if (on) next.delete(userId);
    else next.add(userId);
    setUnchecked(next);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    setDetails([]);
    try {
      const res = await copy.mutateAsync({ sourceSeasonId: Number(sourceId), userIds: selectedIds });
      let msg = `${res.created} kişi kopyalandı.`;
      if (res.approverCleared > 0) {
        msg += ` ${res.approverCleared} kişinin onaylayıcısı yeni yıla atanmadığı için boş bırakıldı.`;
      }
      toast.success(msg);
      onClose();
    } catch (err) {
      if (err instanceof ApiError) setDetails(err.details);
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4" noValidate>
      <FormField label="Kaynak yıl" htmlFor="copy-source" required>
        <SimpleSelect
          id="copy-source"
          value={sourceId}
          onChange={changeSource}
          options={sources.map((s) => ({ value: String(s.id), label: s.name }))}
          placeholder="Yıl seçin"
        />
      </FormField>

      {rows.isPending ? (
        <Skeleton className="h-40 w-full" />
      ) : rows.isError ? (
        <ErrorState error={rows.error} onRetry={() => void rows.refetch()} />
      ) : (
        <>
          <div className="max-h-72 overflow-y-auto rounded-lg border">
            {active.length === 0 ? (
              <p className="p-4 text-center text-sm text-muted-foreground">
                Bu yılda kopyalanabilecek personel yok.
              </p>
            ) : (
              <ul>
                <li className="flex min-h-11 items-center gap-3 border-b px-3">
                  <Checkbox
                    id="copy-all"
                    checked={selectedIds.length === active.length}
                    onCheckedChange={(v) => setUnchecked(v === true ? new Set() : new Set(active.map((r) => r.userId)))}
                  />
                  <label htmlFor="copy-all" className="flex-1 text-sm font-medium">
                    Tümünü seç / kaldır
                  </label>
                </li>
                {active.map((r) => (
                  <li key={r.userId} className="flex min-h-11 items-center gap-3 px-3">
                    <Checkbox
                      id={`copy-${r.userId}`}
                      checked={!unchecked.has(r.userId)}
                      onCheckedChange={(v) => toggle(r.userId, v === true)}
                    />
                    <label htmlFor={`copy-${r.userId}`} className="flex flex-1 items-center gap-3 py-1.5 text-sm">
                      <UserAvatar userId={r.userId} name={r.fullName} size="sm" />
                      <span className="min-w-0">
                        <span className="block truncate">{r.fullName}</span>
                        <span className="block truncate text-xs text-muted-foreground">
                          {[r.branchName, r.title].filter(Boolean).join(" · ")}
                        </span>
                      </span>
                    </label>
                  </li>
                ))}
              </ul>
            )}
          </div>
          {passiveCount > 0 && (
            <p className="text-[13px] text-muted-foreground">{passiveCount} pasif personel listelenmedi.</p>
          )}
        </>
      )}

      <DetailList items={details} />
      <FormActions
        submitLabel={`${selectedIds.length} Kişiyi Kopyala`}
        submitting={pending}
        disabled={selectedIds.length === 0}
        onCancel={onClose}
      />
    </form>
  );
}

export function CopyFromSeasonDialog({ target, seasons, open, onOpenChange }: Props) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title="Önceki Yıldan Kopyala">
      <Body target={target} seasons={seasons} onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
