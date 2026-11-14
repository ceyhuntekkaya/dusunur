import { z } from "zod";

const collapse = (s: string) => s.trim().replace(/\s+/g, " ");

export function nameSchema(label: "Ad" | "Soyad") {
  const message = `${label} 2–50 karakter olmalı ve yalnızca harf içermelidir.`;
  return z
    .string()
    .transform(collapse)
    .pipe(
      z
        .string()
        .min(2, message)
        .max(50, message)
        .regex(/^[\p{L}][\p{L} .'-]*$/u, message),
    );
}

export const emailSchema = z
  .string()
  .transform((s) => collapse(s).toLowerCase())
  .pipe(
    z
      .string()
      .max(254, "Geçerli bir e-posta adresi girin.")
      .regex(/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/, "Geçerli bir e-posta adresi girin."),
  );

export const phoneSchema = z
  .string()
  .transform((s) => s.replace(/\D/g, ""))
  .pipe(
    z
      .string()
      .refine((s) => s === "" || /^(5\d{9}|05\d{9}|905\d{9})$/.test(s), {
        message: "Telefon numarası 05XX XXX XX XX biçiminde olmalıdır.",
      }),
  );

export const passwordSchema = z
  .string()
  .refine((s) => s.length >= 8 && s.length <= 64 && /\p{L}/u.test(s) && /\d/.test(s), {
    message: "Şifre en az 8 karakter olmalı, en az bir harf ve bir rakam içermelidir.",
  });

export function optionalText(max: number, label = "Açıklama") {
  return z
    .string()
    .transform((s) => s.trim())
    .pipe(z.string().max(max, `${label} en fazla ${max} karakter olabilir.`));
}

export const entityNameSchema = z
  .string()
  .transform(collapse)
  .pipe(z.string().min(2, "Ad 2–100 karakter olmalıdır.").max(100, "Ad 2–100 karakter olmalıdır."));
