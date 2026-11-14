"use client";

import { useCallback, useEffect, useState } from "react";
import type { CalendarDto } from "./types";

const KEY = "takvim-gorunur";

function read(): Record<string, boolean> {
  try {
    const raw = window.localStorage.getItem(KEY);
    const parsed: unknown = raw ? JSON.parse(raw) : {};
    return typeof parsed === "object" && parsed !== null ? (parsed as Record<string, boolean>) : {};
  } catch {
    return {};
  }
}

function write(map: Record<string, boolean>) {
  try {
    window.localStorage.setItem(KEY, JSON.stringify(map));
  } catch {
    // Depolama kullanılamıyorsa seçimler yalnızca oturum boyunca geçerli olur.
  }
}

function defaultVisible(c: CalendarDto, myBranchName?: string): boolean {
  switch (c.section) {
    case "INSTITUTION":
    case "MINE":
    case "SHARED":
    case "GROUP":
      return true;
    case "BRANCH":
      return !!myBranchName && c.branchName === myBranchName;
    default:
      return false;
  }
}

export function useCalendarVisibility(calendars: CalendarDto[] | undefined, myBranchName?: string) {
  const [stored, setStored] = useState<Record<string, boolean>>({});
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    setStored(read());
    setLoaded(true);
  }, []);

  const isVisible = useCallback(
    (c: CalendarDto) => stored[String(c.id)] ?? defaultVisible(c, myBranchName),
    [stored, myBranchName],
  );

  const toggle = useCallback(
    (c: CalendarDto) => {
      setStored((prev) => {
        const next = { ...prev, [String(c.id)]: !(prev[String(c.id)] ?? defaultVisible(c, myBranchName)) };
        write(next);
        return next;
      });
    },
    [myBranchName],
  );

  const showOnly = useCallback(
    (id: number) => {
      const next: Record<string, boolean> = {};
      for (const c of calendars ?? []) next[String(c.id)] = c.id === id;
      write(next);
      setStored(next);
    },
    [calendars],
  );

  const visibleIds = loaded && calendars ? calendars.filter(isVisible).map((c) => c.id) : [];
  return { loaded, isVisible, toggle, showOnly, visibleIds };
}
