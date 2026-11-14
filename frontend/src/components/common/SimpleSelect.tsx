"use client";

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { cn } from "@/lib/utils";

export type SelectOption = { value: string; label: string };

type Props = {
  value: string;
  onChange: (value: string) => void;
  options: SelectOption[];
  placeholder?: string;
  id?: string;
  invalid?: boolean;
  disabled?: boolean;
  className?: string;
};

export function SimpleSelect({ value, onChange, options, placeholder, id, invalid, disabled, className }: Props) {
  return (
    <Select
      value={value === "" ? null : value}
      onValueChange={(v) => onChange(v ?? "")}
      items={options}
      disabled={disabled}
    >
      <SelectTrigger
        id={id}
        aria-invalid={invalid}
        className={cn("h-11 w-full bg-card text-[15px] md:h-10", className)}
      >
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {options.map((o) => (
          <SelectItem key={o.value} value={o.value} className="min-h-10">
            {o.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
