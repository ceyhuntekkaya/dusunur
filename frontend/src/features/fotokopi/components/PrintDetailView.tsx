"use client";

import { Download, Eye, FileText, Lock } from "lucide-react";
import { DateText } from "@/components/common/DateText";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { formatFileSize } from "@/lib/format";
import { cn } from "@/lib/utils";
import { printFileUrl } from "../api";
import { COLOR_LABEL, PAPER_LABEL, SIDES_LABEL, STATUS_LABEL, STATUS_TONE, formatDue } from "../labels";
import type { PrintRequestDetail } from "../types";
import { PrintActionButtons, type ActionButtonKind } from "./PrintActionButtons";
import { PrintStepper } from "./PrintStepper";

function Info({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="min-w-0">
      <dt className="text-[13px] text-muted-foreground">{label}</dt>
      <dd className="text-[15px] break-words">{children}</dd>
    </div>
  );
}

function ClosedBox({ r }: { r: PrintRequestDetail }) {
  const rejected = r.status === "REJECTED";
  const cancelEntry = [...r.history].reverse().find((h) => h.toStatus === "CANCELLED");
  const who = rejected ? r.decidedBy?.fullName : (cancelEntry?.actor?.fullName ?? "Sistem");
  const when = rejected ? r.decidedAt : r.cancelledAt;
  const reason = rejected ? r.rejectReason : r.cancelReason;
  return (
    <div
      className={cn(
        "rounded-lg px-4 py-3 text-sm",
        rejected ? "bg-destructive/10 text-destructive" : "bg-muted text-muted-foreground",
      )}
    >
      <p className="font-semibold">{STATUS_LABEL[r.status]}</p>
      {reason && <p className="mt-1">{rejected ? `Gerekçe: ${reason}` : reason}</p>}
      <p className="mt-1">
        {who}
        {when && (
          <>
            {" · "}
            <DateText value={when} />
          </>
        )}
      </p>
    </div>
  );
}

function FileCard({ r }: { r: PrintRequestDetail }) {
  let content: React.ReactNode;
  if (r.file?.purged) {
    content = <p className="text-sm text-muted-foreground">Dosya saklama süresi dolduğu için silindi.</p>;
  } else if (r.file) {
    const previewable = r.file.contentType === "application/pdf" || r.file.contentType.startsWith("image/");
    content = (
      <div className="flex flex-wrap items-center gap-3">
        <FileText className="size-5 shrink-0 text-muted-foreground" aria-hidden />
        <div className="min-w-0 flex-1">
          <p className="truncate text-[15px]">{r.file.name}</p>
          <p className="text-[13px] text-muted-foreground">{formatFileSize(r.file.sizeBytes)}</p>
        </div>
        {r.actions.canDownload && (
          <div className="flex gap-2">
            {previewable && (
              <Button
                variant="outline"
                className="h-11 md:h-10"
                nativeButton={false}
                render={<a href={printFileUrl(r.id, false)} target="_blank" rel="noreferrer" />}
              >
                <Eye aria-hidden />
                Önizle
              </Button>
            )}
            <Button
              variant="outline"
              className="h-11 md:h-10"
              nativeButton={false}
              render={<a href={printFileUrl(r.id, true)} target="_blank" rel="noreferrer" />}
            >
              <Download aria-hidden />
              İndir
            </Button>
          </div>
        )}
      </div>
    );
  } else {
    content = <p className="text-sm text-muted-foreground">Basılı orijinal fotokopi birimine getirilecek.</p>;
  }
  return <section className="rounded-xl border bg-card p-4">{content}</section>;
}

export function PrintDetailView({ r }: { r: PrintRequestDetail }) {
  const a = r.actions;
  const kinds: ActionButtonKind[] = [];
  if (a.canApprove) kinds.push("approve");
  if (a.canReject) kinds.push("reject");
  if (a.canStart) kinds.push("start");
  if (a.canMarkReady) kinds.push("ready");
  if (a.canDeliver) kinds.push("deliver");
  if (a.canCancel) kinds.push("cancel");
  if (a.canCopy) kinds.push("copy");

  const closed = r.status === "REJECTED" || r.status === "CANCELLED";
  const vekil = r.originalApprover && r.approver;

  return (
    <div className={cn("space-y-4", kinds.length > 0 && "pb-24 md:pb-0")}>
      <div className="flex items-start justify-between gap-3">
        <h1 className="min-w-0 text-xl font-semibold md:text-[22px] md:leading-7">
          {r.secret && <Lock className="mr-1.5 inline size-5 align-[-2px]" aria-label="Gizli evrak" />}
          {r.title}
        </h1>
        <StatusBadge tone={STATUS_TONE[r.status]} className="shrink-0">
          {STATUS_LABEL[r.status]}
        </StatusBadge>
      </div>

      <section className="rounded-xl border bg-card p-4">
        {closed ? <ClosedBox r={r} /> : <PrintStepper status={r.status} auto={!r.approver && !r.decidedBy} />}
      </section>

      <section className="rounded-xl border bg-card p-4">
        <dl className="grid gap-4 sm:grid-cols-2">
          <Info label="Talep sahibi">{r.requester.fullName}</Info>
          <Info label="Şube">{r.branchName}</Info>
          <Info label="Sayfa / kopya / toplam baskı">
            {r.pageCount} sayfa × {r.copies} kopya = {r.totalPages.toLocaleString("tr-TR")} baskı
          </Info>
          <Info label="Renk · yüz · kâğıt">
            {COLOR_LABEL[r.color]} · {SIDES_LABEL[r.sides]} · {PAPER_LABEL[r.paperSize]}
          </Info>
          <Info label="Teslim zamanı">{formatDue(r.dueAt)}</Info>
          <Info label="Onaylayıcı">
            {r.approver ? (
              <>
                {r.approver.fullName}
                {vekil && ` (vekil — asıl: ${r.originalApprover!.fullName})`}
              </>
            ) : (
              "Onaysız"
            )}
          </Info>
          {r.operator && <Info label="Fotokopi sorumlusu">{r.operator.fullName}</Info>}
          {r.note && <Info label="Not">{r.note}</Info>}
          {r.secret && <Info label="Gizli">Evet</Info>}
        </dl>
      </section>

      <FileCard r={r} />

      <section className="rounded-xl border bg-card p-4">
        <h2 className="mb-3 text-[15px] font-semibold">Geçmiş</h2>
        <ol className="relative space-y-4 border-l pl-5">
          {r.history.map((h, i) => (
            <li key={i} className="relative">
              <span aria-hidden className="absolute top-1.5 -left-[25px] size-2.5 rounded-full bg-primary" />
              <p className="text-[13px] text-muted-foreground">
                <DateText value={h.createdAt} /> · {h.actor?.fullName ?? "Sistem"}
              </p>
              <p className="text-[15px]">
                {h.fromStatus ? `${STATUS_LABEL[h.fromStatus]} → ` : ""}
                {STATUS_LABEL[h.toStatus]}
              </p>
              {h.note && <p className="text-[13px] text-muted-foreground">{h.note}</p>}
            </li>
          ))}
        </ol>
      </section>

      {kinds.length > 0 && (
        <div className="fixed inset-x-0 bottom-[calc(64px+env(safe-area-inset-bottom))] z-30 border-t bg-card p-3 md:static md:border-0 md:bg-transparent md:p-0">
          <PrintActionButtons requestId={r.id} ownerName={r.requester.fullName} kinds={kinds} />
        </div>
      )}
    </div>
  );
}
