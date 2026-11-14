"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { UsersRound } from "lucide-react";
import { DataTable, type Column } from "@/components/common/DataTable";
import { PageHeader } from "@/components/common/PageHeader";
import { SearchInput } from "@/components/common/SearchInput";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { useGroups } from "@/features/core/admin-api";
import { GroupCreateDialog } from "@/features/core/components/GroupCreateDialog";
import type { GroupRow } from "@/features/core/types";
import { useUrlParams } from "@/lib/url-state";

export default function GroupsPage() {
  const router = useRouter();
  const url = useUrlParams();
  const q = url.get("q");
  const archived = url.get("arsiv") === "1";
  const groups = useGroups({ q, archived });
  const [creating, setCreating] = useState(false);

  const columns: Column<GroupRow>[] = [
    {
      key: "name",
      header: "Ad",
      cell: (g) => (
        <Link href={`/yonetim/gruplar/${g.id}`} className="font-medium text-primary hover:underline">
          {g.name}
        </Link>
      ),
    },
    {
      key: "desc",
      header: "Açıklama",
      className: "max-w-80",
      cell: (g) => <span className="line-clamp-2 text-muted-foreground">{g.description ?? "—"}</span>,
    },
    { key: "count", header: "Üye sayısı", cell: (g) => g.memberCount },
    {
      key: "status",
      header: "Durum",
      cell: (g) => (
        <StatusBadge tone={g.archived ? "neutral" : "success"}>{g.archived ? "Arşivlenmiş" : "Aktif"}</StatusBadge>
      ),
    },
  ];

  return (
    <div>
      <PageHeader title="Gruplar" primaryAction={{ label: "Grup Oluştur", onClick: () => setCreating(true) }} />
      <div className="mb-4 flex flex-wrap items-center gap-4">
        <SearchInput
          label="Grup ara"
          placeholder="Grup adı"
          className="w-full md:w-72"
          value={q}
          onChange={(v) => url.set({ q: v })}
        />
        <div className="flex min-h-11 items-center gap-2">
          <Switch
            id="show-archived"
            checked={archived}
            onCheckedChange={(c) => url.set({ arsiv: c ? "1" : undefined })}
          />
          <Label htmlFor="show-archived" className="text-sm">
            Arşivlenmiş grupları göster
          </Label>
        </div>
      </div>
      <DataTable
        columns={columns}
        rows={groups.data}
        rowKey={(g) => g.id}
        loading={groups.isPending}
        error={groups.error}
        onRetry={() => void groups.refetch()}
        onRowClick={(g) => router.push(`/yonetim/gruplar/${g.id}`)}
        emptyMessage="Henüz grup yok. Grup Oluştur ile ilk grubu ekleyin."
        emptyIcon={UsersRound}
      />
      {creating && (
        <GroupCreateDialog
          open
          onOpenChange={(o) => !o && setCreating(false)}
          onCreated={(g) => router.push(`/yonetim/gruplar/${g.id}`)}
        />
      )}
    </div>
  );
}
