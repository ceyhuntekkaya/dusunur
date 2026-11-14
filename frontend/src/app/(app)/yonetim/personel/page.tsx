"use client";

import { useState } from "react";
import { Upload, Users, X } from "lucide-react";
import { toast } from "sonner";
import { CheckboxField } from "@/components/common/CheckboxField";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { DataTable, type Column } from "@/components/common/DataTable";
import { DateText } from "@/components/common/DateText";
import { PageHeader } from "@/components/common/PageHeader";
import { RowMenu } from "@/components/common/RowMenu";
import { SearchInput } from "@/components/common/SearchInput";
import { SimpleSelect } from "@/components/common/SimpleSelect";
import { StatusBadge } from "@/components/common/StatusBadge";
import { UserAvatar } from "@/components/common/UserAvatar";
import { Button } from "@/components/ui/button";
import {
  useActiveBranches,
  useAdminUsers,
  useReactivateUser,
  useSendSetupLink,
} from "@/features/core/admin-api";
import { DeactivateUserDialog } from "@/features/core/components/DeactivateUserDialog";
import { ImportUsersDialog } from "@/features/core/components/ImportUsersDialog";
import { UserFormDialog } from "@/features/core/components/UserFormDialog";
import type { AdminUserRow } from "@/features/core/types";
import { useMe } from "@/lib/me";
import { useUrlParams } from "@/lib/url-state";

const STATUS_OPTIONS = [
  { value: "ACTIVE", label: "Aktif" },
  { value: "PASSIVE", label: "Pasif" },
  { value: "ALL", label: "Tümü" },
];

