"use client";

import { Share } from "lucide-react";
import { Button } from "@/components/ui/button";
import { isIos, useInstallPrompt } from "@/lib/pwa";

export function InstallInstructions() {
  const { canPrompt, prompt } = useInstallPrompt();

  if (isIos()) {
    return (
      <p className="text-[15px]">
        Alttaki Paylaş <Share className="inline size-4 align-text-bottom" aria-label="Paylaş simgesi" />{" "}
        simgesine dokunun → &apos;Ana Ekrana Ekle&apos;yi seçin → &apos;Ekle&apos;ye dokunun.
      </p>
    );
  }
  if (canPrompt) {
    return (
      <Button className="h-11 md:h-10" onClick={() => void prompt()}>
        Yükle
      </Button>
    );
  }
  return (
    <p className="text-[15px] text-muted-foreground">
      Tarayıcınızın menüsünden &apos;Uygulamayı yükle&apos; veya &apos;Ana ekrana ekle&apos; seçeneğini kullanın.
    </p>
  );
}
