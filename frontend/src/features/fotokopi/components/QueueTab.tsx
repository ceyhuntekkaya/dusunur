"use client";

import { useState } from "react";
import Link from "next/link";
import { Download } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { SegmentedControl } from "@/components/common/SegmentedControl";
import { ListSkeleton } from "@/components/common/Skeletons";
import { StatusBadge } from "@/components/common/StatusBadge";
import { UserAvatar } from "@/components/common/UserAvatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useQueueBoard, printFileUrl } from "../api";
import { COLOR_LABEL, PAPER_LABEL, SIDES_LABEL, dueState, formatDue } from "../labels";
import type { PrintRequestRow } from "../types";
import { PagedRequestList } from "./PagedRequestList";
import { PrintActionButtons, type ActionButtonKind } from "./PrintActionButtons";
import { PrintRequestCard } from "./PrintRequestCard";
import { cn } from "@/lib/utils";
import { Lock, Printer } from "lucide-react";
import { format } from "date-fns";

type ColumnKey = "approved" | "inProgress" | "ready";

const COLUMNS: { key: ColumnKey; title: string; segment: string; kinds: ActionButtonKind[] }[] = [
  { key: "approved", title: "Yeni", segment: "Yeni", kinds: ["start", "ready"] },
  { key: "inProgress", title: "Hazırlanıyor", segment: "Hazırlanıyor", kinds: ["ready"] },
  { key: "ready", title: "Teslim Bekliyor", segment: "Teslim Bekliyor", kinds: ["deliver"] },
];

function QueueCard({ row, kinds }: { row: PrintRequestRow; kinds: ActionButtonKind[] }) {
  const due = dueState(row);
  return (
    <article className="relative rounded-xl border bg-card p-3 shadow-sm">
      <Link
        href={`/fotokopi/${row.id}`}
        className="line-clamp-2 text-[15px] font-semibold outline-none after:absolute after:inset-0 after:rounded-xl focus-visible:after:ring-3 focus-visible:after:ring-ring/50"
      >
        {row.secret && <Lock className="mr-1 inline size-4 align-[-2px]" aria-label="Gizli evrak" />}
        {row.title}
      </Link>
      <p className="mt-1 text-2xl font-semibold">{row.totalPages.toLocaleString("tr-TR")} baskı</p>
      <div className="mt-1 flex flex-wrap gap-1.5">
        <StatusBadge tone={row.color === "COLOR" ? "info" : "neutral"}>{COLOR_LABEL[row.color]}</StatusBadge>
        <StatusBadge>{PAPER_LABEL[row.paperSize]}</StatusBadge>
        <StatusBadge>{SIDES_LABEL[row.sides]}</StatusBadge>
        {row.originalOnPaper && <StatusBadge tone="warning">Basılı orijinal</StatusBadge>}
      </div>
      <p
        className={cn(
          "mt-2 text-[13px]",
          due === "overdue" && "text-destructive",
          due === "today" && "text-warning",
          due === "normal" && "text-muted-foreground",
        )}
      >
        {due === "overdue" && "Gecikti · "}
        {due === "today" && "Bugün · "}Teslim: {formatDue(row.dueAt)}
      </p>
      <div className="mt-2 flex items-center gap-2 text-[13px]">
        <UserAvatar userId={row.requester.id} name={row.requester.fullName} size="sm" />
        <span className="min-w-0 flex-1 truncate">
          {row.requester.fullName}
          <span className="text-muted-foreground"> · {row.requester.branchName ?? row.branchName}</span>
        </span>
        {!row.originalOnPaper && (
          <Button
            variant="outline"
            size="icon"
            className="relative z-10 size-11 shrink-0 md:size-10"
            aria-label="Dosyayı indir"
            nativeButton={false}
            render={<a href={printFileUrl(row.id, true)} target="_blank" rel="noreferrer" />}
          >
            <Download aria-hidden />
          </Button>
        )}
      </div>
      <PrintActionButtons
        requestId={row.id}
        ownerName={row.requester.fullName}
        kinds={kinds}
        compact
        className="relative z-10 mt-3"
      />
    </article>
  );
}

function DeliveredDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (o: boolean) => void }) {
  const [date, setDate] = useState(() => format(new Date(), "yyyy-MM-dd"));
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title="Teslim edilenler" className="sm:max-w-xl">
      <div className="space-y-4">
        <Input
          type="date"
          aria-label="Teslim tarihi"
          className="h-11 md:h-10"
          value={date}
          onChange={(e) => setDate(e.target.value)}
        />
        <PagedRequestList
          queryKey={["print", "delivered", date]}
          path="/api/print/queue/delivered"
          params={{ date: date || undefined }}
          renderItem={(r) => <PrintRequestCard request={r} context="person" />}
          empty={<EmptyState icon={Printer} message="Bu tarihte teslim edilen talep yok." />}
        />
      </div>
    </ResponsiveDialog>
  );
}

export function QueueTab() {
  const board = useQueueBoard(true);
  const [segment, setSegment] = useState<ColumnKey>("approved");
  const [delivered, setDelivered] = useState(false);

  if (board.isPending) return <ListSkeleton />;
  if (board.isError) return <ErrorState error={board.error} onRetry={() => void board.refetch()} />;

  return (
    <div>
      <SegmentedControl
        kind="tabs"
        ariaLabel="Kuyruk sütunu"
        className="mb-4 lg:hidden"
        value={segment}
        onChange={setSegment}
        options={COLUMNS.map((c) => ({ value: c.key, label: `${c.segment} ${board.data[c.key].length}` }))}
      />
      <div className="grid gap-4 lg:grid-cols-3">
        {COLUMNS.map((c) => {
          const rows = board.data[c.key];
          return (
            <section key={c.key} className={cn(segment === c.key ? "block" : "hidden", "lg:block")}>
              <h2 className="mb-3 hidden items-center gap-2 text-[15px] font-semibold lg:flex">
                {c.title}
                <span className="rounded-full bg-muted px-2 text-[13px] font-medium text-muted-foreground">
                  {rows.length}
                </span>
              </h2>
              {rows.length === 0 ? (
                <p className="rounded-xl border border-dashed p-6 text-center text-[13px] text-muted-foreground">
                  Bu sütunda talep yok.
                </p>
              ) : (
                <div className="space-y-3">
                  {rows.map((r) => (
                    <QueueCard key={r.id} row={r} kinds={c.kinds} />
                  ))}
                </div>
              )}
            </section>
          );
        })}
      </div>
      <div className="mt-6">
        <Button variant="link" className="h-11 px-0 md:h-10" onClick={() => setDelivered(true)}>
          Teslim edilenler
        </Button>
      </div>
      {delivered && <DeliveredDialog open onOpenChange={setDelivered} />}
    </div>
  );
}
