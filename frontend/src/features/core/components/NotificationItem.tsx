"use client";

import { useRouter } from "next/navigation";
import {
  Bell,
  CalendarDays,
  FolderOpen,
  MessageCircle,
  Newspaper,
  Printer,
  type LucideIcon,
} from "lucide-react";
import { DateText } from "@/components/common/DateText";
import { useMarkNotificationRead } from "@/features/core/api";
import type { NotificationDto } from "@/features/core/types";
import { cn } from "@/lib/utils";

const ICONS: [string, LucideIcon][] = [
  ["PRINT_", Printer],
  ["FEED_", Newspaper],
  ["CHAT_", MessageCircle],
  ["DMS_", FolderOpen],
  ["CALENDAR_", CalendarDays],
];

function NotificationIcon({ type }: { type: string }) {
  const Icon = ICONS.find(([prefix]) => type.startsWith(prefix))?.[1] ?? Bell;
  return <Icon className="size-4" aria-hidden />;
}

export function NotificationItem({
  notification,
  onNavigate,
}: {
  notification: NotificationDto;
  onNavigate?: () => void;
}) {
  const router = useRouter();
  const markRead = useMarkNotificationRead();

  function open() {
    if (!notification.read) markRead.mutate(notification.id);
    onNavigate?.();
    if (notification.link) router.push(notification.link);
  }

  return (
    <button
      type="button"
      onClick={open}
      className="flex min-h-14 w-full items-start gap-3 rounded-lg border bg-card p-3 text-left outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-accent text-accent-foreground">
        <NotificationIcon type={notification.type} />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex items-start gap-2">
          {!notification.read && (
            <span className="mt-1.5 size-2 shrink-0 rounded-full bg-primary" aria-label="Okunmadı" />
          )}
          <span className={cn("text-[15px]", notification.read ? "font-normal" : "font-semibold")}>
            {notification.title}
          </span>
        </span>
        {notification.body && (
          <span className="mt-0.5 line-clamp-2 block text-[13px] text-muted-foreground">
            {notification.body}
          </span>
        )}
        <DateText
          value={notification.createdAt}
          relative
          className="mt-1 block text-xs text-muted-foreground"
        />
      </span>
    </button>
  );
}
