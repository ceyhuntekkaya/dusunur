import { addDays, format, parseISO } from "date-fns";
import { tr } from "date-fns/locale";
import type { EntryDto } from "./types";

const DATE = "yyyy-MM-dd";
const LOCAL_DT = "yyyy-MM-dd'T'HH:mm";

export function addDaysToDate(date: string, days: number): string {
  return format(addDays(parseISO(date), days), DATE);
}

export function toFcEvent(entry: EntryDto, color: string) {
  return {
    id: String(entry.id),
    title: entry.title,
    allDay: entry.allDay,
    start: entry.allDay ? entry.startDate! : entry.startAt,
    end: entry.allDay ? addDaysToDate(entry.endDate!, 1) : entry.endAt,
    backgroundColor: color,
    borderColor: color,
    textColor: "#ffffff",
    extendedProps: { entry },
  };
}

export function fcEndToApiEndDate(fcEnd: string): string {
  return addDaysToDate(fcEnd, -1);
}

export function toLocalInput(value: Date | string): string {
  return format(typeof value === "string" ? parseISO(value) : value, LOCAL_DT);
}

export function toDateInput(value: Date | string): string {
  return format(typeof value === "string" ? parseISO(value) : value, DATE);
}

export function localInputToOffsetIso(value: string): string {
  return format(parseISO(value), "yyyy-MM-dd'T'HH:mm:00XXX");
}

export function entryTimeText(entry: EntryDto): string {
  if (entry.allDay) {
    const s = parseISO(entry.startDate!);
    const e = parseISO(entry.endDate!);
    if (entry.startDate === entry.endDate) return `${format(s, "d MMMM yyyy", { locale: tr })} (tüm gün)`;
    if (format(s, "yyyy-MM") === format(e, "yyyy-MM"))
      return `${format(s, "d", { locale: tr })}–${format(e, "d MMMM yyyy", { locale: tr })} (tüm gün)`;
    return `${format(s, "d MMMM", { locale: tr })} – ${format(e, "d MMMM yyyy", { locale: tr })} (tüm gün)`;
  }
  const s = parseISO(entry.startAt);
  const e = parseISO(entry.endAt);
  if (format(s, DATE) === format(e, DATE))
    return `${format(s, "d MMMM yyyy EEEE, HH:mm", { locale: tr })}–${format(e, "HH:mm")}`;
  return `${format(s, "d MMM HH:mm", { locale: tr })} – ${format(e, "d MMM HH:mm", { locale: tr })}`;
}
