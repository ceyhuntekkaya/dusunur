"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import type { NotificationDto } from "@/features/core/types";
import { connectWs, disconnectWs, useWsEvent, useWsReconnect } from "@/lib/ws";

export function RealtimeBridge() {
  const router = useRouter();
  const queryClient = useQueryClient();

  useEffect(() => {
    connectWs();
    return disconnectWs;
  }, []);

  const refreshCritical = () => {
    void queryClient.invalidateQueries({ queryKey: ["notifications"] });
    void queryClient.invalidateQueries({ queryKey: ["me", "badges"] });
  };

  useWsEvent<NotificationDto>("NOTIFICATION", (n) => {
    refreshCritical();
    if (document.visibilityState === "visible") {
      const link = n.link;
      toast(n.title, {
        description: n.body,
        action: link ? { label: "Aç", onClick: () => router.push(link) } : undefined,
      });
    }
  });

  useWsEvent("BADGES", () => {
    void queryClient.invalidateQueries({ queryKey: ["me", "badges"] });
  });

  useWsReconnect(() => {
    refreshCritical();
    void queryClient.invalidateQueries({ queryKey: ["chat"] });
  });

  return null;
}
