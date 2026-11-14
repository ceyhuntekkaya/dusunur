import Link from "next/link";
import { Folder } from "lucide-react";
import { DateText } from "@/components/common/DateText";
import { Badge } from "@/components/ui/badge";
import { formatFileSize } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { SpaceRow } from "../types";
import { LEVEL_BADGE, LEVEL_LABEL, spaceColor } from "../utils";

export function SpaceCard({ space, isOwner }: { space: SpaceRow; isOwner: boolean }) {
  return (
    <Link
      href={`/dokumanlar/${space.id}`}
      className="relative flex min-h-11 flex-col gap-3 rounded-xl border bg-card p-4 outline-none transition-colors hover:bg-muted/40 focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex size-11 items-center justify-center rounded-lg" style={spaceColor(space.id)}>
          <Folder className="size-6" aria-hidden />
        </div>
        <Badge className={cn(LEVEL_BADGE[space.myLevel])}>
          {isOwner ? "Sahibi" : LEVEL_LABEL[space.myLevel]}
        </Badge>
      </div>
      <div className="min-w-0 space-y-1">
        <h2 className="truncate font-semibold">{space.name}</h2>
        {space.description && (
          <p className="line-clamp-2 text-[13px] text-muted-foreground">{space.description}</p>
        )}
      </div>
      <p className="mt-auto text-xs text-muted-foreground">
        {space.fileCount} dosya · {formatFileSize(space.totalBytes)}
        {space.lastUpdate && (
          <>
            {" · Son güncelleme "}
            <DateText value={space.lastUpdate} />
          </>
        )}
      </p>
    </Link>
  );
}
