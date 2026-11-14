"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Bell } from "lucide-react";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { CountBadge } from "@/components/common/CountBadge";
import { pageTitle } from "@/components/layout/nav-items";
import { UserMenu } from "@/components/layout/UserMenu";
import { useBadges } from "@/features/core/api";
import type { MeResponse } from "@/features/core/types";

export function TopBar({ me }: { me: MeResponse }) {
  const pathname = usePathname();
  const unread = useBadges(true).data?.notifications ?? 0;

  return (
    <header className="sticky top-0 z-20 flex h-14 items-center gap-2 border-b bg-card px-4 md:h-16 md:justify-end md:bg-background md:px-6">
      <h2 className="min-w-0 flex-1 truncate text-lg font-semibold md:hidden">{pageTitle(pathname)}</h2>

      {me.season && (
        <Tooltip>
          <TooltipTrigger
            render={
              <span className="hidden h-8 items-center rounded-full bg-accent px-3 text-[13px] font-medium text-accent-foreground md:inline-flex" />
            }
          >
            {me.season.name}
          </TooltipTrigger>
          <TooltipContent>Aktif eğitim-öğretim yılı</TooltipContent>
        </Tooltip>
      )}

      <Link
        href="/bildirimler"
        aria-label="Bildirimler"
        className="relative flex size-11 items-center justify-center rounded-full outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        <Bell className="size-5" strokeWidth={1.75} aria-hidden />
        <CountBadge count={unread} className="absolute top-0.5 right-0 h-4 min-w-4 px-1 text-[10px]" />
      </Link>

      <div className="md:hidden">
        <UserMenu me={me} variant="avatar" />
      </div>
    </header>
  );
}
