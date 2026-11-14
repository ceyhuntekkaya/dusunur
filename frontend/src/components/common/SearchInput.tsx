"use client";

import { useEffect, useRef, useState } from "react";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { useDebouncedValue } from "@/lib/debounce";
import { cn } from "@/lib/utils";

type Props = {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  label: string;
  className?: string;
};

export function SearchInput({ value, onChange, placeholder, label, className }: Props) {
  const [text, setText] = useState(value);
  const debounced = useDebouncedValue(text.trim(), 300);
  const latest = useRef({ value, onChange });
  useEffect(() => {
    latest.current = { value, onChange };
  });

  useEffect(() => {
    if (debounced !== latest.current.value) latest.current.onChange(debounced);
  }, [debounced]);

  return (
    <div className={cn("relative", className)}>
      <Search
        className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
        aria-hidden
      />
      <Input
        type="search"
        aria-label={label}
        placeholder={placeholder}
        className="h-11 bg-card pl-9 md:h-10"
        value={text}
        maxLength={100}
        onChange={(e) => setText(e.target.value)}
      />
    </div>
  );
}
