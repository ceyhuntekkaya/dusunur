"use client";

import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Switch } from "@/components/ui/switch";
import { usePush, type PushStatus } from "@/lib/push";

const DESCRIPTIONS: Partial<Record<PushStatus, string>> = {
  on: "Bu cihazda bildirimler açık.",
  denied:
    "Bildirim izni tarayıcı ayarlarından kapatılmış. Açmak için tarayıcı/telefon ayarlarından bu siteye bildirim izni verin.",
  "needs-install": "Bildirim almak için önce uygulamayı ana ekrana ekleyin.",
  unsupported: "Bu tarayıcı bildirimleri desteklemiyor.",
};

export function PushSettingsCard() {
  const push = usePush();
  const toggle = useMutation({
    mutationFn: async (on: boolean) => {
      if (on) await push.enable();
      else await push.disable();
      return on;
    },
    onSuccess: (on) => toast.success(on ? "Bu cihazda bildirimler açıldı." : "Bu cihazda bildirimler kapatıldı."),
  });

  const usable = push.status === "on" || push.status === "off";

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base font-semibold">Bildirimler</CardTitle>
      </CardHeader>
      <CardContent>
        {push.status === "loading" ? (
          <Skeleton className="h-11 w-full" />
        ) : (
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0">
              <label htmlFor="push-switch" className="block text-[15px] font-medium">
                Bu cihazda bildirim al
              </label>
              {DESCRIPTIONS[push.status] && (
                <p className="mt-1 text-[13px] text-muted-foreground">{DESCRIPTIONS[push.status]}</p>
              )}
            </div>
            <Switch
              id="push-switch"
              checked={push.status === "on"}
              disabled={!usable || toggle.isPending}
              onCheckedChange={(v) => toggle.mutate(v)}
            />
          </div>
        )}
      </CardContent>
    </Card>
  );
}
