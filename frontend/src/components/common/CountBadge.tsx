import { formatBadgeCount } from "@/lib/format";
import { cn } from "@/lib/utils";

export function CountBadge({ count, className }: { count: number; className?: string }) {
  if (count <= 0) return null;
  return (
    <span
      className={cn(
        "inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-destructive px-1.5 text-[11px] font-semibold text-white",
        className,
      )}
      aria-label={`${count} yeni`}
    >
      {formatBadgeCount(count)}
    </span>
  );
}
