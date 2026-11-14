"use client";

import { useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Users } from "lucide-react";
import { toast } from "sonner";
import { ArchiveBanner } from "@/components/common/ArchiveBanner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { DataTable, type Column } from "@/components/common/DataTable";
import { ErrorState } from "@/components/common/ErrorState";
import { RowMenu } from "@/components/common/RowMenu";
import { SearchInput } from "@/components/common/SearchInput";
import { PageSkeleton } from "@/components/common/Skeletons";
import { SimpleSelect } from "@/components/common/SimpleSelect";
import { StatusBadge } from "@/components/common/StatusBadge";
import { UserAvatar } from "@/components/common/UserAvatar";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  useActiveBranches,
  useBulkDelete,
  useBulkUpdate,
  useDeleteSeason,
  useRemoveMembership,
  useSeason,
  useSeasonMemberships,
  useSeasons,
  useUnassignedUsers,
} from "@/features/core/admin-api";
import { ActivateSeasonDialog } from "@/features/core/components/ActivateSeasonDialog";
import { AssignUnassignedDialog } from "@/features/core/components/AssignUnassignedDialog";
import {
  BulkApproverDialog,
  BulkBranchDialog,
  RemoveMembershipsDialog,
} from "@/features/core/components/BulkMembershipDialogs";
import { CopyFromSeasonDialog } from "@/features/core/components/CopyFromSeasonDialog";
import { MembershipEditDialog } from "@/features/core/components/MembershipEditDialog";
import { SeasonFormDialog } from "@/features/core/components/SeasonFormDialog";
import { SEASON_STATUS } from "@/features/core/season-status";
import type { MembershipFilter, MembershipRow } from "@/features/core/types";
import { formatFullDate } from "@/lib/format";
import { useUrlParams } from "@/lib/url-state";
import { cn } from "@/lib/utils";

const FILTERS: { value: MembershipFilter; label: string }[] = [
  { value: "manager", label: "Yöneticiler" },
  { value: "printOperator", label: "Fotokopi sorumluları" },
  { value: "dmsCreator", label: "Doküman alanı açabilenler" },
  { value: "noApprover", label: "Onaylayıcısı olmayanlar" },
];

type Bulk = "branch" | "approver" | "clear" | "remove" | null;

