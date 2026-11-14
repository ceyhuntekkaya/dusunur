import { Check, Circle } from "lucide-react";
import { cn } from "@/lib/utils";

export function PasswordRules({ value }: { value: string }) {
  const rules = [
    { label: "En az 8 karakter", ok: value.length >= 8 },
    { label: "En az bir harf", ok: /\p{L}/u.test(value) },
    { label: "En az bir rakam", ok: /\d/.test(value) },
  ];
  return (
    <ul className="space-y-1 text-[13px]">
      {rules.map((r) => (
        <li
          key={r.label}
          className={cn("flex items-center gap-2", r.ok ? "text-success" : "text-muted-foreground")}
        >
          {r.ok ? <Check className="size-4" aria-hidden /> : <Circle className="size-4" aria-hidden />}
          {r.label}
        </li>
      ))}
    </ul>
  );
}
