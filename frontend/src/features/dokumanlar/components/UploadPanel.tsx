"use client";

import { Check, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import type { UploadItem } from "../useUploadQueue";

type Props = { items: UploadItem[]; onRetry: (key: number) => void; onDismiss: () => void };

export function UploadPanel({ items, onRetry, onDismiss }: Props) {
  if (items.length === 0) return null;
  const active = items.filter((i) => i.status === "waiting" || i.status === "uploading").length;
  return (
    <section
      aria-label="Yüklemeler"
      className="fixed inset-x-0 bottom-[calc(64px+env(safe-area-inset-bottom))] z-40 max-h-[50dvh] overflow-hidden border bg-card shadow-lg md:right-4 md:bottom-4 md:left-auto md:w-[360px] md:rounded-xl"
    >
      <header className="flex items-center justify-between gap-2 border-b px-4 py-2">
        <h2 className="text-sm font-semibold">
          {active > 0 ? `${active} dosya yükleniyor` : "Yükleme tamamlandı"}
        </h2>
        {active === 0 && (
          <Button variant="ghost" size="icon" className="size-11 md:size-9" aria-label="Paneli kapat" onClick={onDismiss}>
            <X className="size-4" aria-hidden />
          </Button>
        )}
      </header>
      <ul className="max-h-[calc(50dvh-3rem)] divide-y overflow-y-auto">
        {items.map((i) => (
          <li key={i.key} className="space-y-1 px-4 py-2.5">
            <div className="flex items-center gap-2">
              <span className="min-w-0 flex-1 truncate text-sm">{i.file.name}</span>
              {i.status === "done" && <Check className="size-4 text-green-600" aria-label="Yüklendi" />}
              {i.status === "error" && <X className="size-4 text-destructive" aria-label="Hata" />}
              {i.status === "skipped" && <span className="text-xs text-muted-foreground">Atlandı</span>}
              {i.status === "waiting" && <span className="text-xs text-muted-foreground">Bekliyor</span>}
            </div>
            {i.status === "uploading" && (
              <Progress value={i.progress} className="gap-0" aria-label={`${i.file.name} yükleniyor`} />
            )}
            {i.status === "error" && (
              <p className="text-[13px] text-destructive" role="alert">
                {i.error}{" "}
                <button type="button" className="min-h-6 underline" onClick={() => onRetry(i.key)}>
                  Tekrar dene
                </button>
              </p>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
