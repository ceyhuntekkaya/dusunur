"use client";

import { useState } from "react";
import { ChevronsUpDown, Loader2 } from "lucide-react";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { SelectedChips } from "@/components/common/SelectedChips";
import { UserAvatar } from "@/components/common/UserAvatar";
import { useAdminUserLookup } from "@/features/core/admin-api";
import { useDirectorySearch } from "@/features/core/api";
import type { DirUser } from "@/features/core/types";
import { useDebouncedValue } from "@/lib/debounce";

type Props = {
  value: DirUser[];
  onChange: (users: DirUser[]) => void;
  multiple?: boolean;
  placeholder?: string;
  excludeIds?: number[];
  id?: string;
  invalid?: boolean;
  lookup?: { seasonId?: number };
};

export function UserPicker({
  value,
  onChange,
  multiple = true,
  placeholder = "Kişi ara…",
  excludeIds = [],
  id,
  invalid,
  lookup,
}: Props) {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const q = useDebouncedValue(input.trim(), 300);
  const search = useDirectorySearch({ q, kinds: ["user"], enabled: !lookup && q.length >= 2 });
  const adminSearch = useAdminUserLookup(q, lookup?.seasonId, !!lookup);
  const found = lookup ? (adminSearch.data ?? []) : (search.data?.users ?? []);
  const fetching = lookup ? adminSearch.isFetching : search.isFetching;

  const selectedIds = new Set(value.map((u) => u.id));
  const users = (q.length >= 2 ? found : []).filter(
    (u) => !excludeIds.includes(u.id) && !(multiple && selectedIds.has(u.id)),
  );

  function pick(user: DirUser) {
    if (multiple) {
      onChange([...value, user]);
    } else {
      onChange([user]);
      setOpen(false);
    }
    setInput("");
  }

  return (
    <div className="space-y-2">
      {multiple && (
        <SelectedChips
          chips={value.map((u) => ({ key: String(u.id), label: u.fullName }))}
          onRemove={(key) => onChange(value.filter((u) => String(u.id) !== key))}
        />
      )}
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger
          id={id}
          aria-invalid={invalid}
          className="flex h-11 w-full items-center justify-between rounded-lg border border-input bg-card px-3 text-left text-[15px] outline-none focus-visible:ring-3 focus-visible:ring-ring/50 aria-invalid:border-destructive md:h-10"
        >
          <span className={!multiple && value[0] ? "truncate" : "truncate text-muted-foreground"}>
            {!multiple && value[0] ? value[0].fullName : placeholder}
          </span>
          <ChevronsUpDown className="size-4 shrink-0 opacity-50" aria-hidden />
        </PopoverTrigger>
        <PopoverContent align="start" className="w-[min(360px,calc(100vw-2rem))] p-0">
          <Command shouldFilter={false}>
            <CommandInput value={input} onValueChange={setInput} placeholder="Ad veya soyad yazın" />
            <CommandList>
              {q.length < 2 ? (
                <p className="py-6 text-center text-sm text-muted-foreground">
                  Aramak için en az 2 harf yazın.
                </p>
              ) : fetching && users.length === 0 ? (
                <div className="flex justify-center py-6">
                  <Loader2 className="size-4 animate-spin text-muted-foreground" aria-label="Aranıyor" />
                </div>
              ) : (
                <CommandEmpty>Eşleşen kişi bulunamadı.</CommandEmpty>
              )}
              {users.length > 0 && (
                <CommandGroup>
                  {users.map((u) => (
                    <CommandItem
                      key={u.id}
                      value={String(u.id)}
                      className="min-h-11 gap-3"
                      onSelect={() => pick(u)}
                    >
                      <UserAvatar userId={u.id} name={u.fullName} size="sm" />
                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-sm">{u.fullName}</span>
                        <span className="block truncate text-xs text-muted-foreground">
                          {[u.title, u.branchName].filter(Boolean).join(" · ")}
                        </span>
                      </span>
                    </CommandItem>
                  ))}
                </CommandGroup>
              )}
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>
    </div>
  );
}
