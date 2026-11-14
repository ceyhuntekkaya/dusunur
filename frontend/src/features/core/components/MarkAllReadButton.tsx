"use client";

import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { useMarkAllNotificationsRead } from "@/features/core/api";

export function MarkAllReadButton() {
  const markAll = useMarkAllNotificationsRead();
  return (
    <Button
      variant="link"
      className="h-11 px-0 md:h-8"
      disabled={markAll.isPending}
      onClick={() =>
        markAll.mutate(undefined, {
          onSuccess: () => toast.success("Tüm bildirimler okundu olarak işaretlendi."),
        })
      }
    >
      Tümünü okundu işaretle
    </Button>
  );
}
