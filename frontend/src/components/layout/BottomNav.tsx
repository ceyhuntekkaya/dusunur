"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { CountBadge } from "@/components/common/CountBadge";
import { badgeCount, isActive, visibleModuleItems } from "@/components/layout/nav-items";
import { useBadges } from "@/features/core/api";
import type { MeResponse } from "@/features/core/types";
import { cn } from "@/lib/utils";

export function BottomNav({ me }: { me: MeResponse }) {
  const pathname = usePathname();
  const badges = useBadges(true).data;
  const items = visibleModuleItems(me);
  if (items.length === 0) return null;

  return (
    <nav
      aria-label="Ana menü"
      className="fixed inset-x-0 bottom-0 z-30 flex h-[calc(64px+env(safe-area-inset-bottom))] border-t bg-card pb-[env(safe-area-inset-bottom)] md:hidden"
    >
      {items.map((item) => {
        const Icon = item.icon;
        const active = isActive(pathname, item.href);
        const count = badgeCount(badges, item.badgeKeys);
        return (
          <Link
            key={item.href}
            href={item.href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "relative flex flex-1 flex-col items-center justify-center gap-1 text-[11px] font-medium outline-none",
              active ? "text-primary" : "text-muted-foreground",
            )}
          >
            <span className="relative">
              <Icon className="size-5" strokeWidth={1.75} aria-hidden />
              <CountBadge count={count} className="absolute -top-2 -right-3 h-4 min-w-4 px-1 text-[10px]" />
            </span>
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}
