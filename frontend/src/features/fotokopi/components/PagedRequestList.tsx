"use client";

import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ErrorState } from "@/components/common/ErrorState";
import { ListSkeleton } from "@/components/common/Skeletons";
import type { QueryKey } from "@tanstack/react-query";
import { usePagedRequests } from "../api";
import type { PrintRequestRow } from "../types";

type Props = {
  queryKey: QueryKey;
  path: string;
  params: Record<string, string | undefined>;
  renderItem: (row: PrintRequestRow) => React.ReactNode;
  empty: React.ReactNode;
};

export function PagedRequestList({ queryKey, path, params, renderItem, empty }: Props) {
  const query = usePagedRequests<PrintRequestRow>(queryKey, path, params);

  if (query.isPending) return <ListSkeleton />;
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />;

  const rows = query.data.pages.flatMap((p) => p.items);
  if (rows.length === 0) return <>{empty}</>;

  return (
    <div className="space-y-3">
      {rows.map((r) => (
        <div key={r.id}>{renderItem(r)}</div>
      ))}
      {query.hasNextPage && (
        <div className="flex justify-center pt-1">
          <Button
            variant="outline"
            className="h-11 md:h-10"
            disabled={query.isFetchingNextPage}
            onClick={() => void query.fetchNextPage()}
          >
            {query.isFetchingNextPage && <Loader2 className="animate-spin" aria-hidden />}
            Daha fazla göster
          </Button>
        </div>
      )}
    </div>
  );
}
