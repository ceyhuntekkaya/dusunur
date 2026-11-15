"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { CountBadge } from "@/components/common/CountBadge";
import {
  adminItem,
  badgeCount,
  isActive,
  pageTitle,
  visibleModuleItems,
  type NavItem,
} from "@/components/layout/nav-items";
import { UserMenu } from "@/components/layout/UserMenu";
import { NotificationBell } from "@/features/core/components/NotificationBell";
import { useBadges } from "@/features/core/api";
import type { MeResponse } from "@/features/core/types";
import { cn } from "@/lib/utils";

const institutionName = process.env.NEXT_PUBLIC_INSTITUTION_NAME ?? "Okul";

function NavLink({ item, count, active }: { item: NavItem; count: number; active: boolean }) {
  const Icon = item.icon;
  return (
    <Link
      href={item.href}
      aria-current={active ? "page" : undefined}
      className={cn(
        "relative flex h-full w-[72px] flex-col items-center justify-center gap-0.5 border-b-2 text-[12px] outline-none focus-visible:bg-muted",
        active
          ? "border-foreground text-foreground"
          : "border-transparent text-muted-foreground hover:text-foreground",
      )}
    >
      <span className="relative">
        <Icon className="size-6" strokeWidth={active ? 2.25 : 1.75} aria-hidden />
        <CountBadge count={count} className="absolute -top-1.5 -right-3 h-4 min-w-4 px-1 text-[10px]" />
      </span>
      <span className="leading-4">{item.label}</span>
    </Link>
  );
}

export function TopBar({ me }: { me: MeResponse }) {
  const pathname = usePathname();
  const badges = useBadges(true).data;
  const modules = visibleModuleItems(me);
  const admin = adminItem(me);

  return (
    <header className="sticky top-0 z-30 border-b bg-card">
      <div className="mx-auto flex h-14 max-w-[1128px] items-center gap-2 px-4 md:h-[52px] md:gap-4 md:px-6">
        <div className="md:hidden">
          <UserMenu me={me} variant="avatar" />
        </div>
        <h2 className="min-w-0 flex-1 truncate text-lg font-semibold md:hidden">{pageTitle(pathname)}</h2>

        <Link href="/akis" className="hidden shrink-0 items-center gap-2 md:flex" aria-label={institutionName}>
          <img src="/logo.svg" alt="" className="size-8" />
          <span className="hidden max-w-[180px] truncate text-[15px] font-semibold xl:inline">{institutionName}</span>
        </Link>

        {me.season && (
          <span
            className="hidden h-7 items-center rounded-full bg-accent px-3 text-[13px] font-medium text-accent-foreground lg:inline-flex"
            title="Aktif eğitim-öğretim yılı"
          >
            {me.season.name}
          </span>
        )}

        <nav aria-label="Ana menü" className="ml-auto hidden h-full items-stretch md:flex">
          {modules.map((item) => (
            <NavLink
              key={item.href}
              item={item}
              count={badgeCount(badges, item.badgeKeys)}
              active={isActive(pathname, item.href)}
            />
          ))}
          {admin && <NavLink item={admin} count={0} active={isActive(pathname, admin.href)} />}
        </nav>

        <div className="flex h-full items-stretch md:border-l md:pl-2">
          <NotificationBell />
          <div className="hidden md:flex">
            <UserMenu me={me} variant="nav" />
          </div>
        </div>
      </div>
    </header>
  );
}
