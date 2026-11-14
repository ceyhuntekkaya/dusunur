"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { CalendarRange } from "lucide-react";
import { DataTable, type Column } from "@/components/common/DataTable";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { useSeasons } from "@/features/core/admin-api";
import { SeasonFormDialog } from "@/features/core/components/SeasonFormDialog";
import { SEASON_STATUS } from "@/features/core/season-status";
import type { SeasonRow } from "@/features/core/types";
import { formatFullDate } from "@/lib/format";

export default function SeasonsPage() {
  const router = useRouter();
  const seasons = useSeasons();
  const [creating, setCreating] = useState(false);

  const rows = seasons.data;
  const hasDraft = !!rows?.some((s) => s.status === "DRAFT");
  const latest = rows?.[0]?.name.slice(0, 4);
  const suggested = latest ? Number(latest) + 1 : undefined;

  const columns: Column<SeasonRow>[] = [
    {
      key: "name",
      header: "Yıl",
      cell: (s) => (
        <Link href={`/yonetim/yillar/${s.id}`} className="font-medium text-primary hover:underline">
          {s.name}
        </Link>
      ),
    },
    {
      key: "dates",
      header: "Tarih aralığı",
      cell: (s) => `${formatFullDate(s.startDate)} – ${formatFullDate(s.endDate)}`,
    },
    {
      key: "status",
      header: "Durum",
      cell: (s) => <StatusBadge tone={SEASON_STATUS[s.status].tone}>{SEASON_STATUS[s.status].label}</StatusBadge>,
    },
    { key: "count", header: "Personel", cell: (s) => `${s.memberCount} personel` },
  ];

  return (
    <div>
      <PageHeader
        title="Eğitim-Öğretim Yılları"
        primaryAction={{
          label: "Yeni Yıl Oluştur",
          onClick: () => setCreating(true),
          disabled: hasDraft,
          disabledReason: "Hazırlık aşamasında bir yıl var.",
        }}
      />
      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(s) => s.id}
        loading={seasons.isPending}
        error={seasons.error}
        onRetry={() => void seasons.refetch()}
        onRowClick={(s) => router.push(`/yonetim/yillar/${s.id}`)}
        emptyMessage="Henüz eğitim-öğretim yılı yok. Yeni Yıl Oluştur ile ilk yılı ekleyin."
        emptyIcon={CalendarRange}
      />
      {creating && (
        <SeasonFormDialog
          open
          onOpenChange={(o) => !o && setCreating(false)}
          suggestedStartYear={suggested}
          onCreated={(s) => router.push(`/yonetim/yillar/${s.id}`)}
        />
      )}
    </div>
  );
}
