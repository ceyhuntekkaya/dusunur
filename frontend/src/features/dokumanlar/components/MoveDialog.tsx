"use client";

import { useMemo, useState } from "react";
import { Folder, FolderOpen, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { ErrorState } from "@/components/common/ErrorState";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useFolderNodes, useMoveItem } from "../api";
import type { FolderNode } from "../types";

export type MoveTarget = { kind: "folder" | "file"; id: number; name: string };

type Row = { node: FolderNode; blocked: boolean; level: number };

function flatten(nodes: FolderNode[], excludedId?: number): Row[] {
  const byParent = new Map<number | undefined, FolderNode[]>();
  for (const n of nodes) {
    const list = byParent.get(n.parentId) ?? [];
    list.push(n);
    byParent.set(n.parentId, list);
  }
  const out: Row[] = [];
  const walk = (parent: number | undefined, depth: number, blocked: boolean) => {
    for (const n of byParent.get(parent) ?? []) {
      const isBlocked = blocked || n.id === excludedId;
      out.push({ node: n, blocked: isBlocked, level: depth });
      walk(n.id, depth + 1, isBlocked);
    }
  };
  walk(undefined, 0, false);
  return out;
}

function Body({ spaceId, target, onClose }: { spaceId: number; target: MoveTarget; onClose: () => void }) {
  const nodes = useFolderNodes(spaceId, true);
  const move = useMoveItem();
  const [selected, setSelected] = useState<number | null | undefined>(undefined);
  const rows = useMemo(
    () => flatten(nodes.data ?? [], target.kind === "folder" ? target.id : undefined),
    [nodes.data, target],
  );

  if (nodes.isPending)
    return (
      <div className="flex justify-center py-8">
        <Loader2 className="size-5 animate-spin text-muted-foreground" aria-label="Yükleniyor" />
      </div>
    );
  if (nodes.isError) return <ErrorState error={nodes.error} onRetry={() => void nodes.refetch()} />;

  async function submit() {
    if (selected === undefined) return;
    await move.mutateAsync({ kind: target.kind, id: target.id, targetFolderId: selected ?? undefined });
    toast.success("Taşındı.");
    onClose();
  }

  const optionClass = (active: boolean, disabled = false) =>
    cn(
      "flex min-h-11 w-full items-center gap-2 rounded-lg px-3 text-left text-[15px] outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
      active ? "bg-accent font-medium" : "hover:bg-muted",
      disabled && "pointer-events-none opacity-40",
    );

  return (
    <div className="space-y-4">
      <p className="text-[13px] text-muted-foreground">&apos;{target.name}&apos; için hedef klasörü seçin.</p>
      <div role="radiogroup" aria-label="Hedef klasör" className="max-h-[50dvh] space-y-0.5 overflow-y-auto rounded-lg border p-1">
        <button
          type="button"
          role="radio"
          aria-checked={selected === null}
          className={optionClass(selected === null)}
          onClick={() => setSelected(null)}
        >
          <FolderOpen className="size-4 text-yellow-500" aria-hidden />
          Ana dizin
        </button>
        {rows.map(({ node: n, blocked, level }) => {
          return (
            <button
              key={n.id}
              type="button"
              role="radio"
              aria-checked={selected === n.id}
              aria-disabled={blocked}
              tabIndex={blocked ? -1 : 0}
              className={optionClass(selected === n.id, blocked)}
              style={{ paddingLeft: `${12 + level * 16}px` }}
              onClick={() => setSelected(n.id)}
            >
              <Folder className="size-4 shrink-0 text-yellow-500" aria-hidden />
              <span className="truncate">{n.name}</span>
            </button>
          );
        })}
      </div>
      <div className="flex flex-col-reverse gap-2 md:flex-row md:justify-end">
        <Button variant="outline" className="h-11 md:h-10" disabled={move.isPending} onClick={onClose}>
          Vazgeç
        </Button>
        <Button className="h-11 md:h-10" disabled={selected === undefined || move.isPending} onClick={() => void submit().catch(() => {})}>
          {move.isPending && <Loader2 className="animate-spin" aria-hidden />}
          Buraya Taşı
        </Button>
      </div>
    </div>
  );
}

export function MoveDialog({
  spaceId,
  target,
  onClose,
}: {
  spaceId: number;
  target: MoveTarget | null;
  onClose: () => void;
}) {
  return (
    <ResponsiveDialog open={target !== null} onOpenChange={(o) => !o && onClose()} title="Taşı">
      {target && <Body spaceId={spaceId} target={target} onClose={onClose} />}
    </ResponsiveDialog>
  );
}
