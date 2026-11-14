import { format, isToday, parseISO } from "date-fns";
import { tr } from "date-fns/locale";
import type { StatusBadge } from "@/components/common/StatusBadge";
import type { PaperSize, PrintColor, PrintRequestRow, PrintSides, PrintStatus } from "./types";

type Tone = NonNullable<React.ComponentProps<typeof StatusBadge>["tone"]>;

export const STATUS_LABEL: Record<PrintStatus, string> = {
  PENDING_APPROVAL: "Onay Bekliyor",
  APPROVED: "Onaylandı",
  IN_PROGRESS: "Hazırlanıyor",
  READY: "Hazır",
  DELIVERED: "Teslim Edildi",
  REJECTED: "Reddedildi",
  CANCELLED: "İptal Edildi",
};

export const STATUS_TONE: Record<PrintStatus, Tone> = {
  PENDING_APPROVAL: "warning",
  APPROVED: "info",
  IN_PROGRESS: "info",
  READY: "success",
  DELIVERED: "neutral",
  REJECTED: "danger",
  CANCELLED: "neutral",
};

export const COLOR_LABEL: Record<PrintColor, string> = { BW: "Siyah-Beyaz", COLOR: "Renkli" };
export const SIDES_LABEL: Record<PrintSides, string> = { SINGLE: "Tek yüz", DOUBLE: "Çift yüz" };
export const PAPER_LABEL: Record<PaperSize, string> = { A4: "A4", A3: "A3" };

const OPEN: PrintStatus[] = ["PENDING_APPROVAL", "APPROVED", "IN_PROGRESS", "READY"];

export function isOpen(status: PrintStatus): boolean {
  return OPEN.includes(status);
}

export function formatDue(value: string): string {
  return format(parseISO(value), "d MMM EEE HH:mm", { locale: tr });
}

export function dueState(row: Pick<PrintRequestRow, "status" | "dueAt">): "overdue" | "today" | "normal" {
  const due = parseISO(row.dueAt);
  if (isOpen(row.status) && due.getTime() < Date.now()) return "overdue";
  if (isToday(due)) return "today";
  return "normal";
}

export function specLine(r: Pick<PrintRequestRow, "pageCount" | "copies" | "color" | "paperSize" | "sides" | "originalOnPaper">): string {
  const base = `${r.pageCount} sayfa × ${r.copies} kopya · ${COLOR_LABEL[r.color]} · ${PAPER_LABEL[r.paperSize]} · ${SIDES_LABEL[r.sides]}`;
  return r.originalOnPaper ? `${base} · Basılı orijinal` : base;
}

export function sheetCount(pageCount: number, copies: number, sides: PrintSides): number {
  return Math.ceil(pageCount / (sides === "DOUBLE" ? 2 : 1)) * copies;
}

export function formatDateRange(startDate: string, endDate: string): string {
  const s = parseISO(startDate);
  const e = parseISO(endDate);
  if (s.getMonth() === e.getMonth() && s.getFullYear() === e.getFullYear()) {
    return `${format(s, "d", { locale: tr })}–${format(e, "d MMM", { locale: tr })}`;
  }
  return `${format(s, "d MMM", { locale: tr })} – ${format(e, "d MMM", { locale: tr })}`;
}

export const MAX_TOTAL_MESSAGE = "Toplam baskı (sayfa × kopya) en fazla 20.000 olabilir.";
