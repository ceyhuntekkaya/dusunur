import { X } from "lucide-react";

export type Chip = { key: string; label: string };

export function SelectedChips({ chips, onRemove }: { chips: Chip[]; onRemove: (key: string) => void }) {
  if (chips.length === 0) return null;
  return (
    <ul className="flex flex-wrap gap-2">
      {chips.map((c) => (
        <li
          key={c.key}
          className="flex h-8 items-center gap-1 rounded-full bg-accent pr-1 pl-3 text-[13px] font-medium text-accent-foreground"
        >
          <span className="max-w-48 truncate">{c.label}</span>
          <button
            type="button"
            aria-label={`${c.label} seçimini kaldır`}
            className="flex size-6 items-center justify-center rounded-full outline-none hover:bg-black/10 focus-visible:ring-2 focus-visible:ring-ring"
            onClick={() => onRemove(c.key)}
          >
            <X className="size-3.5" aria-hidden />
          </button>
        </li>
      ))}
    </ul>
  );
}
