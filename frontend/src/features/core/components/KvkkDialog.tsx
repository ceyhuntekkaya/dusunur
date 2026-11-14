"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/common/ErrorState";
import { SimpleMarkdown } from "@/components/common/SimpleMarkdown";
import { useAcceptKvkk, useKvkk, useSignOut } from "@/features/core/api";

export function KvkkDialog() {
  const queryClient = useQueryClient();
  const kvkk = useKvkk(true);
  const accept = useAcceptKvkk();
  const { signOut, pending: signingOut } = useSignOut();
  const [checked, setChecked] = useState(false);

  function onAccept() {
    accept.mutate(undefined, {
      onSuccess: () => {
        toast.success("Aydınlatma metni onaylandı.");
        void queryClient.invalidateQueries({ queryKey: ["me"] });
      },
    });
  }

  return (
    <Dialog open onOpenChange={() => {}} disablePointerDismissal>
      <DialogContent
        showCloseButton={false}
        className="top-0 left-0 flex h-dvh max-w-none translate-x-0 translate-y-0 flex-col gap-4 rounded-none p-4 sm:max-w-none md:p-8"
      >
        <DialogHeader>
          <DialogTitle className="text-lg font-semibold">
            Kişisel Verilerin Korunması Hakkında Aydınlatma Metni
          </DialogTitle>
        </DialogHeader>

        <div className="min-h-0 flex-1 overflow-y-auto rounded-lg border bg-card p-4">
          {kvkk.isPending && (
            <div className="space-y-3" aria-busy="true" aria-label="Yükleniyor">
              <Skeleton className="h-4 w-1/2" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-5/6" />
            </div>
          )}
          {kvkk.isError && <ErrorState error={kvkk.error} onRetry={() => void kvkk.refetch()} />}
          {kvkk.data && <SimpleMarkdown source={kvkk.data.text} />}
        </div>

        <div className="space-y-3 pb-[env(safe-area-inset-bottom)]">
          <div className="flex min-h-11 items-center gap-3">
            <Checkbox id="kvkk-ok" checked={checked} onCheckedChange={(v) => setChecked(v === true)} />
            <Label htmlFor="kvkk-ok" className="text-[15px]">
              Okudum, anladım
            </Label>
          </div>
          <div className="flex flex-col-reverse gap-2 md:flex-row md:justify-end">
            <Button variant="outline" className="h-11 md:h-10" disabled={signingOut} onClick={signOut}>
              Çıkış Yap
            </Button>
            <Button className="h-11 md:h-10" disabled={!checked || accept.isPending} onClick={onAccept}>
              {accept.isPending && <Loader2 className="animate-spin" aria-hidden />}
              Onayla ve Devam Et
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
