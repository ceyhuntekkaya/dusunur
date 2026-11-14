import { z } from "zod";
import { optionalText } from "@/lib/validation";
import type { DirUser } from "@/features/core/types";

export const messageContentSchema = optionalText(4000, "Mesaj");

const dirUser = z.custom<DirUser>();

export const chatTitleSchema = z
  .string()
  .transform((s) => s.trim())
  .pipe(
    z
      .string()
      .min(2, "Sohbet adı 2–100 karakter olmalıdır.")
      .max(100, "Sohbet adı 2–100 karakter olmalıdır."),
  );

export const newGroupSchema = z.object({
  title: chatTitleSchema,
  memberIds: z
    .array(dirUser)
    .min(2, "Grup sohbeti için en az 2 kişi seçin. Tek kişiyle yazışmak için birebir sohbet açın.")
    .max(199, "Bir grup sohbetinde en fazla 200 kişi olabilir."),
});

export const renameSchema = z.object({ title: chatTitleSchema });

export const addMembersSchema = z.object({
  userIds: z
    .array(dirUser)
    .min(1, "En az bir kişi seçin.")
    .max(100, "Tek seferde en fazla 100 kişi ekleyebilirsiniz."),
});
