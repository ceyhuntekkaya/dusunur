"use client";

import { useState } from "react";
import Link from "next/link";
import { Bell } from "lucide-react";
import { CountBadge } from "@/components/common/CountBadge";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { useBadges } from "@/features/core/api";
import { MarkAllReadButton } from "@/features/core/components/MarkAllReadButton";
import { NotificationList } from "@/features/core/components/NotificationList";
import { useIsDesktop } from "@/lib/media";

const BELL_CLASS =
  "relative flex size-11 items-center justify-center rounded-full outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50";

export function NotificationBell() {
  const desktop = useIsDesktop();
  const [open, setOpen] = useState(false);
  const unread = useBadges(true).data?.notifications ?? 0;

  const content = (
    <>
      <Bell className="size-5" strokeWidth={1.75} aria-hidden />
      <CountBadge count={unread} className="absolute top-0.5 right-0 h-4 min-w-4 px-1 text-[10px]" />
    </>
  );

  if (!desktop) {
    return (
      <Link href="/bildirimler" aria-label="Bildirimler" className={BELL_CLASS}>
        {content}
      </Link>
    );
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger aria-label="Bildirimler" className={BELL_CLASS}>
        {content}
      </PopoverTrigger>
      <PopoverContent align="end" className="w-[380px] max-w-[calc(100vw-2rem)] gap-0 p-0">
        <div className="flex items-center justify-between border-b px-4">
          <h2 className="text-[15px] font-semibold">Bildirimler</h2>
          <MarkAllReadButton />
        </div>
        <div className="max-h-[430px] overflow-y-auto p-3">
          <NotificationList onNavigate={() => setOpen(false)} />
        </div>
      </PopoverContent>
    </Popover>
  );
}
