"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ShieldOff } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { isActive } from "@/components/layout/nav-items";
import { ADMIN_TABS } from "@/features/core/admin-tabs";
import { useMe } from "@/lib/me";
import { cn } from "@/lib/utils";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const me = useMe().data;
  const pathname = usePathname();
  if (!me) return null;

  const tabs = ADMIN_TABS.filter((t) => t.allowed(me.permissions));
  const current = ADMIN_TABS.find((t) => isActive(pathname, t.href));
  const forbidden = current !== undefined && !current.allowed(me.permissions);

  return (
    <div>
      <h1 className="mb-3 text-xl font-semibold md:text-[22px] md:leading-7">Yönetim</h1>
      <nav
        aria-label="Yönetim bölümleri"
        className="-mx-4 mb-4 overflow-x-auto border-b px-4 md:mx-0 md:mb-6 md:px-0"
      >
        <ul className="flex min-w-max gap-1">
          {tabs.map((t) => {
            const active = isActive(pathname, t.href);
            return (
              <li key={t.href}>
                <Link
                  href={t.href}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "flex h-11 items-center border-b-2 px-3 text-sm font-medium whitespace-nowrap outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
                    active
                      ? "border-primary text-primary"
                      : "border-transparent text-muted-foreground hover:text-foreground",
                  )}
                >
                  {t.label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>
      {forbidden ? (
        <EmptyState icon={ShieldOff} message="Bu sayfaya erişim yetkiniz yok." />
      ) : (
        children
      )}
    </div>
  );
}
