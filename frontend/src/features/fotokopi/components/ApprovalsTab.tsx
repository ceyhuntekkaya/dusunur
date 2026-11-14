"use client";

import { Inbox } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { SegmentedControl } from "@/components/common/SegmentedControl";
import { useUrlParams } from "@/lib/url-state";
import { DelegationCard } from "./DelegationCard";
import { PagedRequestList } from "./PagedRequestList";
import { PrintActionButtons } from "./PrintActionButtons";
import { PrintRequestCard } from "./PrintRequestCard";

type Tab = "pending" | "decided";

export function ApprovalsTab() {
  const url = useUrlParams();
  const tab: Tab = url.get("onay") === "verdiklerim" ? "decided" : "pending";

  return (
    <div className="mx-auto max-w-[720px]">
      <DelegationCard />
      <SegmentedControl
        variant="chips"
        ariaLabel="Onay listesi"
        className="mb-4"
        value={tab}
        onChange={(v) => url.set({ onay: v === "decided" ? "verdiklerim" : undefined })}
        options={[
          { value: "pending", label: "Bekleyenler" },
          { value: "decided", label: "Karar Verdiklerim" },
        ]}
      />
      <PagedRequestList
        queryKey={["print", "approvals", tab]}
        path="/api/print/approvals"
        params={{ tab }}
        renderItem={(r) => (
          <PrintRequestCard
            request={r}
            context="person"
            actions={
              tab === "pending" ? (
                <PrintActionButtons
                  requestId={r.id}
                  ownerName={r.requester.fullName}
                  kinds={["approve", "reject"]}
                  compact
                />
              ) : undefined
            }
          />
        )}
        empty={
          <EmptyState
            icon={Inbox}
            message={tab === "pending" ? "Onayınızı bekleyen talep yok." : "Henüz karar verdiğiniz talep yok."}
          />
        }
      />
    </div>
  );
}
