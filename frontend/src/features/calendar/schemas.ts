import { differenceInCalendarDays, differenceInMinutes, parseISO } from "date-fns";
import { z } from "zod";
import { optionalText } from "@/lib/validation";

const END_BEFORE = "Bitiş, başlangıçtan sonra olmalıdır.";
const TOO_LONG = "Bir kayıt en fazla 31 gün sürebilir.";
const CAL_NAME = "Takvim adı 2–100 karakter olmalıdır.";

export const entryFormSchema = z
  .object({
    calendarId: z.string().min(1, "Bir takvim seçin."),
    title: z
      .string()
      .transform((s) => s.trim())
      .pipe(z.string().min(2, "Başlık 2–150 karakter olmalıdır.").max(150, "Başlık 2–150 karakter olmalıdır.")),
    allDay: z.boolean(),
    startDate: z.string(),
    endDate: z.string(),
    startAt: z.string(),
    endAt: z.string(),
    location: optionalText(200, "Konum"),
    description: optionalText(2000, "Açıklama"),
  })
  .superRefine((v, ctx) => {
    if (v.allDay) {
      if (!v.startDate || !v.endDate) {
        ctx.addIssue({ code: "custom", path: ["endDate"], message: "Başlangıç ve bitiş tarihini seçin." });
        return;
      }
      const days = differenceInCalendarDays(parseISO(v.endDate), parseISO(v.startDate));
      if (days < 0) ctx.addIssue({ code: "custom", path: ["endDate"], message: END_BEFORE });
      else if (days + 1 > 31) ctx.addIssue({ code: "custom", path: ["endDate"], message: TOO_LONG });
      return;
    }
    if (!v.startAt || !v.endAt) {
      ctx.addIssue({ code: "custom", path: ["endAt"], message: "Başlangıç ve bitiş saatini seçin." });
      return;
    }
    const minutes = differenceInMinutes(parseISO(v.endAt), parseISO(v.startAt));
    if (minutes <= 0) ctx.addIssue({ code: "custom", path: ["endAt"], message: END_BEFORE });
    else if (minutes > 31 * 24 * 60) ctx.addIssue({ code: "custom", path: ["endAt"], message: TOO_LONG });
  });

export type EntryFormValues = z.input<typeof entryFormSchema>;

const calendarName = z
  .string()
  .transform((s) => s.trim().replace(/\s+/g, " "))
  .pipe(z.string().min(2, CAL_NAME).max(100, CAL_NAME));

export const calendarFormSchema = z.object({
  name: calendarName,
  description: optionalText(500),
  branchId: z.string(),
});
export type CalendarFormValues = z.input<typeof calendarFormSchema>;
