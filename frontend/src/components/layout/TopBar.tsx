"use client";

import { usePathname } from "next/navigation";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { pageTitle } from "@/components/layout/nav-items";
import { UserMenu } from "@/components/layout/UserMenu";
import { NotificationBell } from "@/features/core/components/NotificationBell";
import type { MeResponse } from "@/features/core/types";

export function TopBar({ me }: { me: MeResponse }) {
  const pathname = usePathname();

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

      <NotificationBell />

      <div className="md:hidden">
        <UserMenu me={me} variant="avatar" />
      </div>
    </header>
  );
}
