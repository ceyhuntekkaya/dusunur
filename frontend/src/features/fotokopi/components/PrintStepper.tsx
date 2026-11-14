import { Check } from "lucide-react";
import { cn } from "@/lib/utils";
import type { PrintStatus } from "../types";

const STEPS = ["Gönderildi", "Onaylandı", "Hazırlanıyor", "Hazır", "Teslim Edildi"];
const INDEX: Partial<Record<PrintStatus, number>> = {
  PENDING_APPROVAL: 0,
  APPROVED: 1,
  IN_PROGRESS: 2,
  READY: 3,
  DELIVERED: 4,
};

export function PrintStepper({ status, auto }: { status: PrintStatus; auto: boolean }) {
  const current = INDEX[status] ?? 0;
  const finished = status === "DELIVERED";
  return (
    <ol className="flex items-start" aria-label="Talep adımları">
      {STEPS.map((label, i) => {
        const done = i < current || (finished && i === current);
        const active = i === current && !finished;
        return (
          <li key={label} className="relative flex min-w-0 flex-1 flex-col items-center gap-1 text-center">
            {i > 0 && (
              <span
                aria-hidden
                className={cn("absolute top-3.5 right-1/2 h-0.5 w-full", i <= current ? "bg-success" : "bg-border")}
              />
            )}
            <span
              className={cn(
                "relative z-10 flex size-7 items-center justify-center rounded-full border-2 text-xs font-semibold",
                done && "border-success bg-success text-white",
                active && "border-primary bg-primary text-primary-foreground",
                !done && !active && "border-border bg-card text-muted-foreground",
              )}
              aria-current={active ? "step" : undefined}
            >
              {done ? <Check className="size-4" aria-hidden /> : i + 1}
            </span>
            <span className={cn("text-[11px] leading-tight sm:text-xs", active ? "font-semibold" : "text-muted-foreground")}>
              {label}
            </span>
            {i === 1 && auto && i <= current && <span className="text-[11px] text-muted-foreground">Otomatik</span>}
          </li>
        );
      })}
    </ol>
  );
}
