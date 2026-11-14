"use client";

import { useState } from "react";
import { Smartphone, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { InstallInstructions } from "@/components/common/InstallInstructions";
import { isMobileDevice, isStandalone, readLocal, writeLocal } from "@/lib/pwa";

const KEY = "pwa-install-dismissed";

export function InstallCard() {
  const [dismissed, setDismissed] = useState(() => typeof window === "undefined" || readLocal(KEY) === "1");
  const [visible] = useState(() => typeof window !== "undefined" && isMobileDevice() && !isStandalone());

  if (dismissed || !visible) return null;

  return (
    <div className="relative mb-4 flex gap-3 rounded-xl border bg-card p-4">
      <Smartphone className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden />
      <div className="min-w-0 flex-1 space-y-2 pr-8">
        <h2 className="text-[15px] font-semibold">Uygulamayı ana ekrana ekleyin</h2>
        <InstallInstructions />
      </div>
      <Button
        variant="ghost"
        size="icon"
        className="absolute top-1 right-1 size-11"
        aria-label="Kapat"
        onClick={() => {
          writeLocal(KEY, "1");
          setDismissed(true);
        }}
      >
        <X className="size-4" aria-hidden />
      </Button>
    </div>
  );
}
