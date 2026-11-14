"use client";

import { PageSkeleton } from "@/components/common/Skeletons";
import { ErrorState } from "@/components/common/ErrorState";
import { PageHeader } from "@/components/common/PageHeader";
import { SegmentedControl } from "@/components/common/SegmentedControl";
import { useBadges } from "@/features/core/api";
import { usePrintContext, usePrintLiveRefresh } from "@/features/fotokopi/api";
import { ApprovalsTab } from "@/features/fotokopi/components/ApprovalsTab";
import { MyRequestsTab } from "@/features/fotokopi/components/MyRequestsTab";
import { QueueTab } from "@/features/fotokopi/components/QueueTab";
import { ReportTab } from "@/features/fotokopi/components/ReportTab";
import { useUrlParams } from "@/lib/url-state";

type Tab = "taleplerim" | "onay" | "kuyruk" | "rapor";

export default function PrintPage() {
  const url = useUrlParams();
  const context = usePrintContext();
  const badges = useBadges(true);
  usePrintLiveRefresh();

  if (context.isPending) return <PageSkeleton />;
  if (context.isError) return <ErrorState error={context.error} onRetry={() => void context.refetch()} />;

  const ctx = context.data;
  const approvals = badges.data?.printApprovals ?? 0;
  const queue = badges.data?.printQueue ?? 0;

  const options: { value: Tab; label: string; count?: number }[] = [{ value: "taleplerim", label: "Taleplerim" }];
  if (ctx.isApprover) options.push({ value: "onay", label: "Onay Bekleyenler", count: approvals });
  if (ctx.isOperator) options.push({ value: "kuyruk", label: "Fotokopi Kuyruğu", count: queue });
  if (ctx.isManager) options.push({ value: "rapor", label: "Rapor" });

  const requested = url.get("sekme");
  const fallback: Tab = ctx.isOperator ? "kuyruk" : ctx.isApprover && approvals > 0 ? "onay" : "taleplerim";
  const tab = options.find((o) => o.value === requested)?.value ?? fallback;

  return (
    <div>
      <PageHeader title="Fotokopi" primaryAction={{ label: "Yeni Talep", href: "/fotokopi/yeni" }} />
      {options.length > 1 && (
        <SegmentedControl
          kind="tabs"
          ariaLabel="Fotokopi bölümleri"
          className="mb-4 max-w-2xl overflow-x-auto"
          value={tab}
          onChange={(v) => url.set({ sekme: v, yil: undefined, grup: undefined, onay: undefined })}
          options={options}
        />
      )}
      {tab === "taleplerim" && <MyRequestsTab />}
      {tab === "onay" && <ApprovalsTab />}
      {tab === "kuyruk" && <QueueTab />}
      {tab === "rapor" && <ReportTab />}
    </div>
  );
}
