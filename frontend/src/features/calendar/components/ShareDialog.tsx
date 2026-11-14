"use client";

import { useState } from "react";
import { Loader2, X } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { SimpleSelect } from "@/components/common/SimpleSelect";
import { TargetPicker } from "@/components/common/TargetPicker";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Users } from "lucide-react";
import type { TargetView } from "@/features/core/types";
import { useAddShare, useDeleteShare, useShares, useUpdateShare } from "../api";
import type { CalendarDto, CalShareLevel } from "../types";

const LEVELS = [
  { value: "VIEW", label: "Görüntüleyebilir" },
  { value: "EDIT", label: "Düzenleyebilir" },
];

function Body({ calendar }: { calendar: CalendarDto }) {
  const shares = useShares(calendar.id);
  const add = useAddShare(calendar.id);
  const update = useUpdateShare(calendar.id);
  const remove = useDeleteShare(calendar.id);
  const [targets, setTargets] = useState<TargetView[]>([]);
  const [level, setLevel] = useState<string>("");
  const [levelError, setLevelError] = useState<string>();
  const [removing, setRemoving] = useState<number | null>(null);

  async function addAll() {
    if (!level) {
      setLevelError("Bir yetki seviyesi seçin.");
      return;
    }
    setLevelError(undefined);
    const pending = [...targets];
    try {
      for (const t of pending) {
        await add.mutateAsync({ subject: { type: t.type, id: t.id }, level: level as CalShareLevel });
        setTargets((prev) => prev.filter((p) => !(p.type === t.type && p.id === t.id)));
      }
      toast.success("Takvim paylaşıldı.");
    } catch {
      // Hata global toast ile gösterilir; kalan seçimler listede kalır.
    }
  }

  return (
    <div className="space-y-5">
      <div className="space-y-3">
        <TargetPicker value={targets} onChange={setTargets} allowed={["USER", "GROUP"]} groupScope="all" />
        <div className="space-y-1.5">
          <SimpleSelect
            value={level}
            onChange={(v) => {
              setLevel(v);
              setLevelError(undefined);
            }}
            options={LEVELS}
            placeholder="Yetki seviyesi seçin"
            invalid={!!levelError}
          />
          {levelError ? (
            <p className="text-[13px] text-destructive" role="alert">
              {levelError}
            </p>
          ) : (
            <p className="text-[13px] text-muted-foreground">
              Düzenleyebilir: kayıt ekleyebilir, tüm kayıtları değiştirebilir ve silebilir.
            </p>
          )}
        </div>
        <Button className="h-11 w-full md:h-10 md:w-auto" disabled={targets.length === 0 || add.isPending} onClick={() => void addAll()}>
          {add.isPending && <Loader2 className="animate-spin" aria-hidden />}
          Ekle
        </Button>
      </div>

      {shares.isPending ? (
        <Skeleton className="h-12 w-full" />
      ) : shares.isError ? (
        <ErrorState error={shares.error} onRetry={() => void shares.refetch()} />
      ) : shares.data.length === 0 ? (
        <EmptyState icon={Users} message="Bu takvimi henüz kimseyle paylaşmadınız." />
      ) : (
        <ul className="divide-y rounded-lg border bg-card">
          {shares.data.map((s) => (
            <li key={s.id} className="flex items-center gap-2 p-2">
              <span className="min-w-0 flex-1 truncate text-[15px]">{s.subject.name}</span>
              <SimpleSelect
                className="w-44"
                value={s.level}
                options={LEVELS}
                onChange={(v) =>
                  void update
                    .mutateAsync({ sid: s.id, level: v as CalShareLevel })
                    .then(() => toast.success("Paylaşım güncellendi."))
                    .catch(() => undefined)
                }
              />
              <Button
                variant="ghost"
                size="icon"
                className="size-11 md:size-10"
                aria-label={`${s.subject.name} paylaşımını kaldır`}
                onClick={() => setRemoving(s.id)}
              >
                <X className="size-4" aria-hidden />
              </Button>
            </li>
          ))}
        </ul>
      )}

      <ConfirmDialog
        open={removing !== null}
        onOpenChange={(o) => !o && setRemoving(null)}
        title="Paylaşımı kaldır"
        description="Bu kişi veya grup artık takvimi göremeyecek."
        confirmLabel="Kaldır"
        destructive
        onConfirm={async () => {
          await remove.mutateAsync(removing!);
          toast.success("Paylaşım kaldırıldı.");
        }}
      />
    </div>
  );
}

export function ShareDialog({
  calendar,
  open,
  onOpenChange,
}: {
  calendar: CalendarDto;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title={`'${calendar.name}' takvimini paylaş`}>
      <Body calendar={calendar} />
    </ResponsiveDialog>
  );
}
