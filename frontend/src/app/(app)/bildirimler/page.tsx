"use client";

import { MarkAllReadButton } from "@/features/core/components/MarkAllReadButton";
import { NotificationList } from "@/features/core/components/NotificationList";
import { PageHeader } from "@/components/common/PageHeader";

export default function NotificationsPage() {
  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title="Bildirimler" actions={<MarkAllReadButton />} />
      <NotificationList />
    </div>
  );
}
