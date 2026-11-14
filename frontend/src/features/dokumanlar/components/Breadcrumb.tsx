"use client";

import Link from "next/link";
import { ChevronRight } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useIsDesktop } from "@/lib/media";

export type Crumb = { label: string; href: string };

export function Breadcrumb({ crumbs }: { crumbs: Crumb[] }) {
  const desktop = useIsDesktop();
  const hidden = desktop ? [] : crumbs.slice(0, Math.max(0, crumbs.length - 2));
  const shown = crumbs.slice(hidden.length);
  const link = "inline-flex min-h-11 items-center rounded-md px-1 text-sm outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50 md:min-h-9";

  return (
    <nav aria-label="Konum" className="flex flex-wrap items-center gap-x-1 text-muted-foreground">
      {hidden.length > 0 && (
        <>
          <DropdownMenu>
            <DropdownMenuTrigger aria-label="Üst klasörler" className={`${link} min-w-11 justify-center`}>
              …
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="min-w-48">
              {hidden.map((c) => (
                <DropdownMenuItem key={c.href} className="min-h-11" render={<Link href={c.href} />}>
                  {c.label}
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
          <ChevronRight className="size-4" aria-hidden />
        </>
      )}
      {shown.map((c, i) => {
        const last = i === shown.length - 1;
        return (
          <span key={c.href} className="flex min-w-0 items-center gap-1">
            {last ? (
              <span aria-current="page" className="max-w-[60vw] truncate px-1 text-sm font-semibold text-foreground">
                {c.label}
              </span>
            ) : (
              <Link href={c.href} className={`${link} max-w-[40vw] truncate`}>
                {c.label}
              </Link>
            )}
            {!last && <ChevronRight className="size-4 shrink-0" aria-hidden />}
          </span>
        );
      })}
    </nav>
  );
}
