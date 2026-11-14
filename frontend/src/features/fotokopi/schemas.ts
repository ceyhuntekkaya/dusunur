import { addDays, differenceInCalendarDays, parseISO, startOfDay } from "date-fns";
import { z } from "zod";
import type { DirUser } from "@/features/core/types";
import { optionalText } from "@/lib/validation";
import { MAX_TOTAL_MESSAGE } from "./labels";

const PAGE_MESSAGE = "Sayfa sayısı 1 ile 2000 arasında olmalıdır.";
const COPIES_MESSAGE = "Kopya adedi 1 ile 1000 arasında olmalıdır.";
const TITLE_MESSAGE = "Açıklama 3–200 karakter olmalıdır.";
const CHOICE_MESSAGE = "Bir seçenek seçin.";

export function printRequestSchema(limits: { minLeadMinutes: number; maxDaysAhead: number }) {
  return z
    .object({
      title: z
        .string()
        .transform((s) => s.trim())
        .pipe(z.string().min(3, TITLE_MESSAGE).max(200, TITLE_MESSAGE)),
      fileId: z.string().optional(),
      originalOnPaper: z.boolean(),
      pageCount: z.number({ error: PAGE_MESSAGE }).int(PAGE_MESSAGE).min(1, PAGE_MESSAGE).max(2000, PAGE_MESSAGE),
      copies: z.number({ error: COPIES_MESSAGE }).int(COPIES_MESSAGE).min(1, COPIES_MESSAGE).max(1000, COPIES_MESSAGE),
      color: z.enum(["BW", "COLOR"], { error: CHOICE_MESSAGE }),
      sides: z.enum(["SINGLE", "DOUBLE"], { error: CHOICE_MESSAGE }),
      paperSize: z.enum(["A4", "A3"], { error: CHOICE_MESSAGE }),
      secret: z.boolean(),
      dueAt: z.string().min(1, "Teslim tarihi ve saati seçin."),
      note: optionalText(500, "Not"),
    })
    .superRefine((v, ctx) => {
      if (!v.originalOnPaper && !v.fileId) {
        ctx.addIssue({
          code: "custom",
          path: ["fileId"],
          message: 'Bir dosya yükleyin veya "Basılı orijinali getireceğim" seçeneğini işaretleyin.',
        });
      }
      if (v.pageCount * v.copies > 20000) {
        ctx.addIssue({ code: "custom", path: ["copies"], message: MAX_TOTAL_MESSAGE });
      }
      const due = new Date(v.dueAt).getTime();
      if (!Number.isNaN(due)) {
        const now = Date.now();
        if (due < now + limits.minLeadMinutes * 60_000) {
          ctx.addIssue({
            code: "custom",
            path: ["dueAt"],
            message: `Teslim zamanı en erken ${limits.minLeadMinutes} dakika sonrası olabilir. Lütfen daha ileri bir saat seçin.`,
          });
        } else if (due > now + limits.maxDaysAhead * 86_400_000) {
          ctx.addIssue({
            code: "custom",
            path: ["dueAt"],
            message: `Teslim zamanı en fazla ${limits.maxDaysAhead} gün sonrası olabilir.`,
          });
        }
      }
    });
}

export type PrintRequestFormIn = z.input<ReturnType<typeof printRequestSchema>>;
export type PrintRequestFormOut = z.output<ReturnType<typeof printRequestSchema>>;

export const rejectSchema = z.object({
  reason: z
    .string()
    .transform((s) => s.trim())
    .pipe(z.string().min(5, "Red gerekçesi 5–500 karakter olmalıdır.").max(500, "Red gerekçesi 5–500 karakter olmalıdır.")),
});

const DATES_MESSAGE =
  "Başlangıç bugün veya sonrası, bitiş başlangıçtan sonra olmalı; vekâlet en fazla 90 gün sürebilir.";

export const delegationSchema = z
  .object({
    delegate: z.custom<DirUser[]>().refine((u) => u.length === 1, { message: "Vekil seçin." }),
    startDate: z.string().min(1, "Başlangıç tarihini seçin."),
    endDate: z.string().min(1, "Bitiş tarihini seçin."),
  })
  .superRefine((v, ctx) => {
    if (!v.startDate || !v.endDate) return;
    const start = parseISO(v.startDate);
    const end = parseISO(v.endDate);
    const today = startOfDay(new Date());
    if (start < today) {
      ctx.addIssue({ code: "custom", path: ["startDate"], message: DATES_MESSAGE });
    } else if (end < start || differenceInCalendarDays(end, start) > 90 || end > addDays(start, 90)) {
      ctx.addIssue({ code: "custom", path: ["endDate"], message: DATES_MESSAGE });
    }
  });
