"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { cn } from "@/lib/utils";
import { usePrintTransition } from "../api";
import type { PrintAction } from "../types";
import { RejectDialog } from "./RejectDialog";

export type ActionButtonKind = PrintAction | "copy";

type Props = {
  requestId: number;
  ownerName: string;
  kinds: ActionButtonKind[];
  compact?: boolean;
  className?: string;
};

const LABELS: Record<ActionButtonKind, { full: string; compact: string }> = {
  approve: { full: "Onayla", compact: "Onayla" },
  reject: { full: "Reddet", compact: "Reddet" },
  cancel: { full: "İptal Et", compact: "İptal Et" },
  start: { full: "Hazırlamaya Başla", compact: "Başla" },
  ready: { full: "Hazır", compact: "Hazır" },
  deliver: { full: "Teslim Edildi", compact: "Teslim Et" },
  copy: { full: "Kopyala ve Yeniden Gönder", compact: "Kopyala" },
};

export function PrintActionButtons({ requestId, ownerName, kinds, compact, className }: Props) {
  const transition = usePrintTransition();
  const [dialog, setDialog] = useState<"approve" | "reject" | "cancel" | "deliver" | null>(null);

  if (kinds.length === 0) return null;

  function click(kind: ActionButtonKind) {
    if (kind === "approve" || kind === "reject" || kind === "cancel" || kind === "deliver") setDialog(kind);
    else if (kind !== "copy") transition.mutate({ id: requestId, action: kind });
  }

  return (
    <div className={cn("flex flex-wrap gap-2", className)}>
      {kinds.map((kind) => {
        const label = LABELS[kind][compact ? "compact" : "full"];
        const base = "h-11 md:h-10";
        if (kind === "copy") {
          return (
            <Button
              key={kind}
              variant="outline"
              className={base}
              nativeButton={false}
              render={<Link href={`/fotokopi/yeni?kopya=${requestId}`} />}
            >
              {label}
            </Button>
          );
        }
        const secondaryReady = kind === "ready" && !!compact && kinds.includes("start");
        const primary = (kind === "approve" || kind === "start" || kind === "ready" || kind === "deliver") && !secondaryReady;
        return (
          <Button
            key={kind}
            variant={primary ? "default" : "outline"}
            disabled={transition.isPending}
            className={cn(
              base,
              kind === "approve" && "bg-success text-white hover:bg-success/90",
              kind === "reject" && "border-destructive text-destructive hover:bg-destructive/10",
            )}
            onClick={() => click(kind)}
          >
            {label}
          </Button>
        );
      })}

      {dialog === "approve" && (
        <ConfirmDialog
          open
          onOpenChange={(o) => !o && setDialog(null)}
          title="Talebi onayla"
          description="Talep onaylanacak ve fotokopi birimine iletilecek."
          confirmLabel="Onayla"
          onConfirm={() => transition.mutateAsync({ id: requestId, action: "approve" })}
        />
      )}
      {dialog === "cancel" && (
        <ConfirmDialog
          open
          onOpenChange={(o) => !o && setDialog(null)}
          title="Talebi iptal et"
          description="Talep iptal edilecek. Bu işlem geri alınamaz."
          confirmLabel="İptal Et"
          destructive
          onConfirm={() => transition.mutateAsync({ id: requestId, action: "cancel" })}
        />
      )}
      {dialog === "deliver" && (
        <ConfirmDialog
          open
          onOpenChange={(o) => !o && setDialog(null)}
          title="Teslim edildi"
          description={`${ownerName} kişisine teslim edildi olarak işaretlensin mi?`}
          confirmLabel="Teslim Edildi"
          onConfirm={() => transition.mutateAsync({ id: requestId, action: "deliver" })}
        />
      )}
      {dialog === "reject" && <RejectDialog open onOpenChange={(o) => !o && setDialog(null)} requestId={requestId} />}
    </div>
  );
}
