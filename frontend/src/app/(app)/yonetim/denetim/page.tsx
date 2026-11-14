"use client";

import { useState } from "react";
import { ChevronDown, ChevronRight, ScrollText } from "lucide-react";
import { format, subDays } from "date-fns";
import { DataTable, type Column } from "@/components/common/DataTable";
import { DateText } from "@/components/common/DateText";
import { FormField } from "@/components/common/FormField";
import { SimpleSelect } from "@/components/common/SimpleSelect";
import { Input } from "@/components/ui/input";
import { UserPicker } from "@/components/common/UserPicker";
import { useAudit, useAuditActions } from "@/features/core/admin-api";
import type { AuditRow, DirUser } from "@/features/core/types";
import { useUrlParams } from "@/lib/url-state";

const ISO = "yyyy-MM-dd";

function detailLines(detail: unknown): string[] {
  if (detail === null || detail === undefined) return [];
  let value: unknown = detail;
  if (typeof detail === "string") {
    try {
      value = JSON.parse(detail);
    } catch {
      return [detail];
    }
  }
  if (typeof value !== "object" || value === null) return [String(value)];
  return Object.entries(value as Record<string, unknown>).map(
    ([k, v]) => `${k}: ${typeof v === "object" && v !== null ? JSON.stringify(v) : String(v)}`,
  );
}

export default function AuditPage() {
  const url = useUrlParams();
  const today = new Date();
  const from = url.get("bas", format(subDays(today, 7), ISO));
  const to = url.get("bit", format(today, ISO));
  const action = url.get("islem");
  const actorId = url.get("kisi");
  const page = Number(url.get("sayfa", "0")) || 0;
  const [actor, setActor] = useState<DirUser[]>([]);
  const [open, setOpen] = useState<number | null>(null);

  const actions = useAuditActions();
  const audit = useAudit({ from, to, actorId: actorId ? Number(actorId) : undefined, action: action || undefined, page });

  const columns: Column<AuditRow>[] = [
    {
      key: "time",
      header: "Zaman",
      cell: (r) => (
        <span className="flex items-center gap-1">
          {open === r.id ? (
            <ChevronDown className="size-4 text-muted-foreground" aria-hidden />
          ) : (
            <ChevronRight className="size-4 text-muted-foreground" aria-hidden />
          )}
          <DateText value={r.createdAt} />
        </span>
      ),
    },
    { key: "actor", header: "Kişi", cell: (r) => r.actorName ?? "—" },
    { key: "action", header: "İşlem", cell: (r) => <span className="font-medium">{r.actionLabel}</span> },
    {
      key: "entity",
      header: "Kayıt",
      cell: (r) => (r.entityType ? `${r.entityType}${r.entityId ? ` #${r.entityId}` : ""}` : "—"),
    },
    { key: "ip", header: "IP", cell: (r) => r.ip ?? "—" },
  ];

  const items = audit.data?.items;

  return (
    <div>
      <div className="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <FormField label="Başlangıç" htmlFor="a-from">
          <Input
            id="a-from"
            type="date"
            className="h-11 bg-card md:h-10"
            value={from}
            max={to}
            onChange={(e) => e.target.value && url.set({ bas: e.target.value, sayfa: undefined })}
          />
        </FormField>
        <FormField label="Bitiş" htmlFor="a-to">
          <Input
            id="a-to"
            type="date"
            className="h-11 bg-card md:h-10"
            value={to}
            min={from}
            onChange={(e) => e.target.value && url.set({ bit: e.target.value, sayfa: undefined })}
          />
        </FormField>
        <FormField label="Kişi" htmlFor="a-actor">
          <UserPicker
            id="a-actor"
            multiple={false}
            value={actor}
            lookup={{}}
            placeholder="Tüm kişiler"
            onChange={(u) => {
              setActor(u);
              url.set({ kisi: u[0] ? String(u[0].id) : undefined, sayfa: undefined });
            }}
          />
          {actor.length > 0 && (
            <button
              type="button"
              className="min-h-9 text-[13px] text-primary underline-offset-2 hover:underline"
              onClick={() => {
                setActor([]);
                url.set({ kisi: undefined, sayfa: undefined });
              }}
            >
              Kişi filtresini kaldır
            </button>
          )}
        </FormField>
        <FormField label="İşlem" htmlFor="a-action">
          <SimpleSelect
            id="a-action"
            value={action || "all"}
            onChange={(v) => url.set({ islem: v === "all" ? undefined : v, sayfa: undefined })}
            options={[
              { value: "all", label: "Tüm işlemler" },
              ...(actions.data ?? []).map((a) => ({ value: a.code, label: a.label })),
            ]}
          />
        </FormField>
      </div>

      <DataTable
        columns={columns}
        rows={items}
        rowKey={(r) => r.id}
        loading={audit.isPending}
        error={audit.error}
        onRetry={() => void audit.refetch()}
        onRowClick={(r) => setOpen(open === r.id ? null : r.id)}
        expandedRow={(r) => {
          if (open !== r.id) return null;
          const lines = detailLines(r.detail);
          return lines.length === 0 ? (
            <p className="text-sm text-muted-foreground">Bu kayıt için ek bilgi yok.</p>
          ) : (
            <ul className="space-y-1 text-sm">
              {lines.map((l, i) => (
                <li key={i} className="break-words">
                  {l}
                </li>
              ))}
            </ul>
          );
        }}
        emptyMessage="Bu aralıkta denetim kaydı yok. Tarih aralığını genişletmeyi deneyin."
        emptyIcon={ScrollText}
        pagination={
          audit.data
            ? {
                page: audit.data.page,
                size: audit.data.size,
                total: audit.data.total,
                onPageChange: (p) => url.set({ sayfa: p ? String(p) : undefined }),
              }
            : undefined
        }
      />
    </div>
  );
}
