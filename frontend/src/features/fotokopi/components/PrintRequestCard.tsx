import Link from "next/link";
import { CalendarClock, Lock } from "lucide-react";
import { StatusBadge } from "@/components/common/StatusBadge";
import { UserAvatar } from "@/components/common/UserAvatar";
import { cn } from "@/lib/utils";
import { dueState, formatDue, specLine, STATUS_LABEL, STATUS_TONE } from "../labels";
import type { PrintRequestRow } from "../types";

type Props = {
  request: PrintRequestRow;
  context: "mine" | "person";
  actions?: React.ReactNode;
};

export function PrintRequestCard({ request: r, context, actions }: Props) {
  const due = dueState(r);
  return (
    <article className="relative rounded-xl border bg-card p-4 shadow-sm transition-colors hover:bg-accent/40">
      <div className="flex items-start justify-between gap-3">
        <h3 className="min-w-0 text-[15px] font-semibold">
          <Link
            href={`/fotokopi/${r.id}`}
            className="line-clamp-2 outline-none after:absolute after:inset-0 after:rounded-xl focus-visible:after:ring-3 focus-visible:after:ring-ring/50"
          >
            {r.secret && <Lock className="mr-1 inline size-4 align-[-2px]" aria-label="Gizli evrak" />}
            {r.title}
          </Link>
        </h3>
        <StatusBadge tone={STATUS_TONE[r.status]} className="shrink-0">
          {STATUS_LABEL[r.status]}
        </StatusBadge>
      </div>
      <p className="mt-1 text-[13px] text-muted-foreground">{specLine(r)}</p>
      <p
        className={cn(
          "mt-1 flex items-center gap-1.5 text-[13px]",
          due === "overdue" && "text-destructive",
          due === "today" && "text-warning",
          due === "normal" && "text-muted-foreground",
        )}
      >
        <CalendarClock className="size-4 shrink-0" aria-hidden />
        <span>
          {due === "overdue" && "Gecikti · "}
          {due === "today" && "Bugün · "}Teslim: {formatDue(r.dueAt)}
        </span>
      </p>
      {context === "mine" ? (
        <p className="mt-1 text-[13px] text-muted-foreground">
          {r.approver ? `Onaylayıcı: ${r.approver.fullName}` : "Onaysız"}
        </p>
      ) : (
        <div className="mt-2 flex items-center gap-2 text-[13px]">
          <UserAvatar userId={r.requester.id} name={r.requester.fullName} size="sm" />
          <span className="truncate">
            {r.requester.fullName}
            <span className="text-muted-foreground"> · {r.requester.branchName ?? r.branchName}</span>
          </span>
        </div>
      )}
      {actions && <div className="relative z-10 mt-3">{actions}</div>}
    </article>
  );
}
