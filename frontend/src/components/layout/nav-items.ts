import {
  CalendarDays,
  FolderOpen,
  MessageCircle,
  Newspaper,
  Printer,
  Settings,
  type LucideIcon,
} from "lucide-react";
import type { BadgeMap, MeResponse } from "@/features/core/types";

export type NavItem = {
  href: string;
  label: string;
  icon: LucideIcon;
  badgeKeys?: string[];
};

export const MODULE_NAV_ITEMS: NavItem[] = [
  { href: "/akis", label: "Akış", icon: Newspaper },
  { href: "/mesajlar", label: "Mesajlar", icon: MessageCircle, badgeKeys: ["chat"] },
  { href: "/fotokopi", label: "Fotokopi", icon: Printer, badgeKeys: ["printApprovals", "printQueue"] },
  { href: "/dokumanlar", label: "Dokümanlar", icon: FolderOpen },
  { href: "/takvim", label: "Takvim", icon: CalendarDays },
];

export const ADMIN_NAV_ITEM: NavItem = { href: "/yonetim", label: "Yönetim", icon: Settings };

export function visibleModuleItems(me: MeResponse): NavItem[] {
  return me.permissions.member ? MODULE_NAV_ITEMS : [];
}

export function adminItem(me: MeResponse): NavItem | null {
  return me.permissions.canAccessAdmin || me.permissions.canManageGroups ? ADMIN_NAV_ITEM : null;
}

export function badgeCount(badges: BadgeMap | undefined, keys?: string[]): number {
  if (!badges || !keys) return 0;
  return keys.reduce((sum, k) => sum + (badges[k] ?? 0), 0);
}

export function isActive(pathname: string, href: string): boolean {
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function pageTitle(pathname: string): string {
  if (pathname.startsWith("/bildirimler")) return "Bildirimler";
  if (pathname.startsWith("/profil")) return "Profilim";
  const item = [...MODULE_NAV_ITEMS, ADMIN_NAV_ITEM].find((i) => isActive(pathname, i.href));
  return item?.label ?? "";
}
