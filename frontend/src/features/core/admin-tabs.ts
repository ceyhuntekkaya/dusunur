import type { MeResponse } from "@/features/core/types";

export type AdminTab = { href: string; label: string; allowed: (p: MeResponse["permissions"]) => boolean };

export const ADMIN_TABS: AdminTab[] = [
  { href: "/yonetim/personel", label: "Personel", allowed: (p) => p.canAccessAdmin },
  { href: "/yonetim/yillar", label: "Eğitim-Öğretim Yılları", allowed: (p) => p.canAccessAdmin },
  { href: "/yonetim/subeler", label: "Şubeler", allowed: (p) => p.canAccessAdmin },
  { href: "/yonetim/gruplar", label: "Gruplar", allowed: (p) => p.canManageGroups },
  { href: "/yonetim/denetim", label: "Denetim Kaydı", allowed: (p) => p.canAccessAdmin },
];
