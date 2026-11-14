"use client";

import Link from "next/link";
import { SearchX } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { ListSkeleton } from "@/components/common/Skeletons";
import { formatFileSize } from "@/lib/format";
import { useDmsSearch } from "../api";
import type { SearchResult } from "../types";
import { FOLDER_ICON, iconFor } from "../utils";
import { Highlight } from "./Highlight";

function hrefOf(r: SearchResult): string {
  const base = `/dokumanlar/${r.spaceId}`;
  if (r.type === "FOLDER") return `${base}?klasor=${r.id}`;
  const params = new URLSearchParams();
  if (r.folderId !== undefined) params.set("klasor", String(r.folderId));
  params.set("vurgu", String(r.id));
  return `${base}?${params.toString()}`;
}

function Section({ title, rows, q }: { title: string; rows: SearchResult[]; q: string }) {
  if (rows.length === 0) return null;
  return (
    <section className="space-y-2">
      <h2 className="text-sm font-semibold text-muted-foreground">{title}</h2>
      <ul className="divide-y rounded-xl border bg-card">
        {rows.map((r) => {
          const { Icon, className } = r.type === "FOLDER" ? FOLDER_ICON : iconFor(r.name, r.contentType);
          return (
            <li key={`${r.type}-${r.id}`}>
              <Link
                href={hrefOf(r)}
                className="flex min-h-14 items-center gap-3 p-3 outline-none hover:bg-muted/40 focus-visible:bg-muted/40"
              >
                <Icon className={`size-5 shrink-0 ${className}`} aria-hidden />
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-[15px]">
                    <Highlight text={r.name} query={q} />
                  </span>
                  <span className="block truncate text-xs text-muted-foreground">{r.path}</span>
                </span>
                {r.sizeBytes !== undefined && (
                  <span className="shrink-0 text-xs text-muted-foreground">{formatFileSize(r.sizeBytes)}</span>
                )}
              </Link>
            </li>
          );
        })}
      </ul>
    </section>
  );
}

export function SearchResults({ q }: { q: string }) {
  const search = useDmsSearch(q);
  if (search.isPending) return <ListSkeleton rows={4} />;
  if (search.isError) return <ErrorState error={search.error} onRetry={() => void search.refetch()} />;
  const rows = search.data;
  if (rows.length === 0)
    return <EmptyState icon={SearchX} message={`'${q}' ile eşleşen dosya veya klasör bulunamadı.`} />;
  return (
    <div className="space-y-5">
      <Section title="Klasörler" rows={rows.filter((r) => r.type === "FOLDER")} q={q} />
      <Section title="Dosyalar" rows={rows.filter((r) => r.type === "FILE")} q={q} />
    </div>
  );
}
