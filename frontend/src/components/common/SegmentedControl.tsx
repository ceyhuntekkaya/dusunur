"use client";

import { CountBadge } from "@/components/common/CountBadge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export type SegmentOption<T extends string> = { value: T; label: string; count?: number };

type Props<T extends string> = {
  value: T;
  onChange: (value: T) => void;
  options: SegmentOption<T>[];
  ariaLabel: string;
  kind?: "tabs" | "radio";
  variant?: "segmented" | "chips";
  className?: string;
};

export function SegmentedControl<T extends string>({
  value,
  onChange,
  options,
  ariaLabel,
  kind = "radio",
  variant = "segmented",
  className,
}: Props<T>) {
  const itemRole = kind === "tabs" ? "tab" : "radio";
  return (
    <div
      role={kind === "tabs" ? "tablist" : "radiogroup"}
      aria-label={ariaLabel}
      className={cn(
        variant === "segmented" ? "flex gap-1 rounded-lg bg-muted p-1" : "-mx-1 flex gap-2 overflow-x-auto px-1 py-1",
        className,
      )}
    >
      {options.map((o) => {
        const active = o.value === value;
        return (
          <Button
            key={o.value}
            type="button"
            role={itemRole}
            aria-selected={kind === "tabs" ? active : undefined}
            aria-checked={kind === "radio" ? active : undefined}
            variant={variant === "chips" ? (active ? "default" : "outline") : active ? "outline" : "ghost"}
            className={cn(
              "h-11 md:h-10",
              variant === "chips" ? "shrink-0 rounded-full px-4" : "min-w-0 flex-1 px-3",
              variant === "segmented" && active && "bg-card shadow-sm",
            )}
            onClick={() => onChange(o.value)}
          >
            <span className="truncate">{o.label}</span>
            {o.count !== undefined && <CountBadge count={o.count} />}
          </Button>
        );
      })}
    </div>
  );
}
