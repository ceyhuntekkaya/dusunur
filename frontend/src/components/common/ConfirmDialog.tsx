"use client";

import { useState } from "react";
import { Loader2 } from "lucide-react";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import {
  Drawer,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import { useIsDesktop } from "@/lib/media";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  confirmLabel: string;
  destructive?: boolean;
  onConfirm: () => Promise<unknown> | unknown;
};

export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  destructive,
  onConfirm,
}: Props) {
  const desktop = useIsDesktop();
  const [pending, setPending] = useState(false);

  async function confirm() {
    setPending(true);
    try {
      await onConfirm();
      onOpenChange(false);
    } catch {
      // Hata mutasyon önbelleği tarafından toast ile gösterilir; pencere açık kalır.
    } finally {
      setPending(false);
    }
  }

  const buttons = (
    <>
      <Button
        variant="outline"
        className="h-11 md:h-10"
        disabled={pending}
        onClick={() => onOpenChange(false)}
      >
        Vazgeç
      </Button>
      <Button
        variant={destructive ? "destructive" : "default"}
        className="h-11 md:h-10"
        disabled={pending}
        onClick={confirm}
      >
        {pending && <Loader2 className="animate-spin" aria-hidden />}
        {confirmLabel}
      </Button>
    </>
  );

  if (desktop) {
    return (
      <AlertDialog open={open} onOpenChange={onOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{title}</AlertDialogTitle>
            <AlertDialogDescription>{description}</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>{buttons}</AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    );
  }

  return (
    <Drawer open={open} onOpenChange={onOpenChange}>
      <DrawerContent>
        <DrawerHeader>
          <DrawerTitle>{title}</DrawerTitle>
          <DrawerDescription>{description}</DrawerDescription>
        </DrawerHeader>
        <DrawerFooter className="pb-[calc(1rem+env(safe-area-inset-bottom))]">{buttons}</DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}
