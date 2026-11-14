"use client";

import { useState } from "react";
import { ChevronDown, ChevronRight, Lock } from "lucide-react";
import { RowMenu, type RowMenuItem } from "@/components/common/RowMenu";
import type { CalendarDto, CalendarSection } from "../types";

const SECTIONS: { key: CalendarSection; title: string; collapsedByDefault?: boolean }[] = [
  { key: "INSTITUTION", title: "Kurum" },
  { key: "BRANCH", title: "Şubeler" },
  { key: "MINE", title: "Takvimlerim" },
  { key: "SHARED", title: "Benimle Paylaşılanlar" },
  { key: "GROUP", title: "Gruplarım" },
  { key: "OTHER_GROUP", title: "Diğer Grup Takvimleri", collapsedByDefault: true },
  { key: "RESOURCE", title: "Salonlar ve Laboratuvarlar" },
];

type Props = {
  calendars: CalendarDto[];
  isVisible: (c: CalendarDto) => boolean;
  onToggle: (c: CalendarDto) => void;
  isManager: boolean;
  onShare: (c: CalendarDto) => void;
  onRename: (c: CalendarDto) => void;
  onEditResource: (c: CalendarDto) => void;
  onDelete: (c: CalendarDto) => void;
  onShowOnly: (c: CalendarDto) => void;
  onCreateCalendar: () => void;
  onCreateResource: () => void;
};

export function CalendarList(props: Props) {
  const { calendars, isManager } = props;
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({ OTHER_GROUP: true });

  function menuItems(c: CalendarDto): RowMenuItem[] {
    if (c.type === "RESOURCE") {
      return [
        { label: "Yalnızca bunu göster", onSelect: () => props.onShowOnly(c) },
        { label: "Düzenle", onSelect: () => props.onEditResource(c), hidden: !c.canManage },
        { label: "Sil", onSelect: () => props.onDelete(c), destructive: true, hidden: !c.canManage },
      ];
    }
    if (!c.canManage) return [];
    return [
      { label: "Paylaş", onSelect: () => props.onShare(c), hidden: c.type !== "PERSONAL" },
      { label: "Yeniden adlandır", onSelect: () => props.onRename(c), hidden: c.isDefault },
      { label: "Sil", onSelect: () => props.onDelete(c), destructive: true, hidden: c.isDefault },
    ];
  }

  return (
    <div className="space-y-3">
      {SECTIONS.map((s) => {
        const items = calendars.filter((c) => c.section === s.key);
        const addable = s.key === "MINE" || (s.key === "RESOURCE" && isManager);
        if (items.length === 0 && !addable) return null;
        const isCollapsed = collapsed[s.key] ?? false;
        const Chevron = isCollapsed ? ChevronRight : ChevronDown;
        return (
          <section key={s.key}>
            <button
              type="button"
              aria-expanded={!isCollapsed}
              className="flex min-h-11 w-full items-center gap-1 rounded-md px-1 text-left text-xs font-semibold tracking-wide text-muted-foreground uppercase outline-none focus-visible:ring-2 focus-visible:ring-ring md:min-h-9"
              onClick={() => setCollapsed((p) => ({ ...p, [s.key]: !isCollapsed }))}
            >
              <Chevron className="size-4" aria-hidden />
              {s.title}
            </button>
            {!isCollapsed && (
              <ul>
                {items.map((c) => (
                  <li key={c.id} className="flex items-center">
                    <label className="flex min-h-11 min-w-0 flex-1 cursor-pointer items-center gap-3 rounded-md px-1 md:min-h-10">
                      <input
                        type="checkbox"
                        className="size-[18px] shrink-0"
                        style={{ accentColor: c.color }}
                        checked={props.isVisible(c)}
                        onChange={() => props.onToggle(c)}
                      />
                      <span className="min-w-0 truncate text-[15px]">
                        {c.name}
                        {c.type === "RESOURCE" && c.branchName && (
                          <span className="ml-1.5 text-[13px] text-muted-foreground">{c.branchName}</span>
                        )}
                        {c.section === "SHARED" && c.owner && (
                          <span className="ml-1.5 text-[13px] text-muted-foreground">{c.owner.fullName}</span>
                        )}
                      </span>
                      {c.readOnly && <Lock className="size-3.5 shrink-0 text-muted-foreground" aria-label="Salt okunur" />}
                    </label>
                    <RowMenu items={menuItems(c)} label={`${c.name} işlemleri`} />
                  </li>
                ))}
              </ul>
            )}
            {!isCollapsed && s.key === "MINE" && (
              <button
                type="button"
                className="min-h-11 rounded-md px-1 text-sm font-medium text-primary outline-none hover:underline focus-visible:ring-2 focus-visible:ring-ring md:min-h-9"
                onClick={props.onCreateCalendar}
              >
                + Takvim oluştur
              </button>
            )}
            {!isCollapsed && s.key === "RESOURCE" && isManager && (
              <button
                type="button"
                className="min-h-11 rounded-md px-1 text-sm font-medium text-primary outline-none hover:underline focus-visible:ring-2 focus-visible:ring-ring md:min-h-9"
                onClick={props.onCreateResource}
              >
                + Salon ekle
              </button>
            )}
          </section>
        );
      })}
    </div>
  );
}
