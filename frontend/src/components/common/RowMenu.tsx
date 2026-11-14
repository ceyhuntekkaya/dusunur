"use client";

import { MoreHorizontal } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";

export type RowMenuItem = {
  label: string;
  onSelect: () => void;
  destructive?: boolean;
  hidden?: boolean;
};

export function RowMenu({ items, label = "İşlemler" }: { items: RowMenuItem[]; label?: string }) {
  const visible = items.filter((i) => !i.hidden);
  if (visible.length === 0) return null;
  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        aria-label={label}
        onClick={(e) => e.stopPropagation()}
        className="flex size-11 items-center justify-center rounded-lg outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 md:size-10"
      >
        <MoreHorizontal className="size-5" aria-hidden />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="min-w-48">
        {visible.map((i) => (
          <DropdownMenuItem
            key={i.label}
            className={cn("min-h-11", i.destructive && "text-destructive focus:text-destructive")}
            onClick={(e) => {
              e.stopPropagation();
              i.onSelect();
            }}
          >
            {i.label}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
