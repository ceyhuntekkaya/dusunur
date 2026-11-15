"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { ArrowLeft, SearchX } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { PageSkeleton } from "@/components/common/Skeletons";
import { usePrintContext, usePrintRequest } from "@/features/fotokopi/api";
import { PrintRequestForm } from "@/features/fotokopi/components/PrintRequestForm";
import { ApiError } from "@/lib/api";

export default function NewPrintRequestPage() {
  const kopya = Number(useSearchParams().get("kopya"));
  const copyId = Number.isInteger(kopya) && kopya > 0 ? kopya : 0;
  const context = usePrintContext();
  const source = usePrintRequest(copyId);

  let body: React.ReactNode;
  if (context.isPending || (copyId > 0 && source.isPending)) {
    body = <PageSkeleton />;
  } else if (context.isError) {
    body = <ErrorState error={context.error} onRetry={() => void context.refetch()} />;
  } else if (copyId > 0 && source.isError) {
    body =
      source.error instanceof ApiError && source.error.status === 404 ? (
        <EmptyState
          icon={SearchX}
          message="Kopyalanacak talep bulunamadı veya görüntüleme yetkiniz yok."
          action={{ label: "Fotokopi'ye dön", href: "/fotokopi" }}
        />
      ) : (
        <ErrorState error={source.error} onRetry={() => void source.refetch()} />
      );
  } else if (copyId > 0 && !source.data?.actions.canCopy) {
    body = (
      <EmptyState
        icon={SearchX}
        message="Bu talep kopyalanamaz."
        action={{ label: "Fotokopi'ye dön", href: "/fotokopi" }}
      />
    );
  } else {
    body = (
      <div className="md:rounded-xl md:border md:bg-card md:p-6">
        <h1 className="mb-4 text-xl font-semibold md:text-[22px] md:leading-7">Yeni Fotokopi Talebi</h1>
        <PrintRequestForm ctx={context.data!} source={copyId > 0 ? source.data : undefined} />
      </div>
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