export default function SeasonDetailPage() {
  const id = Number(useParams<{ id: string }>().id);
  const router = useRouter();
  const url = useUrlParams();
  const season = useSeason(id);
  const seasons = useSeasons();
  const branches = useActiveBranches();

  const q = url.get("q");
  const branchId = url.get("sube");
  const filter = (url.get("filtre") || undefined) as MembershipFilter | undefined;
  const page = Number(url.get("sayfa", "0")) || 0;

  const writable = season.data?.status !== "CLOSED";
  const memberships = useSeasonMemberships(
    id,
    { q, branchId: branchId ? Number(branchId) : undefined, filter, page },
    !!season.data,
  );
  const unassigned = useUnassignedUsers(id, "", !!season.data && writable);

  const removeOne = useRemoveMembership(id);
  const bulkUpdate = useBulkUpdate(id);
  const bulkDelete = useBulkDelete(id);
  const deleteSeason = useDeleteSeason();

  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [bulk, setBulk] = useState<Bulk>(null);
  const [edit, setEdit] = useState<MembershipRow | null>(null);
  const [removing, setRemoving] = useState<MembershipRow | null>(null);
  const [assignOpen, setAssignOpen] = useState(false);
  const [copyOpen, setCopyOpen] = useState(false);
  const [activateOpen, setActivateOpen] = useState(false);
  const [editSeason, setEditSeason] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  if (season.isPending) return <PageSkeleton />;
  if (season.isError) return <ErrorState error={season.error} onRetry={() => void season.refetch()} />;

  const s = season.data;
  const st = SEASON_STATUS[s.status];
  const rows = memberships.data?.items;
  const unassignedCount = unassigned.data?.length ?? 0;
  const ids = [...selected];
  const pageAllSelected = !!rows && rows.length > 0 && rows.every((r) => selected.has(r.id));

  function toggle(rowId: number, on: boolean) {
    const next = new Set(selected);
    if (on) next.add(rowId);
    else next.delete(rowId);
    setSelected(next);
  }

  function togglePage(on: boolean) {
    const next = new Set(selected);
    for (const r of rows ?? []) {
      if (on) next.add(r.id);
      else next.delete(r.id);
    }
    setSelected(next);
  }

  const columns: Column<MembershipRow>[] = [
    ...(writable
      ? [
          {
            key: "check",
            header: (
              <Checkbox
                aria-label="Sayfadaki tümünü seç"
                checked={pageAllSelected}
                onCheckedChange={(v) => togglePage(v === true)}
              />
            ),
            className: "w-10",
            cell: (r: MembershipRow) => (
              <Checkbox
                aria-label={`${r.fullName} seç`}
                checked={selected.has(r.id)}
                onCheckedChange={(v) => toggle(r.id, v === true)}
              />
            ),
          },
        ]
      : []),
    {
      key: "name",
      header: "Ad Soyad",
      className: "min-w-56",
      cell: (r) => (
        <div className="flex items-center gap-3">
          <UserAvatar userId={r.userId} name={r.fullName} size="sm" />
          <div className="min-w-0">
            <div className="truncate font-medium">{r.fullName}</div>
            <div className="truncate text-[13px] text-muted-foreground">{r.email}</div>
          </div>
          {r.userStatus === "PASSIVE" && <StatusBadge>Pasif</StatusBadge>}
        </div>
      ),
    },
    { key: "branch", header: "Şube", cell: (r) => r.branchName },
    { key: "title", header: "Unvan", cell: (r) => r.title ?? "—" },
    {
      key: "roles",
      header: "Yetkiler",
      className: "min-w-44",
      cell: (r) => (
        <div className="flex flex-wrap gap-1">
          {r.manager && <StatusBadge tone="info">Yönetici</StatusBadge>}
          {r.printOperator && <StatusBadge tone="success">Fotokopi Sorumlusu</StatusBadge>}
          {r.canCreateDmsSpace && <StatusBadge>Doküman Alanı Açabilir</StatusBadge>}
        </div>
      ),
    },
    { key: "approver", header: "Onaylayıcı", cell: (r) => r.printApproverName ?? "—" },
    ...(writable
      ? [
          {
            key: "menu",
            header: <span className="sr-only">İşlemler</span>,
            className: "w-14 text-right",
            cell: (r: MembershipRow) => (
              <RowMenu
                label={`${r.fullName} işlemleri`}
                items={[
                  { label: "Düzenle", onSelect: () => setEdit(r) },
                  { label: "Yıldan çıkar", destructive: true, onSelect: () => setRemoving(r) },
                ]}
              />
            ),
          },
        ]
      : []),
  ];

  const removeNote = s.status === "ACTIVE" ? " Oturumları kapatılacak." : "";

  return (
    <div className={cn(selected.size > 0 && "pb-24")}>
      <Link
        href="/yonetim/yillar"
        className="mb-3 inline-flex min-h-11 items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" aria-hidden />
        Eğitim-Öğretim Yılları
      </Link>

      <div className="mb-4 flex flex-wrap items-start justify-between gap-3 md:mb-6">
        <div className="min-w-0">
          <div className="flex items-center gap-3">
            <h2 className="text-xl font-semibold md:text-[22px] md:leading-7">{s.name}</h2>
            <StatusBadge tone={st.tone}>{st.label}</StatusBadge>
          </div>
          <p className="mt-1 text-[13px] text-muted-foreground">
            {formatFullDate(s.startDate)} – {formatFullDate(s.endDate)} · {s.memberCount} personel
          </p>
        </div>
        <div className="flex items-center gap-2">
          {s.status === "DRAFT" && (
            <>
              <Button variant="outline" className="h-11 md:h-10" onClick={() => setCopyOpen(true)}>
                Önceki Yıldan Kopyala
              </Button>
              <Button className="h-11 md:h-10" onClick={() => setActivateOpen(true)}>
                Yılı Aktifleştir
              </Button>
            </>
          )}
          {s.status !== "CLOSED" && (
            <RowMenu
              label="Yıl işlemleri"
              items={[
                { label: "Düzenle", onSelect: () => setEditSeason(true) },
                { label: "Sil", destructive: true, hidden: s.status !== "DRAFT", onSelect: () => setDeleteOpen(true) },
              ]}
            />
          )}
        </div>
      </div>

      {s.status === "CLOSED" && <ArchiveBanner seasonName={s.name} />}

      {writable && unassignedCount > 0 && (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-info-bg px-4 py-3 text-sm text-primary">
          <span>Bu yıla atanmamış {unassignedCount} aktif personel var.</span>
          <Button variant="outline" className="h-11 bg-card md:h-10" onClick={() => setAssignOpen(true)}>
            Göster ve Ata
          </Button>
        </div>
      )}

      <div className="mb-3 flex flex-wrap items-center gap-3">
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
      </div>
      <div className="mb-4 flex flex-wrap gap-2" role="group" aria-label="Hızlı filtreler">
        {FILTERS.map((f) => {
          const on = filter === f.value;
          return (
            <button
              key={f.value}
              type="button"
              aria-pressed={on}
              className={cn(
                "h-9 rounded-full border px-3 text-[13px] font-medium outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
                on ? "border-primary bg-accent text-accent-foreground" : "bg-card text-muted-foreground hover:text-foreground",
              )}
              onClick={() => url.set({ filtre: on ? undefined : f.value, sayfa: undefined })}
            >
              {f.label}
            </button>
          );
        })}
      </div>

      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(r) => r.id}
        loading={memberships.isPending}
        error={memberships.error}
        onRetry={() => void memberships.refetch()}
        emptyMessage={
          writable
            ? "Bu yılda gösterilecek personel yok. Atanmamış personeli atayın ya da önceki yıldan kopyalayın."
            : "Bu yılda gösterilecek personel yok."
        }
        emptyIcon={Users}
        pagination={
          memberships.data
            ? {
                page: memberships.data.page,
                size: memberships.data.size,
                total: memberships.data.total,
                onPageChange: (p) => url.set({ sayfa: p ? String(p) : undefined }),
              }
            : undefined
        }
      />

      {selected.size > 0 && (
        <div className="fixed inset-x-0 bottom-[calc(64px+env(safe-area-inset-bottom))] z-30 border-t bg-card px-4 py-3 shadow-lg md:bottom-0 md:left-[72px] lg:left-[248px]">
          <div className="mx-auto flex max-w-[1200px] flex-wrap items-center gap-2 md:px-2">
            <span className="mr-2 text-sm font-medium">{selected.size} kişi seçildi</span>
            <Button variant="outline" className="h-11 md:h-10" onClick={() => setBulk("branch")}>
              Şube Değiştir
            </Button>
            <Button variant="outline" className="h-11 md:h-10" onClick={() => setBulk("approver")}>
              Onaylayıcı Ata
            </Button>
            <Button variant="outline" className="h-11 md:h-10" onClick={() => setBulk("clear")}>
              Onaylayıcıyı Kaldır
            </Button>
            <Button variant="outline" className="h-11 text-destructive md:h-10" onClick={() => setBulk("remove")}>
              Yıldan Çıkar
            </Button>
            <Button variant="ghost" className="h-11 md:h-10" onClick={() => setSelected(new Set())}>
              Seçimi Temizle
            </Button>
          </div>
        </div>
      )}

      <MembershipEditDialog seasonId={id} row={edit} onOpenChange={(o) => !o && setEdit(null)} />
      <AssignUnassignedDialog seasonId={id} open={assignOpen} onOpenChange={setAssignOpen} />
      {seasons.data && (
        <CopyFromSeasonDialog target={s} seasons={seasons.data} open={copyOpen} onOpenChange={setCopyOpen} />
      )}
      <ActivateSeasonDialog season={s} open={activateOpen} onOpenChange={setActivateOpen} />
      {editSeason && <SeasonFormDialog open onOpenChange={(o) => !o && setEditSeason(false)} season={s} />}

      <BulkBranchDialog
        seasonId={id}
        ids={ids}
        open={bulk === "branch"}
        onOpenChange={(o) => !o && setBulk(null)}
        onDone={() => setSelected(new Set())}
      />
      <BulkApproverDialog
        seasonId={id}
        ids={ids}
        open={bulk === "approver"}
        onOpenChange={(o) => !o && setBulk(null)}
        onDone={() => setSelected(new Set())}
      />
      <ConfirmDialog
        open={bulk === "clear"}
        onOpenChange={(o) => !o && setBulk(null)}
        title="Onaylayıcı kaldırılsın mı?"
        description={`${selected.size} kişinin fotokopi onaylayıcısı kaldırılacak. Değişiklik yeni taleplere uygulanır.`}
        confirmLabel="Onaylayıcıyı Kaldır"
        onConfirm={async () => {
          const res = await bulkUpdate.mutateAsync({ membershipIds: ids, clearApprover: true });
          toast.success(`${res.updated} kayıt güncellendi.`);
          setSelected(new Set());
        }}
      />
      <RemoveMembershipsDialog
        open={bulk === "remove"}
        onOpenChange={(o) => !o && setBulk(null)}
        description={`${selected.size} kişi bu yıldan çıkarılacak.${removeNote} Bu işlem geri alınamaz.`}
        onRemove={async () => {
          const res = await bulkDelete.mutateAsync(ids);
          toast.success(`${res.deleted} kişi yıldan çıkarıldı.`);
          setSelected(new Set());
        }}
      />
      <RemoveMembershipsDialog
        open={!!removing}
        onOpenChange={(o) => !o && setRemoving(null)}
        description={`${removing?.fullName ?? ""} bu yıldan çıkarılacak.${removeNote} Bu işlem geri alınamaz.`}
        onRemove={async () => {
          if (!removing) return;
          await removeOne.mutateAsync(removing.id);
          toast.success("Personel yıldan çıkarıldı.");
          toggle(removing.id, false);
        }}
      />
      <ConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title="Yıl silinsin mi?"
        description={`${s.name} ve tüm atamaları silinecek. Bu işlem geri alınamaz.`}
        confirmLabel="Sil"
        destructive
        onConfirm={async () => {
          await deleteSeason.mutateAsync(id);
          toast.success("Yıl silindi.");
          router.replace("/yonetim/yillar");
        }}
      />
    </div>
  );
}
