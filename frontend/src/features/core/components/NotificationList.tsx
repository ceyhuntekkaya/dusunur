"use client";

import { Bell } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { InfiniteList } from "@/components/common/InfiniteList";
import { NotificationItem } from "@/features/core/components/NotificationItem";
import type { NotificationDto } from "@/features/core/types";

export function NotificationList({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <InfiniteList<NotificationDto>
      queryKey={["notifications", "list"]}
      path="/api/notifications"
      getKey={(n) => n.id}
      renderItem={(n) => <NotificationItem notification={n} onNavigate={onNavigate} />}
      empty={<EmptyState icon={Bell} message="Henüz bildiriminiz yok." />}
    />
  );
}
