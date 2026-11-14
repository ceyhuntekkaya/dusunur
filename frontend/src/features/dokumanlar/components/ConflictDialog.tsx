"use client";

import { useState } from "react";
import { CheckboxField } from "@/components/common/CheckboxField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { Button } from "@/components/ui/button";
import type { ConflictRequest } from "../useUploadQueue";
import { splitExtension } from "../utils";

function Body({ request }: { request: ConflictRequest }) {
  const [all, setAll] = useState(false);
  const { base, ext } = splitExtension(request.name);
  const renamed = `${base} (2)${ext}`;
  return (
    <div className="space-y-4">
      <p className="text-[15px]">&apos;{request.name}&apos; adında bir dosya bu klasörde zaten var.</p>
      <div className="space-y-3">
        <div className="space-y-1">
          <Button
            variant="destructive"
            className="h-11 w-full md:h-10"
            onClick={() => request.resolve({ choice: "OVERWRITE", all })}
          >
            Üzerine Yaz
          </Button>
          <p className="text-[13px] text-muted-foreground">Eski dosya kalıcı olarak değiştirilir.</p>
        </div>
        <div className="space-y-1">
          <Button
            variant="outline"
            className="h-11 w-full md:h-10"
            onClick={() => request.resolve({ choice: "RENAME", all })}
          >
            İkisini de Sakla
          </Button>
          <p className="text-[13px] text-muted-foreground">Yeni dosya &quot;{renamed}&quot; olarak kaydedilir.</p>
        </div>
        <Button
          variant="outline"
          className="h-11 w-full md:h-10"
          onClick={() => request.resolve({ choice: "SKIP", all })}
        >
          Bu Dosyayı Atla
        </Button>
      </div>
      <CheckboxField
        id="conflict-all"
        checked={all}
        onCheckedChange={setAll}
        label="Kalan tüm çakışmalar için aynısını yap"
      />
    </div>
  );
}

export function ConflictDialog({ request }: { request: ConflictRequest | null }) {
  return (
    <ResponsiveDialog
      open={request !== null}
      onOpenChange={(o) => {
        if (!o) request?.resolve({ choice: "SKIP", all: false });
      }}
      title="Aynı adlı dosya var"
    >
      {request && <Body request={request} />}
    </ResponsiveDialog>
  );
}