export default function StaffPage() {
  const me = useMe().data;
  const url = useUrlParams();
  const branches = useActiveBranches();
  const sendLink = useSendSetupLink();
  const reactivate = useReactivateUser();

  const q = url.get("q");
  const status = url.get("durum", "ACTIVE") as "ACTIVE" | "PASSIVE" | "ALL";
  const branchId = url.get("sube");
  const unassignedOnly = url.get("atanmamis") === "1";
  const noPassword = url.get("parola") === "yok";
  const page = Number(url.get("sayfa", "0")) || 0;

  const users = useAdminUsers({
    q,
    status,
    branchId: branchId ? Number(branchId) : undefined,
    assigned: unassignedOnly ? false : undefined,
    passwordSet: noPassword ? false : undefined,
    page,
  });

  const [form, setForm] = useState<{ user?: AdminUserRow } | null>(null);
  const [importOpen, setImportOpen] = useState(false);
  const [linkTarget, setLinkTarget] = useState<AdminUserRow | null>(null);
  const [deactivating, setDeactivating] = useState<AdminUserRow | null>(null);

  const columns: Column<AdminUserRow>[] = [
    {
      key: "name",
      header: "Ad Soyad",
      className: "min-w-56",
      cell: (u) => (
        <div className="flex items-center gap-3">
          <UserAvatar userId={u.id} name={u.fullName} size="sm" />
          <div className="min-w-0">
            <div className="truncate font-medium">{u.fullName}</div>
            <div className="truncate text-[13px] text-muted-foreground">{u.email}</div>
          </div>
        </div>
      ),
    },
    { key: "branch", header: "Şube", cell: (u) => u.membership?.branchName ?? "—" },
    { key: "title", header: "Unvan", cell: (u) => u.membership?.title ?? "—" },
    {
      key: "roles",
      header: "Yetkiler",
      className: "min-w-48",
      cell: (u) => (
        <div className="flex flex-wrap gap-1">
          {u.systemAdmin && (
            <StatusBadge className="bg-purple-100 text-purple-800">Sistem Yöneticisi</StatusBadge>
          )}
          {u.membership?.manager && <StatusBadge tone="info">Yönetici</StatusBadge>}
          {u.membership?.printOperator && <StatusBadge tone="success">Fotokopi Sorumlusu</StatusBadge>}
          {u.membership?.canCreateDmsSpace && <StatusBadge>Doküman Alanı Açabilir</StatusBadge>}
        </div>
      ),
    },
    { key: "approver", header: "Onaylayıcı", cell: (u) => u.membership?.printApproverName ?? "—" },
    {
      key: "status",
      header: "Durum",
      className: "min-w-40",
      cell: (u) => (
        <div className="flex flex-wrap gap-1">
          <StatusBadge tone={u.status === "ACTIVE" ? "success" : "neutral"}>
            {u.status === "ACTIVE" ? "Aktif" : "Pasif"}
          </StatusBadge>
          {!u.passwordSet && <StatusBadge tone="warning">Şifre belirlenmedi</StatusBadge>}
          {!u.membership && <StatusBadge>Bu yıla atanmamış</StatusBadge>}
        </div>
      ),
    },
    {
      key: "last",
      header: "Son giriş",
      cell: (u) =>
        u.lastLoginAt ? <DateText value={u.lastLoginAt} /> : <span className="text-muted-foreground">—</span>,
    },
    {
      key: "menu",
      header: <span className="sr-only">İşlemler</span>,
      className: "w-14 text-right",
      cell: (u) => (
        <RowMenu
          label={`${u.fullName} işlemleri`}
          items={[
            { label: "Düzenle", onSelect: () => setForm({ user: u }) },
            { label: "Şifre bağlantısı gönder", onSelect: () => setLinkTarget(u), hidden: u.status !== "ACTIVE" },
            {
              label: "Pasifleştir",
              destructive: true,
              onSelect: () => setDeactivating(u),
              hidden: u.status !== "ACTIVE" || u.id === me?.id,
            },
            {
              label: "Aktifleştir",
              hidden: u.status !== "PASSIVE",
              onSelect: () =>
                reactivate.mutate(u.id, {
                  onSuccess: () =>
                    toast.success("Personel aktifleştirildi. Giriş yapabilmesi için bu yıla atanmış olması gerekir."),
                }),
            },
          ]}
        />
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Personel"
        actions={
          <Button variant="outline" className="h-11 md:h-10" onClick={() => setImportOpen(true)}>
            <Upload aria-hidden />
            Toplu Yükle
          </Button>
        }
        primaryAction={{ label: "Personel Ekle", onClick: () => setForm({}) }}
      />

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <SearchInput
          label="Personel ara"
          placeholder="Ad, soyad veya e-posta"
          className="w-full md:w-72"
          value={q}
          onChange={(v) => url.set({ q: v, sayfa: undefined })}
        />
        <SimpleSelect
          className="w-full md:w-48"
          value={branchId || "all"}
          onChange={(v) => url.set({ sube: v === "all" ? undefined : v, sayfa: undefined })}
          options={[
            { value: "all", label: "Tüm şubeler" },
            ...(branches.data ?? []).map((b) => ({ value: String(b.id), label: b.name })),
          ]}
        />
        <SimpleSelect
          className="w-full md:w-36"
          value={status}
          onChange={(v) => url.set({ durum: v === "ACTIVE" ? undefined : v, sayfa: undefined })}
          options={STATUS_OPTIONS}
        />
        <CheckboxField
          id="f-unassigned"
          checked={unassignedOnly}
          onCheckedChange={(c) => url.set({ atanmamis: c ? "1" : undefined, sayfa: undefined })}
          label="Bu yıla atanmamışlar"
        />
        {noPassword && (
          <button
            type="button"
            className="flex h-8 items-center gap-1 rounded-full bg-accent pr-2 pl-3 text-[13px] font-medium text-accent-foreground"
            onClick={() => url.set({ parola: undefined, sayfa: undefined })}
          >
            Şifresini belirlememiş olanlar
            <X className="size-3.5" aria-label="Filtreyi kaldır" />
          </button>
        )}
      </div>

      <DataTable
        columns={columns}
        rows={users.data?.items}
        rowKey={(u) => u.id}
        loading={users.isPending}
        error={users.error}
        onRetry={() => void users.refetch()}
        emptyMessage="Aramanıza uyan personel yok. Filtreleri değiştirin ya da Personel Ekle ile yeni kişi ekleyin."
        emptyIcon={Users}
        pagination={
          users.data
            ? {
                page: users.data.page,
                size: users.data.size,
                total: users.data.total,
                onPageChange: (p) => url.set({ sayfa: p ? String(p) : undefined }),
              }
            : undefined
        }
      />

      {form && <UserFormDialog open onOpenChange={(o) => !o && setForm(null)} user={form.user} />}
      <ImportUsersDialog open={importOpen} onOpenChange={setImportOpen} />
      <DeactivateUserDialog user={deactivating} onOpenChange={(o) => !o && setDeactivating(null)} />
      <ConfirmDialog
        open={!!linkTarget}
        onOpenChange={(o) => !o && setLinkTarget(null)}
        title="Şifre bağlantısı gönderilsin mi?"
        description={`${linkTarget?.fullName ?? ""} kişisine ${linkTarget?.email ?? ""} adresine şifre belirleme bağlantısı gönderilecek. Önceki bağlantılar geçersiz olur.`}
        confirmLabel="Gönder"
        onConfirm={async () => {
          if (!linkTarget) return;
          await sendLink.mutateAsync(linkTarget.id);
          toast.success("Bağlantı gönderildi.");
        }}
      />
    </div>
  );
}
