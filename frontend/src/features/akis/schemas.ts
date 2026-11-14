import { z } from "zod";
import type { UploadedFileDto } from "@/features/core/types";

const target = z.object({
  type: z.enum(["USER", "GROUP", "BRANCH", "ALL"]),
  id: z.number().optional(),
  name: z.string(),
});

export const composerSchema = z
  .object({
    targets: z
      .array(target)
      .min(1, "En az bir alıcı seçin.")
      .max(50, "En fazla 50 alıcı seçebilirsiniz. Daha geniş bir kitle için grup veya şube seçin."),
    content: z
      .string()
      .transform((s) => s.trim())
      .pipe(z.string().max(5000, "Metin en fazla 5000 karakter olabilir.")),
    files: z.custom<UploadedFileDto[]>().refine((f) => f.length <= 10, {
      message: "Bir paylaşıma en fazla 10 dosya eklenebilir.",
    }),
    important: z.boolean(),
  })
  .refine((v) => v.content !== "" || v.files.length > 0, {
    message: "Paylaşım için bir metin yazın veya dosya ekleyin.",
    path: ["content"],
  });

export const commentSchema = z
  .string()
  .transform((s) => s.trim())
  .pipe(z.string().min(1, "Yorum 1–1000 karakter olmalıdır.").max(1000, "Yorum 1–1000 karakter olmalıdır."));
