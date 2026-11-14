import { differenceInCalendarDays, format, isSameDay, parseISO } from "date-fns";
import { tr } from "date-fns/locale";

export function formatListTime(iso: string, now: Date = new Date()): string {
  const d = parseISO(iso);
  const days = differenceInCalendarDays(now, d);
  if (days === 0) return format(d, "HH:mm", { locale: tr });
  if (days === 1) return "Dün";
  if (days > 1 && days < 7) return format(d, "EEEE", { locale: tr });
  return format(d, "dd.MM.yyyy", { locale: tr });
}

export function formatDayLabel(iso: string, now: Date = new Date()): string {
  const d = parseISO(iso);
  const days = differenceInCalendarDays(now, d);
  if (days === 0) return "Bugün";
  if (days === 1) return "Dün";
  return format(d, "d MMMM yyyy", { locale: tr });
}

export const formatClock = (iso: string) => format(parseISO(iso), "HH:mm", { locale: tr });

export const sameDay = (a: string, b: string) => isSameDay(parseISO(a), parseISO(b));

export const gapMinutes = (a: string, b: string) =>
  Math.abs(parseISO(b).getTime() - parseISO(a).getTime()) / 60_000;

const NAME_COLORS = ["#1E40AF", "#166534", "#9D174D", "#92400E", "#3730A3", "#115E59", "#9F1239", "#5B21B6"];
export const nameColor = (userId: number) => NAME_COLORS[Math.abs(userId) % NAME_COLORS.length];
