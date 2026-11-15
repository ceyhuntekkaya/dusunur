"use client";

import Link from "next/link";
import { format, parseISO } from "date-fns";
import { tr } from "date-fns/locale";
import { FileText } from "lucide-react";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Skeleton } from "@/components/ui/skeleton";
import { useCalendars, useUpcomingEntries } from "@/features/calendar/api";
import type { EntryDto } from "@/features/calendar/types";
import { useRecentFiles } from "@/features/dokumanlar/api";
import { usePrintLiveRefresh, useMyActiveRequests, usePendingApprovals } from "@/features/fotokopi/api";
import { formatDue, STATUS_LABEL, STATUS_TONE } from "@/features/fotokopi/labels";

type QueryState = { isPending: boolean; isError: boolean };

function SummaryCard({
  title,
  href,
  query,
  empty,
  children,
}: {
  title: string;
  href: string;
  query: QueryState;
  empty?: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-xl border bg-card p-4" aria-label={title}>
      <div className="mb-2 flex items-center justify-between gap-2">
        <h2 className="text-sm font-semibold">{title}</h2>
        <Link href={href} className="text-xs font-medium text-primary hover:underline">
          Tümü
        </Link>
      </div>
      {query.isPending ? (
        <div className="space-y-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
      ) : query.isError ? (
        <p className="text-[13px] text-muted-foreground">Bilgiler yüklenemedi. Sayfayı yenileyip tekrar deneyin.</p>
      ) : empty ? (
        <p className="text-[13px] text-muted-foreground">{empty}</p>
      ) : (
        children
      )}
    </section>
  );
}

const ROW = "block rounded-lg px-2 py-2 hover:bg-muted";

function entryWhen(e: EntryDto): string {
  return e.allDay && e.startDate
    ? format(parseISO(e.startDate), "d MMM EEE", { locale: tr })
    : format(parseISO(e.startAt), "d MMM EEE HH:mm", { locale: tr });
}

export function UpcomingEventsCard() {
  const q = useUpcomingEntries(7);
  const calendars = useCalendars();
  const colors = new Map((calendars.data ?? []).map((c) => [c.id, c.color]));
  const items = (q.data ?? []).slice(0, 5);
  return (
    <SummaryCard
      title="Takvimde Yaklaşanlar"
      href="/takvim"
      query={q}
      empty={items.length === 0 ? "Önümüzdeki 7 günde takvimde kayıt yok." : undefined}
    >
      <ul className="space-y-1">
        {items.map((e) => (
          <li key={e.id}>
            <Link href="/takvim" className={`${ROW} flex items-start gap-2`}>
              <span
                className="mt-1.5 size-2.5 shrink-0 rounded-full"
                style={{ backgroundColor: colors.get(e.calendarId) ?? "#0A66C2" }}
                aria-hidden
              />
              <span className="min-w-0">
                <span className="block truncate text-[13px] font-medium">{e.title}</span>
                <span className="block text-xs text-muted-foreground">{entryWhen(e)}</span>
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </SummaryCard>
  );
}

export function MyPrintRequestsCard() {
  const q = useMyActiveRequests();
  const items = q.data ?? [];
  return (
    <SummaryCard
      title="Aktif Fotokopi Taleplerim"
      href="/fotokopi?sekme=taleplerim"
      query={q}
      empty={items.length === 0 ? "Devam eden fotokopi talebiniz yok." : undefined}
    >
      <ul className="space-y-1">
        {items.map((r) => (
          <li key={r.id}>
            <Link href={`/fotokopi/${r.id}`} className={ROW}>
              <span className="block truncate text-[13px] font-medium">{r.title}</span>
              <span className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-1">
                <StatusBadge tone={STATUS_TONE[r.status]}>{STATUS_LABEL[r.status]}</StatusBadge>
                <span className="text-xs text-muted-foreground">Teslim: {formatDue(r.dueAt)}</span>
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </SummaryCard>
  );
}

export function PendingApprovalsCard() {
  const q = usePendingApprovals();
  if (!q.isPending && !q.isError && (q.data?.total ?? 0) === 0) return null;
  const items = q.data?.items ?? [];
  return (
    <SummaryCard title={`Onay Bekleyenler${q.data ? ` (${q.data.total})` : ""}`} href="/fotokopi?sekme=onay" query={q}>
      <ul className="space-y-1">
        {items.map((r) => (
          <li key={r.id}>
            <Link href={`/fotokopi/${r.id}`} className={ROW}>
              <span className="block truncate text-[13px] font-medium">{r.title}</span>
              <span className="block text-xs text-muted-foreground">
                {r.requester.fullName} · Teslim: {formatDue(r.dueAt)}
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </SummaryCard>
  );
}

export function RecentDocsCard() {
  const q = useRecentFiles();
  const items = q.data ?? [];
  return (
    <SummaryCard
      title="Son Eklenen Dokümanlar"
      href="/dokumanlar"
      query={q}
      empty={items.length === 0 ? "Görüntüleyebileceğiniz bir doküman yok." : undefined}
    >
      <ul className="space-y-1">
        {items.map((f) => {
          const params = new URLSearchParams();
          if (f.folderId !== undefined) params.set("klasor", String(f.folderId));
          params.set("vurgu", String(f.id));
          return (
            <li key={f.id}>
              <Link href={`/dokumanlar/${f.spaceId}?${params.toString()}`} className={`${ROW} flex items-start gap-2`}>
                <FileText className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden />
                <span className="min-w-0">
                  <span className="block truncate text-[13px] font-medium">{f.name}</span>
                  <span className="block truncate text-xs text-muted-foreground">{f.spaceName}</span>
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
    </SummaryCard>
  );
}

export function SideSummary() {
  usePrintLiveRefresh();
  return (
    <>
      <PendingApprovalsCard />
      <UpcomingEventsCard />
      <MyPrintRequestsCard />
      <RecentDocsCard />
    </>
  );
}
