import {
  differenceInCalendarDays,
  differenceInMinutes,
  format,
  isSameYear,
  parseISO,
} from "date-fns";
import { tr } from "date-fns/locale";

export function formatBadgeCount(n: number): string {
  return n > 99 ? "99+" : String(n);
}

export function formatDateText(value: string | Date, relative = false, now: Date = new Date()): string {
  const dateOnly = typeof value === "string" && value.length === 10;
  const date = typeof value === "string" ? parseISO(value) : value;
  const time = dateOnly ? "" : ` ${format(date, "HH:mm", { locale: tr })}`;

  if (relative && !dateOnly) {
    const minutes = differenceInMinutes(now, date);
    if (minutes >= 0 && minutes < 60) return minutes < 1 ? "Az önce" : `${minutes} dk önce`;
  }

  const days = differenceInCalendarDays(now, date);
  if (days === 0) return `Bugün${time}`;
  if (days === 1) return `Dün${time}`;
  if (isSameYear(date, now)) return `${format(date, "d MMM", { locale: tr })}${time}`;
  return format(date, "d MMM yyyy", { locale: tr });
}

export function formatFullDate(value: string | Date): string {
  const date = typeof value === "string" ? parseISO(value) : value;
  const dateOnly = typeof value === "string" && value.length === 10;
  return format(date, dateOnly ? "d MMMM yyyy" : "d MMMM yyyy HH:mm", { locale: tr });
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1).replace(".", ",")} MB`;
}
