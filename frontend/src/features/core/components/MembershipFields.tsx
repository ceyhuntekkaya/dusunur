"use client";

import { CheckboxField } from "@/components/common/CheckboxField";
import { FormField } from "@/components/common/FormField";
import { SimpleSelect } from "@/components/common/SimpleSelect";
import { UserPicker } from "@/components/common/UserPicker";
import { Input } from "@/components/ui/input";
import { useActiveBranches, useTitles } from "@/features/core/admin-api";
import type { MembershipFormValues } from "@/features/core/schemas";
import type { DirBranch } from "@/features/core/types";

type ErrorEntry = { message?: string } | undefined;

export type MembershipErrors = { branchId?: string; title?: string; printApproverId?: string };

export function membershipErrors(formErrors: unknown): MembershipErrors {
  const root = (formErrors ?? {}) as Record<string, unknown>;
  const nested = (root.membership ?? {}) as Record<string, ErrorEntry>;
  const flat = root as Record<string, ErrorEntry>;
  return {
    branchId: (nested.branchId ?? flat.branchId)?.message,
    title: (nested.title ?? flat.title)?.message,
    printApproverId: (nested.printApproverId ?? nested.approver ?? flat.printApproverId)?.message,
  };
}

type Props = {
  value: MembershipFormValues;
  onChange: (value: MembershipFormValues) => void;
  errors?: MembershipErrors;
  seasonId?: number;
  excludeUserId?: number;
  currentBranch?: DirBranch;
  idPrefix?: string;
};

export function MembershipFields({
  value,
  onChange,
  errors = {},
  seasonId,
  excludeUserId,
  currentBranch,
  idPrefix = "m",
}: Props) {
  const branches = useActiveBranches();
  const titles = useTitles();

  const options = (branches.data ?? []).map((b) => ({ value: String(b.id), label: b.name }));
  if (currentBranch && !options.some((o) => o.value === String(currentBranch.id))) {
    options.push({ value: String(currentBranch.id), label: `${currentBranch.name} (pasif)` });
  }
  const set = (patch: Partial<MembershipFormValues>) => onChange({ ...value, ...patch });

  return (
    <div className="space-y-4">
      <FormField label="Şube" htmlFor={`${idPrefix}-branch`} required error={errors.branchId}>
        <SimpleSelect
          id={`${idPrefix}-branch`}
          value={value.branchId}
          onChange={(v) => set({ branchId: v })}
          options={options}
          placeholder="Şube seçin"
          invalid={!!errors.branchId}
        />
      </FormField>

      <FormField label="Unvan" htmlFor={`${idPrefix}-title`} error={errors.title}>
        <Input
          id={`${idPrefix}-title`}
          list={`${idPrefix}-titles`}
          className="h-11 md:h-10"
          placeholder="Örn. Öğretmen"
          value={value.title}
          aria-invalid={!!errors.title}
          onChange={(e) => set({ title: e.target.value })}
        />
        <datalist id={`${idPrefix}-titles`}>
          {(titles.data ?? []).map((t) => (
            <option key={t} value={t} />
          ))}
        </datalist>
      </FormField>

      <FormField
        label="Fotokopi onaylayıcısı"
        htmlFor={`${idPrefix}-approver`}
        hint="Boş bırakılırsa talepleri onaysız olarak fotokopi birimine düşer. Değişiklik yeni taleplere uygulanır."
        error={errors.printApproverId}
      >
        <UserPicker
          id={`${idPrefix}-approver`}
          multiple={false}
          value={value.approver}
          onChange={(u) => set({ approver: u })}
          lookup={{ seasonId }}
          excludeIds={excludeUserId ? [excludeUserId] : []}
          invalid={!!errors.printApproverId}
        />
        {value.approver.length > 0 && (
          <button
            type="button"
            className="min-h-9 text-[13px] text-primary underline-offset-2 hover:underline"
            onClick={() => set({ approver: [] })}
          >
            Onaylayıcıyı kaldır
          </button>
        )}
      </FormField>

      <div>
        <CheckboxField
          id={`${idPrefix}-manager`}
          checked={value.manager}
          onCheckedChange={(v) => set({ manager: v })}
          label="Yönetici"
          hint="Tüm kuruma ve şubelere duyuru ile önemli duyuru yapabilir, grupları ve kurum takvimini yönetir."
        />
        <CheckboxField
          id={`${idPrefix}-print`}
          checked={value.printOperator}
          onCheckedChange={(v) => set({ printOperator: v })}
          label="Fotokopi sorumlusu"
          hint="Kendi şubesinin fotokopi taleplerini hazırlar ve teslim eder."
        />
        <CheckboxField
          id={`${idPrefix}-dms`}
          checked={value.canCreateDmsSpace}
          onCheckedChange={(v) => set({ canCreateDmsSpace: v })}
          label="Doküman alanı açabilir"
          hint="Dokümanlar bölümünde yeni alan açıp yetki dağıtabilir."
        />
      </div>
    </div>
  );
}
