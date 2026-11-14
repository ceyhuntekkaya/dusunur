"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Separator } from "@/components/ui/separator";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { CountBadge } from "@/components/common/CountBadge";
import { UserMenu } from "@/components/layout/UserMenu";
import {
  adminItem,
  badgeCount,
  isActive,
  visibleModuleItems,
  type NavItem,
} from "@/components/layout/nav-items";
import { useBadges } from "@/features/core/api";
import type { MeResponse } from "@/features/core/types";
import { cn } from "@/lib/utils";

const institutionName = process.env.NEXT_PUBLIC_INSTITUTION_NAME ?? "Okul";

function SidebarLink({ item, count, active }: { item: NavItem; count: number; active: boolean }) {
  const Icon = item.icon;
  return (
    <Tooltip>
      <TooltipTrigger
        render={
          <Link
            href={item.href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "relative flex min-h-11 items-center justify-center gap-3 rounded-lg px-3 text-sm font-medium outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 lg:justify-start",
              active ? "bg-accent text-accent-foreground" : "text-foreground/80 hover:bg-muted",
            )}
          />
        }
      >
        <Icon className="size-5 shrink-0" strokeWidth={1.75} aria-hidden />
        <span className="hidden flex-1 lg:inline">{item.label}</span>
        <CountBadge count={count} className="absolute top-1 right-1 lg:static" />
      </TooltipTrigger>
      <TooltipContent side="right" className="lg:hidden">
        {item.label}
      </TooltipContent>
    </Tooltip>
  );
}

export function Sidebar({ me }: { me: MeResponse }) {
  const pathname = usePathname();
  const badges = useBadges(true).data;
  const modules = visibleModuleItems(me);
  const admin = adminItem(me);

  return (
    <aside className="fixed inset-y-0 left-0 z-20 hidden w-[72px] flex-col border-r bg-card md:flex lg:w-[248px]">
      <div className="flex h-16 items-center justify-center gap-3 px-3 lg:justify-start lg:px-5">
        <img src="/logo.svg" alt="" className="size-8 shrink-0" />
        <span className="hidden truncate text-[15px] font-semibold lg:inline">{institutionName}</span>
      </div>
      <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-2" aria-label="Ana menü">
        {modules.map((item) => (
          <SidebarLink
            key={item.href}
            item={item}
            count={badgeCount(badges, item.badgeKeys)}
            active={isActive(pathname, item.href)}
          />
        ))}
        {admin && (
          <>
            {modules.length > 0 && <Separator className="my-2" />}
            <SidebarLink item={admin} count={0} active={isActive(pathname, admin.href)} />
          </>
        )}
      </nav>
      <div className="border-t p-2">
        <UserMenu me={me} variant="card" side="top" />
      </div>
    </aside>
  );
}
