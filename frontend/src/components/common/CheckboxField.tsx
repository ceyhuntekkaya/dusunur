"use client";

import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";

type Props = {
  id: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  label: string;
  hint?: string;
  disabled?: boolean;
};

export function CheckboxField({ id, checked, onCheckedChange, label, hint, disabled }: Props) {
  return (
    <div className="flex min-h-11 items-start gap-3 py-1.5">
      <Checkbox
        id={id}
        className="mt-0.5"
        checked={checked}
        disabled={disabled}
        onCheckedChange={(v) => onCheckedChange(v === true)}
      />
      <div className="space-y-0.5">
        <Label htmlFor={id} className="text-[15px] font-medium">
          {label}
        </Label>
        {hint && <p className="text-[13px] text-muted-foreground">{hint}</p>}
      </div>
    </div>
  );
}
