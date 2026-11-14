"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { ShieldOff } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { PageSkeleton } from "@/components/common/Skeletons";
import { ADMIN_TABS } from "@/features/core/admin-tabs";
import { useMe } from "@/lib/me";

export default function AdminIndexPage() {
  const me = useMe().data;
  const router = useRouter();
  const first = me ? ADMIN_TABS.find((t) => t.allowed(me.permissions)) : undefined;

  useEffect(() => {
    if (first) router.replace(first.href);
  }, [first, router]);

  if (me && !first) {
    return <EmptyState icon={ShieldOff} message="Bu sayfaya erişim yetkiniz yok." />;
  }
  return <PageSkeleton />;
}
