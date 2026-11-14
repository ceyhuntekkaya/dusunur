import { z } from "zod";
import type { DirUser, MembershipInfo, MembershipInput } from "@/features/core/types";
import {
  emailSchema,
  entityNameSchema,
  nameSchema,
  optionalText,
  phoneSchema,
} from "@/lib/validation";

const dirUser = z.custom<DirUser>();

export const membershipFormSchema = z.object({
  branchId: z.string(),
  title: optionalText(100, "Unvan"),
  manager: z.boolean(),
  printOperator: z.boolean(),
  canCreateDmsSpace: z.boolean(),
  approver: z.array(dirUser).max(1),
});
export type MembershipFormValues = z.input<typeof membershipFormSchema>;

export const EMPTY_MEMBERSHIP: MembershipFormValues = {
  branchId: "",
  title: "",
  manager: false,
  printOperator: false,
  canCreateDmsSpace: false,
  approver: [],
};

export function membershipToForm(m?: MembershipInfo): MembershipFormValues {
  if (!m) return EMPTY_MEMBERSHIP;
  return {
    branchId: String(m.branchId),
    title: m.title ?? "",
    manager: m.manager,
    printOperator: m.printOperator,
    canCreateDmsSpace: m.canCreateDmsSpace,
    approver: m.printApproverId ? [{ id: m.printApproverId, fullName: m.printApproverName ?? "" }] : [],
  };
}

export function formToMembership(v: z.output<typeof membershipFormSchema>): MembershipInput {
  return {
    branchId: Number(v.branchId),
    title: v.title || undefined,
    manager: v.manager,
    printOperator: v.printOperator,
    canCreateDmsSpace: v.canCreateDmsSpace,
    printApproverId: v.approver[0]?.id,
  };
}

const BRANCH_REQUIRED = "Şube seçin.";

export const membershipEditSchema = z.object({
  membership: membershipFormSchema.refine((m) => m.branchId !== "", {
    path: ["branchId"],
    message: BRANCH_REQUIRED,
  }),
});

export const userFormSchema = z
  .object({
    firstName: nameSchema("Ad"),
    lastName: nameSchema("Soyad"),
    email: emailSchema,
    phone: phoneSchema,
    discipline: optionalText(100, "Branş"),
    systemAdmin: z.boolean(),
    assign: z.boolean(),
    membership: membershipFormSchema,
  })
  .superRefine((v, ctx) => {
    if (v.assign && v.membership.branchId === "") {
      ctx.addIssue({ code: "custom", path: ["membership", "branchId"], message: BRANCH_REQUIRED });
    }
  });

export const branchFormSchema = z.object({ name: entityNameSchema });

const SEASON_NAME_MESSAGE =
  "Yıl adı 2026-2027 biçiminde olmalı ve ikinci yıl birinciden bir fazla olmalıdır.";

export const seasonFormSchema = z
  .object({
    name: z
      .string()
      .transform((s) => s.trim())
      .pipe(
        z.string().refine(
          (s) => {
            const m = /^(\d{4})-(\d{4})$/.exec(s);
            return !!m && Number(m[2]) === Number(m[1]) + 1;
          },
          { message: SEASON_NAME_MESSAGE },
        ),
      ),
    startDate: z.string().min(1, "Başlangıç tarihini seçin."),
    endDate: z.string().min(1, "Bitiş tarihini seçin."),
  })
  .superRefine((v, ctx) => {
    if (v.startDate && v.endDate && v.endDate <= v.startDate) {
      ctx.addIssue({
        code: "custom",
        path: ["endDate"],
        message: "Bitiş tarihi başlangıç tarihinden sonra olmalıdır.",
      });
    }
  });

export const groupFormSchema = z.object({
  name: entityNameSchema,
  description: optionalText(500),
});

export const groupCreateSchema = groupFormSchema.extend({ members: z.array(dirUser).max(1000) });
