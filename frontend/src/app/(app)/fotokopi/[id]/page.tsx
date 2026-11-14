"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowLeft, SearchX } from "lucide-react";
import { ArchiveBanner } from "@/components/common/ArchiveBanner";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { ListSkeleton } from "@/components/common/Skeletons";
import { usePrintLiveRefresh, usePrintRequest, usePrintSeasons } from "@/features/fotokopi/api";
import { PrintDetailView } from "@/features/fotokopi/components/PrintDetailView";
import { ApiError } from "@/lib/api";

const NOT_FOUND = "Fotokopi talebi bulunamadı veya görüntüleme yetkiniz yok.";

export default function PrintDetailPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const valid = Number.isInteger(id) && id > 0;
  const request = usePrintRequest(valid ? id : 0);
  const seasons = usePrintSeasons();
  usePrintLiveRefresh();

  let body: React.ReactNode;
  if (!valid || (request.isError && request.error instanceof ApiError && request.error.status === 404)) {
    body = <EmptyState icon={SearchX} message={NOT_FOUND} action={{ label: "Fotokopi'ye dön", href: "/fotokopi" }} />;
  } else if (request.isPending) {
    body = <ListSkeleton rows={3} />;
  } else if (request.isError) {
    body = <ErrorState error={request.error} onRetry={() => void request.refetch()} />;
  } else {
    const r = request.data;
    body = (
      <>
        {r.archived && <ArchiveBanner seasonName={seasons.data?.find((s) => s.id === r.seasonId)?.name ?? ""} />}
        <PrintDetailView r={r} />
      </>
    );
  }

  return (
    <div className="mx-auto max-w-[720px]">
      <Link
        href="/fotokopi"
        className="mb-2 inline-flex h-11 items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground md:h-10"
      >
        <ArrowLeft className="size-4" aria-hidden />
        Fotokopi
      </Link>
      {body}
    </div>
  );
}
