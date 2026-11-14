"use client";

import { useState } from "react";
import { Building2, Loader2, Plus, User, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { SelectedChips } from "@/components/common/SelectedChips";
import { UserAvatar } from "@/components/common/UserAvatar";
import { useDirectorySearch } from "@/features/core/api";
import type { TargetType, TargetView } from "@/features/core/types";
import { useDebouncedValue } from "@/lib/debounce";

type Props = {
  value: TargetView[];
  onChange: (targets: TargetView[]) => void;
  allowed: TargetType[];
  groupScope?: "mine" | "all";
  id?: string;
};

const ALL_VIEW: TargetView = { type: "ALL", name: "Tüm Kurum" };

function keyOf(t: { type: TargetType; id?: number }) {
  return `${t.type}:${t.id ?? ""}`;
}

export function TargetPicker({ value, onChange, allowed, groupScope = "mine", id }: Props) {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const q = useDebouncedValue(input.trim(), 300);

  const kinds = [
    ...(allowed.includes("USER") ? (["user"] as const) : []),
    ...(allowed.includes("GROUP") ? (["group"] as const) : []),
    ...(allowed.includes("BRANCH") ? (["branch"] as const) : []),
  ];
  const search = useDirectorySearch({ q, kinds, groupScope });

  const selected = new Set(value.map(keyOf));
  const hasAll = value.some((t) => t.type === "ALL");

  const q2 = q.toLocaleLowerCase("tr");
  const showAll = allowed.includes("ALL") && !hasAll && (q === "" || "tüm kurum".includes(q2));
  const users = (search.data?.users ?? []).filter((u) => !selected.has(`USER:${u.id}`));
  const groups = (search.data?.groups ?? []).filter((g) => !selected.has(`GROUP:${g.id}`));
  const branches = (search.data?.branches ?? []).filter((b) => !selected.has(`BRANCH:${b.id}`));
  const empty = !showAll && users.length + groups.length + branches.length === 0;

  function add(target: TargetView) {
    onChange(target.type === "ALL" ? [target] : [...value, target]);
    setInput("");
    setOpen(false);
  }

  return (
    <div className="space-y-2">
      <SelectedChips
        chips={value.map((t) => ({ key: keyOf(t), label: t.name }))}
        onRemove={(key) => onChange(value.filter((t) => keyOf(t) !== key))}
      />
      {!hasAll && (
        <Popover open={open} onOpenChange={setOpen}>
          <PopoverTrigger
            render={<Button id={id} type="button" variant="outline" className="h-11 justify-start md:h-10" />}
          >
            <Plus aria-hidden />
            Alıcı ekle
          </PopoverTrigger>
          <PopoverContent align="start" className="w-[min(360px,calc(100vw-2rem))] p-0">
            <Command shouldFilter={false}>
              <CommandInput
                value={input}
                onValueChange={setInput}
                placeholder={allowed.includes("USER") ? "Kişi, grup veya şube ara" : "Grup veya şube ara"}
              />
              <CommandList>
                {search.isFetching && empty ? (
                  <div className="flex justify-center py-6">
                    <Loader2 className="size-4 animate-spin text-muted-foreground" aria-label="Aranıyor" />
                  </div>
                ) : (
                  empty && <CommandEmpty>Eşleşen alıcı bulunamadı.</CommandEmpty>
                )}
                {showAll && (
                  <CommandGroup>
                    <CommandItem value="ALL" className="min-h-11 gap-3" onSelect={() => add(ALL_VIEW)}>
                      <Building2 aria-hidden />
                      Tüm Kurum
                    </CommandItem>
                  </CommandGroup>
                )}
                {branches.length > 0 && (
                  <CommandGroup heading="Şubeler">
                    {branches.map((b) => (
                      <CommandItem
                        key={b.id}
                        value={`BRANCH:${b.id}`}
                        className="min-h-11 gap-3"
                        onSelect={() => add({ type: "BRANCH", id: b.id, name: b.name })}
                      >
                        <Building2 aria-hidden />
                        {b.name}
                      </CommandItem>
                    ))}
                  </CommandGroup>
                )}
                {groups.length > 0 && (
                  <CommandGroup heading="Gruplar">
                    {groups.map((g) => (
                      <CommandItem
                        key={g.id}
                        value={`GROUP:${g.id}`}
                        className="min-h-11 gap-3"
                        onSelect={() => add({ type: "GROUP", id: g.id, name: g.name })}
                      >
                        <Users aria-hidden />
                        <span className="min-w-0 flex-1 truncate">{g.name}</span>
                        <span className="text-xs text-muted-foreground">{g.memberCount} kişi</span>
                      </CommandItem>
                    ))}
                  </CommandGroup>
                )}
                {users.length > 0 && (
                  <CommandGroup heading="Kişiler">
                    {users.map((u) => (
                      <CommandItem
                        key={u.id}
                        value={`USER:${u.id}`}
                        className="min-h-11 gap-3"
                        onSelect={() => add({ type: "USER", id: u.id, name: u.fullName })}
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
                {allowed.includes("USER") && q.length < 2 && (
                  <p className="flex items-center justify-center gap-1.5 px-3 py-3 text-xs text-muted-foreground">
                    <User className="size-3.5" aria-hidden />
                    Kişi aramak için en az 2 harf yazın.
                  </p>
                )}
              </CommandList>
            </Command>
          </PopoverContent>
        </Popover>
      )}
    </div>
  );
}
