import { z } from "zod";
import { optionalText } from "@/lib/validation";

export const DMS_NAME_INVALID =
  'Ad boş olamaz, çok uzun olamaz ve şu karakterleri içeremez: / \\ : * ? " < > |';

const FORBIDDEN = /[/\\:*?"<>|\u0000-\u001f\u007f]/;

export function dmsNameSchema(max: number) {
  return z
    .string()
    .transform((s) => s.trim())
    .pipe(
      z
        .string()
        .min(1, DMS_NAME_INVALID)
        .max(max, DMS_NAME_INVALID)
        .refine((s) => s !== "." && s !== ".." && !FORBIDDEN.test(s), DMS_NAME_INVALID),
    );
}

const spaceName = z
  .string()
  .transform((s) => s.trim())
  .pipe(z.string().min(2, "Alan adı 2–100 karakter olmalıdır.").max(100, "Alan adı 2–100 karakter olmalıdır."));

export const spaceFormSchema = z.object({
  name: spaceName,
  description: optionalText(500),
});
export type SpaceFormValues = z.input<typeof spaceFormSchema>;
