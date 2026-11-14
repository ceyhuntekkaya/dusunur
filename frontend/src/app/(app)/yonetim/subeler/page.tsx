"use client";

import { useState } from "react";
import { Building2 } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { DataTable, type Column } from "@/components/common/DataTable";
import { PageHeader } from "@/components/common/PageHeader";
import { RowMenu } from "@/components/common/RowMenu";
import { StatusBadge } from "@/components/common/StatusBadge";
import { useAdminBranches, useSetBranchActive } from "@/features/core/admin-api";
import { BranchFormDialog } from "@/features/core/components/BranchFormDialog";
import type { BranchRow } from "@/features/core/types";

export default function BranchesPage() {
  const branches = useAdminBranches();
  const setActive = useSetBranchActive();
  const [form, setForm] = useState<{ branch?: BranchRow } | null>(null);
  const [deactivating, setDeactivating] = useState<BranchRow | null>(null);

  const columns: Column<BranchRow>[] = [
    { key: "name", header: "Ad", cell: (b) => <span className="font-medium">{b.name}</span> },
    {
      key: "status",
      header: "Durum",
      cell: (b) => <StatusBadge tone={b.active ? "success" : "neutral"}>{b.active ? "Aktif" : "Pasif"}</StatusBadge>,
    },
    { key: "count", header: "Bu yılki personel", cell: (b) => b.memberCount },
    {
      key: "menu",
      header: <span className="sr-only">İşlemler</span>,
      className: "w-14 text-right",
      cell: (b) => (
        <RowMenu
          label={`${b.name} işlemleri`}
          items={[
            { label: "Yeniden adlandır", onSelect: () => setForm({ branch: b }) },
            { label: "Pasifleştir", onSelect: () => setDeactivating(b), hidden: !b.active },
            {
              label: "Aktifleştir",
              hidden: b.active,
              onSelect: () =>
                setActive.mutate(
                  { id: b.id, active: true },
                  { onSuccess: () => toast.success("Şube aktifleştirildi.") },
                ),
            },
          ]}
        />
      ),
    },
  ];

  return (
    <div>
      <PageHeader title="Şubeler" primaryAction={{ label: "Şube Ekle", onClick: () => setForm({}) }} />
      <DataTable
        columns={columns}
        rows={branches.data}
        rowKey={(b) => b.id}
        loading={branches.isPending}
        error={branches.error}
        onRetry={() => void branches.refetch()}
        emptyMessage="Henüz şube yok. Şube Ekle ile ilk şubeyi oluşturun."
        emptyIcon={Building2}
      />
      {form && (
        <BranchFormDialog open onOpenChange={(o) => !o && setForm(null)} branch={form.branch} />
      )}
      <ConfirmDialog
        open={!!deactivating}
        onOpenChange={(o) => !o && setDeactivating(null)}
        title="Şube pasifleştirilsin mi?"
        description={`${deactivating?.name ?? ""} şubesi pasifleştirilecek ve yeni atamalarda seçilemeyecek.`}
        confirmLabel="Pasifleştir"
        destructive
        onConfirm={async () => {
          if (!deactivating) return;
          await setActive.mutateAsync({ id: deactivating.id, active: false });
          toast.success("Şube pasifleştirildi.");
        }}
      />
    </div>
  );
}
