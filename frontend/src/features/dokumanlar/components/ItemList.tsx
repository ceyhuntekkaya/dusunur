"use client";

import { useEffect, useRef } from "react";
import { DateText } from "@/components/common/DateText";
import { RowMenu, type RowMenuItem } from "@/components/common/RowMenu";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { formatFileSize } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { FileRow, FolderRow } from "../types";
import { FOLDER_ICON, iconFor } from "../utils";

export type Entry =
  | { type: "folder"; row: FolderRow }
  | { type: "file"; row: FileRow };

type Props = {
  entries: Entry[];
  highlightId?: number;
  onOpen: (e: Entry) => void;
  menuItems: (e: Entry) => RowMenuItem[];
};

function entryView(e: Entry) {
  const { Icon, className } = e.type === "folder" ? FOLDER_ICON : iconFor(e.row.name, e.row.contentType);
  const size = e.type === "folder" ? `${e.row.itemCount} öğe` : formatFileSize(e.row.sizeBytes);
  const by = e.type === "folder" ? e.row.createdBy.fullName : e.row.updatedBy.fullName;
  return { Icon, className, size, by };
}

function Desktop({ entry, highlight, onOpen, menu }: { entry: Entry; highlight: boolean; onOpen: () => void; menu: RowMenuItem[] }) {
  const { Icon, className, size, by } = entryView(entry);
  const ref = useRef<HTMLTableRowElement>(null);
  useEffect(() => {
    if (highlight) ref.current?.scrollIntoView({ block: "center", behavior: "smooth" });
  }, [highlight]);
  return (
    <TableRow ref={ref} className={cn("transition-colors duration-500", highlight && "bg-accent")}>
      <TableCell>
        <button
          type="button"
          onClick={onOpen}
          className="flex min-h-10 w-full items-center gap-3 rounded-md text-left outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        >
          <Icon className={cn("size-5 shrink-0", className)} aria-hidden />
          <span className="truncate">{entry.row.name}</span>
        </button>
      </TableCell>
      <TableCell className="text-muted-foreground">{size}</TableCell>
      <TableCell className="text-muted-foreground">{by}</TableCell>
      <TableCell className="text-muted-foreground">
        <DateText value={entry.row.updatedAt} />
      </TableCell>
      <TableCell className="w-12">
        <RowMenu items={menu} />
      </TableCell>
    </TableRow>
  );
}

function Mobile({ entry, highlight, onOpen, menu }: { entry: Entry; highlight: boolean; onOpen: () => void; menu: RowMenuItem[] }) {
  const { Icon, className, size } = entryView(entry);
  const ref = useRef<HTMLLIElement>(null);
  useEffect(() => {
    if (highlight) ref.current?.scrollIntoView({ block: "center", behavior: "smooth" });
  }, [highlight]);
  return (
    <li ref={ref} className={cn("flex items-center transition-colors duration-500", highlight && "bg-accent")}>
      <button
        type="button"
        onClick={onOpen}
        className="flex min-h-14 min-w-0 flex-1 items-center gap-3 px-3 text-left outline-none focus-visible:bg-muted"
      >
        <Icon className={cn("size-6 shrink-0", className)} aria-hidden />
        <span className="min-w-0">
          <span className="block truncate text-[15px]">{entry.row.name}</span>
          <span className="block truncate text-xs text-muted-foreground">
            {size} · <DateText value={entry.row.updatedAt} />
          </span>
        </span>
      </button>
      <RowMenu items={menu} />
    </li>
  );
}

export function ItemList({ entries, highlightId, onOpen, menuItems }: Props) {
  const isHl = (e: Entry) => e.type === "file" && e.row.id === highlightId;
  const key = (e: Entry) => `${e.type}-${e.row.id}`;
  return (
    <>
      <div className="hidden rounded-xl border bg-card md:block">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Ad</TableHead>
              <TableHead>Boyut</TableHead>
              <TableHead>Son değiştiren</TableHead>
              <TableHead>Değiştirilme</TableHead>
              <TableHead className="w-12">
                <span className="sr-only">İşlemler</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {entries.map((e) => (
              <Desktop key={key(e)} entry={e} highlight={isHl(e)} onOpen={() => onOpen(e)} menu={menuItems(e)} />
            ))}
          </TableBody>
        </Table>
      </div>
      <ul className="divide-y rounded-xl border bg-card md:hidden">
        {entries.map((e) => (
          <Mobile key={key(e)} entry={e} highlight={isHl(e)} onOpen={() => onOpen(e)} menu={menuItems(e)} />
        ))}
      </ul>
    </>
  );
}
