"use client";

import { useEffect, useRef } from "react";
import { useInfiniteQuery, type QueryKey } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ErrorState } from "@/components/common/ErrorState";
import { ListSkeleton } from "@/components/common/Skeletons";
import { api, type CursorResponse } from "@/lib/api";

type Props<T> = {
  queryKey: QueryKey;
  path: string;
  params?: Record<string, string | number | undefined>;
  size?: number;
  getKey: (item: T) => string | number;
  renderItem: (item: T) => React.ReactNode;
  empty: React.ReactNode;
};

export function InfiniteList<T>({
  queryKey,
  path,
  params,
  size = 20,
  getKey,
  renderItem,
  empty,
}: Props<T>) {
  const query = useInfiniteQuery({
    queryKey,
    initialPageParam: undefined as number | undefined,
    queryFn: ({ pageParam }) => {
      const qs = new URLSearchParams();
      for (const [k, v] of Object.entries(params ?? {})) {
        if (v !== undefined) qs.set(k, String(v));
      }
      if (pageParam !== undefined) qs.set("before", String(pageParam));
      qs.set("size", String(size));
      return api<CursorResponse<T>>(`${path}?${qs.toString()}`);
    },
    getNextPageParam: (last) => last.nextCursor ?? undefined,
  });

  const { hasNextPage, isFetchingNextPage, fetchNextPage } = query;
  const sentinel = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = sentinel.current;
    if (!el || !hasNextPage) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && !isFetchingNextPage) void fetchNextPage();
      },
      { rootMargin: "200px" },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  if (query.isPending) return <ListSkeleton />;
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />;

  const items = query.data.pages.flatMap((p) => p.items);
  if (items.length === 0) return <>{empty}</>;

  return (
    <div className="space-y-3">
      {items.map((item) => (
        <div key={getKey(item)}>{renderItem(item)}</div>
      ))}
      {hasNextPage && (
        <div ref={sentinel} className="flex justify-center py-2">
          <Button
            variant="outline"
            className="h-11 md:h-10"
            disabled={isFetchingNextPage}
            onClick={() => void fetchNextPage()}
          >
            {isFetchingNextPage && <Loader2 className="animate-spin" aria-hidden />}
            Daha fazla
          </Button>
        </div>
      )}
    </div>
  );
}
