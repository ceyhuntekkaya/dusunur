"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowLeft, SearchX, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { DateText } from "@/components/common/DateText";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { ListSkeleton } from "@/components/common/Skeletons";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api";
import { cn } from "@/lib/utils";
import { useEmptyTrash, usePurgeItem, useRestoreItem, useSpace, useTrash } from "../api";
import type { TrashRow } from "../types";
import { FOLDER_ICON, iconFor, notifyRestored } from "../utils";

function daysLeft(purgeAt: string): number {
  return Math.max(0, Math.ceil((new Date(purgeAt).getTime() - Date.now()) / 86_400_000));
}

export function TrashView({ spaceId }: { spaceId: number }) {
  const space = useSpace(spaceId);
  const level = space.data?.myLevel;
  const canEdit = level === "EDIT" || level === "MANAGE";
  const canManage = level === "MANAGE";
  const trash = useTrash(spaceId, canEdit);
  const restore = useRestoreItem();
  const purge = usePurgeItem();
  const empty = useEmptyTrash(spaceId);
  const [purgeTarget, setPurgeTarget] = useState<TrashRow | null>(null);
  const [emptying, setEmptying] = useState(false);

  const back = `/dokumanlar/${spaceId}`;
  let body: React.ReactNode;
  if (space.isPending || (canEdit && trash.isPending)) {
    body = <ListSkeleton rows={4} />;
  } else if (space.isError) {
    body =
      space.error instanceof ApiError && space.error.status === 404 ? (
        <EmptyState
          icon={SearchX}
          message={space.error.message}
          action={{ label: "Dokümanlar'a dön", href: "/dokumanlar" }}
        />
      ) : (
        <ErrorState error={space.error} onRetry={() => void space.refetch()} />
      );
  } else if (!canEdit) {
    body = (
      <EmptyState
        icon={Trash2}
        message="Çöp kutusunu görmek için alanda en az “Düzenleme” yetkisi gerekir."
        action={{ label: "Alana dön", href: back }}
      />
    );
  } else if (trash.isError) {
    body = <ErrorState error={trash.error} onRetry={() => void trash.refetch()} />;
  } else if ((trash.data?.length ?? 0) === 0) {
    body = <EmptyState icon={Trash2} message="Çöp kutusu boş." />;
  } else {
    body = (
      <ul className="divide-y rounded-xl border bg-card">
        {(trash.data ?? []).map((r) => {
          const { Icon, className } = r.type === "FOLDER" ? FOLDER_ICON : iconFor(r.name);
          const days = daysLeft(r.purgeAt);
          const kind = r.type === "FOLDER" ? "folder" : "file";
          return (
            <li key={`${r.type}-${r.id}`} className="flex flex-col gap-3 p-3 md:flex-row md:items-center">
              <div className="flex min-w-0 flex-1 items-start gap-3">
                <Icon className={cn("mt-0.5 size-5 shrink-0", className)} aria-hidden />
                <div className="min-w-0 space-y-0.5">
                  <p className="truncate text-[15px]">{r.name}</p>
                  <p className="truncate text-xs text-muted-foreground">Eski konum: {r.originalPath}</p>
                  <p className="text-xs text-muted-foreground">
                    {r.deletedBy.fullName} sildi · <DateText value={r.deletedAt} />
                  </p>
                  <p className={cn("text-xs", days <= 3 ? "text-destructive" : "text-muted-foreground")}>
                    {days === 0 ? "Bugün kalıcı silinecek" : `${days} gün sonra kalıcı silinecek`}
                  </p>
                </div>
              </div>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  className="h-11 flex-1 md:h-10 md:flex-none"
                  disabled={restore.isPending}
                  onClick={() => void restore.mutateAsync({ kind, id: r.id }).then(notifyRestored, () => {})}
                >
                  Geri Yükle
                </Button>
                {canManage && (
                  <Button
                    variant="outline"
                    className="h-11 flex-1 text-destructive md:h-10 md:flex-none"
                    onClick={() => setPurgeTarget(r)}
                  >
                    Kalıcı Sil
                  </Button>
                )}
              </div>
            </li>
          );
        })}
      </ul>
    );
  }

  const count = trash.data?.length ?? 0;
  return (
    <div className="mx-auto max-w-[800px]">
      <Link
        href={back}
        className="mb-2 inline-flex h-11 items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground md:h-10"
      >
        <ArrowLeft className="size-4" aria-hidden />
        {space.data?.name ?? "Alan"}
      </Link>
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold md:text-[22px] md:leading-7">
            Çöp Kutusu{space.data ? ` — ${space.data.name}` : ""}
          </h1>
          <p className="mt-1 text-[13px] text-muted-foreground">Silinen öğeler 30 gün sonra kalıcı olarak silinir.</p>
        </div>
        {canManage && count > 0 && (
          <Button variant="destructive" className="h-11 md:h-10" onClick={() => setEmptying(true)}>
            Çöp Kutusunu Boşalt
          </Button>
        )}
      </div>
      {body}
      <ConfirmDialog
        open={purgeTarget !== null}
        onOpenChange={(o) => !o && setPurgeTarget(null)}
        title="Kalıcı olarak sil"
        description={`'${purgeTarget?.name ?? ""}' kalıcı olarak silinecek. Bu işlem geri alınamaz.`}
        confirmLabel="Kalıcı Sil"
        destructive
        onConfirm={async () => {
          if (!purgeTarget) return;
          await purge.mutateAsync({ kind: purgeTarget.type === "FOLDER" ? "folder" : "file", id: purgeTarget.id });
          toast.success("Kalıcı olarak silindi.");
        }}
      />
      <ConfirmDialog
        open={emptying}
        onOpenChange={setEmptying}
        title="Çöp kutusunu boşalt"
        description={`Çöp kutusundaki ${count} öğe kalıcı olarak silinecek. Bu işlem geri alınamaz.`}
        confirmLabel="Çöp Kutusunu Boşalt"
        destructive
        onConfirm={async () => {
          await empty.mutateAsync();
          toast.success("Çöp kutusu boşaltıldı.");
        }}
      />
    </div>
  );
}
