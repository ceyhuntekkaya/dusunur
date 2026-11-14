"use client";

import Link from "next/link";
import { CheckCircle2 } from "lucide-react";
import { DateText } from "@/components/common/DateText";
import { Skeleton } from "@/components/ui/skeleton";
import { useUnreadImportant } from "../api";

export function UnreadImportantCard() {
  const q = useUnreadImportant();
  const items = q.data?.items.slice(0, 5) ?? [];

  return (
    <section className="rounded-xl border bg-card p-4" aria-label="Okumanız Gereken Duyurular">
      <h2 className="mb-3 text-sm font-semibold">Okumanız Gereken Duyurular</h2>
      {q.isPending ? (
        <div className="space-y-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
      ) : items.length === 0 ? (
        <p className="flex items-center gap-2 text-[13px] text-muted-foreground">
          <CheckCircle2 className="size-5 shrink-0 text-success" aria-hidden />
          Okunmamış önemli duyurunuz yok.
        </p>
      ) : (
        <ul className="space-y-1">
          {items.map((p) => (
            <li key={p.id}>
              <Link href={`/akis/${p.id}`} className="block rounded-lg px-2 py-2 hover:bg-muted">
                <p className="truncate text-[13px] font-medium">{p.author.fullName}</p>
                <p className="line-clamp-2 text-[13px] text-muted-foreground">
                  {p.content?.trim() || `${p.attachments.length} ek dosya`}
                </p>
                <DateText value={p.createdAt} className="text-xs text-muted-foreground" />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
