"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { BellRing, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { usePush } from "@/lib/push";
import { readLocal, writeLocal } from "@/lib/pwa";

const KEY = "push-prompt-dismissed-at";
const SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000;

function recentlyDismissed(): boolean {
  const at = Number(readLocal(KEY));
  return Number.isFinite(at) && at > 0 && Date.now() - at < SEVEN_DAYS;
}

export function PushPromptCard() {
  const push = usePush();
  const [dismissed, setDismissed] = useState(() => typeof window === "undefined" || recentlyDismissed());
  const enable = useMutation({ mutationFn: push.enable });

  const needsInstall = push.status === "needs-install";
  if (dismissed || (push.status !== "off" && !needsInstall)) return null;

  return (
    <div className="mb-4 flex gap-3 rounded-xl border bg-card p-4">
      <BellRing className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden />
      <div className="min-w-0 flex-1 space-y-3">
        <p className="text-[15px]">
          {needsInstall
            ? "Bildirim almak için önce uygulamayı ana ekrana ekleyin."
            : "Önemli duyuruları ve onay taleplerini kaçırmamak için bildirimleri açın."}
        </p>
        <div className="flex flex-wrap gap-2">
          {!needsInstall && (
            <Button className="h-11 md:h-10" disabled={enable.isPending} onClick={() => enable.mutate()}>
              {enable.isPending && <Loader2 className="animate-spin" aria-hidden />}
              Bildirimleri Aç
            </Button>
          )}
          <Button
            variant="outline"
            className="h-11 md:h-10"
            onClick={() => {
              writeLocal(KEY, String(Date.now()));
              setDismissed(true);
            }}
          >
            Daha sonra
          </Button>
        </div>
      </div>
    </div>
  );
}
