"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { addDays, addMinutes, format, setHours, startOfDay } from "date-fns";
import { Minus, Plus } from "lucide-react";
import { Controller } from "react-hook-form";
import { toast } from "sonner";
import { CheckboxField } from "@/components/common/CheckboxField";
import { FileUploader } from "@/components/common/FileUploader";
import { FormField } from "@/components/common/FormField";
import { SegmentedControl } from "@/components/common/SegmentedControl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import type { UploadedFileDto } from "@/features/core/types";
import { extensionOf } from "@/lib/files";
import { formatFullDate } from "@/lib/format";
import { useApiForm } from "@/lib/form";
import { cn } from "@/lib/utils";
import { useCreatePrintRequest, usePageCount } from "../api";
import { MAX_TOTAL_MESSAGE, sheetCount } from "../labels";
import { printRequestSchema, type PrintRequestFormIn } from "../schemas";
import type { PrintContext, PrintRequestDetail } from "../types";

const LOCAL_FORMAT = "yyyy-MM-dd'T'HH:mm";

function minDue(ctx: PrintContext): Date {
  const d = addMinutes(new Date(), ctx.minLeadMinutes);
  d.setSeconds(0, 0);
  d.setMinutes(Math.ceil(d.getMinutes() / 5) * 5);
  return d;
}

function defaultDue(ctx: PrintContext): string {
  const tomorrow = setHours(startOfDay(addDays(new Date(), 1)), 8);
  const min = minDue(ctx);
  return format(tomorrow < min ? min : tomorrow, LOCAL_FORMAT);
}

function NumberStepper({
  id,
  value,
  onChange,
  min,
  max,
  invalid,
  label,
}: {
  id: string;
  value: number;
  onChange: (v: number) => void;
  min: number;
  max: number;
  invalid: boolean;
  label: string;
}) {
  const current = Number.isNaN(value) ? min - 1 : value;
  return (
    <div className="flex items-center gap-1.5">
      <Button
        type="button"
        variant="outline"
        className="size-11 shrink-0 md:size-10"
        aria-label={`${label} azalt`}
        disabled={current <= min}
        onClick={() => onChange(Math.max(min, current - 1))}
      >
        <Minus aria-hidden />
      </Button>
      <Input
        id={id}
        inputMode="numeric"
        className="h-11 min-w-0 text-center md:h-10"
        aria-invalid={invalid}
        value={Number.isNaN(value) ? "" : String(value)}
        onChange={(e) => {
          const digits = e.target.value.replace(/\D/g, "");
          onChange(digits === "" ? NaN : Number(digits));
        }}
      />
      <Button
        type="button"
        variant="outline"
        className="size-11 shrink-0 md:size-10"
        aria-label={`${label} artır`}
        disabled={current >= max}
        onClick={() => onChange(Math.min(max, current + 1))}
      >
        <Plus aria-hidden />
      </Button>
    </div>
  );
}

function ContextBox({ ctx }: { ctx: PrintContext }) {
  let content: React.ReactNode;
  if (ctx.myApproverOnLeave) {
    const l = ctx.myApproverOnLeave;
    content = (
      <>
        Onaylayıcınız <b>{l.approver.fullName}</b> {formatFullDate(l.endDate)} tarihine kadar izinde; talebiniz vekili{" "}
        <b>{l.delegate.fullName}</b> kişisine gidecek.
      </>
    );
  } else if (ctx.myApprover) {
    content = (
      <>
        Talebiniz onay için <b>{ctx.myApprover.fullName}</b> kişisine gönderilecek.
      </>
    );
  } else {
    content = (
      <>
        Talebiniz doğrudan <b>{ctx.branchName}</b> fotokopi birimine iletilecek.
      </>
    );
  }
  return <div className="rounded-lg bg-info-bg px-4 py-3 text-sm">{content}</div>;
}

type Props = { ctx: PrintContext; source?: PrintRequestDetail };

export function PrintRequestForm({ ctx, source }: Props) {
  const router = useRouter();
  const create = useCreatePrintRequest();
  const pageCount = usePageCount();
  const [uploading, setUploading] = useState(false);
  const [pageFromFile, setPageFromFile] = useState(false);
  const schema = useMemo(
    () => printRequestSchema({ minLeadMinutes: ctx.minLeadMinutes, maxDaysAhead: ctx.maxDaysAhead }),
    [ctx.minLeadMinutes, ctx.maxDaysAhead],
  );
  const form = useApiForm(schema, {
    defaultValues: {
      title: source?.title ?? "",
      fileId: undefined,
      originalOnPaper: source?.originalOnPaper ?? false,
      pageCount: source?.pageCount ?? 1,
      copies: source?.copies ?? 1,
      color: source?.color ?? "BW",
      sides: source?.sides ?? "SINGLE",
      paperSize: source?.paperSize ?? "A4",
      secret: source?.secret ?? false,
      dueAt: defaultDue(ctx),
      note: source?.note ?? "",
    } satisfies PrintRequestFormIn,
  });
  const { control, register, formState, watch, setValue } = form;
  const { errors } = formState;

  const originalOnPaper = watch("originalOnPaper");
  const pages = watch("pageCount");
  const copies = watch("copies");
  const sides = watch("sides");
  const valid = Number.isInteger(pages) && Number.isInteger(copies) && pages >= 1 && copies >= 1;
  const total = valid ? pages * copies : 0;
  const tooLarge = total > 20000;

  function onFiles(files: UploadedFileDto[]) {
    const file = files[0];
    setValue("fileId", file?.id, { shouldValidate: formState.isSubmitted, shouldDirty: true });
    if (!file) {
      setPageFromFile(false);
      return;
    }
    if (extensionOf(file.originalName) !== "pdf") return;
    pageCount.mutate(file.id, {
      onSuccess: (r) => {
        if (r.pageCount === null) {
          toast.warning("PDF'in sayfa sayısı okunamadı; lütfen elle girin.");
          return;
        }
        setValue("pageCount", r.pageCount, { shouldValidate: true, shouldDirty: true });
        setPageFromFile(true);
      },
    });
  }

  const onSubmit = form.submit(async (v) => {
    const detail = await create.mutateAsync({
      title: v.title,
      fileId: v.originalOnPaper ? undefined : v.fileId,
      originalOnPaper: v.originalOnPaper,
      pageCount: v.pageCount,
      copies: v.copies,
      color: v.color,
      sides: v.sides,
      paperSize: v.paperSize,
      secret: v.secret,
      dueAt: new Date(v.dueAt).toISOString(),
      note: v.note === "" ? undefined : v.note,
      copiedFromId: source?.id,
    });
    toast.success(
      detail.status === "PENDING_APPROVAL" && detail.approver
        ? `Talebiniz ${detail.approver.fullName} onayına gönderildi.`
        : "Talebiniz fotokopi birimine iletildi.",
    );
    router.push(`/fotokopi/${detail.id}`);
  });

  const minValue = format(minDue(ctx), LOCAL_FORMAT);

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-6">
      <ContextBox ctx={ctx} />

      {source && (
        <div className="rounded-lg bg-warning-bg px-4 py-3 text-sm">
          <p>
            Önceki talepten kopyalandı.
            {source.file && !source.originalOnPaper && ` Önceki dosya: ${source.file.name} — dosyayı yeniden yükleyin.`}
          </p>
          {source.status === "REJECTED" && source.rejectReason && (
            <p className="mt-1">Red gerekçesi: {source.rejectReason}</p>
          )}
        </div>
      )}

      <section className="space-y-3">
        <h2 className="text-[15px] font-semibold">Dosya</h2>
        {!originalOnPaper && (
          <FormField label="Dosya" htmlFor="print-file" error={errors.fileId?.message}>
            <FileUploader purpose="PRINT" onUploadingChange={setUploading} onChange={onFiles} />
          </FormField>
        )}
        <Controller
          control={control}
          name="originalOnPaper"
          render={({ field }) => (
            <CheckboxField
              id="original-on-paper"
              label="Basılı orijinali fotokopi birimine getireceğim"
              checked={field.value}
              onCheckedChange={(checked) => {
                field.onChange(checked);
                if (checked) {
                  setValue("fileId", undefined);
                  setUploading(false);
                  setPageFromFile(false);
                }
              }}
            />
          )}
        />
      </section>

      <section className="space-y-4">
        <h2 className="text-[15px] font-semibold">Baskı bilgileri</h2>
        <FormField label="Açıklama" htmlFor="print-title" required error={errors.title?.message}>
          <Input
            id="print-title"
            className="h-11 md:h-10"
            maxLength={200}
            placeholder="Ör. 9-A Matematik 2. ünite çalışma kâğıdı"
            aria-invalid={!!errors.title}
            {...register("title")}
          />
        </FormField>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField
            label="Sayfa sayısı"
            htmlFor="print-pages"
            required
            error={errors.pageCount?.message}
            hint={pageFromFile ? "Sayfa sayısı dosyadan okundu. Gerekirse değiştirin." : undefined}
          >
            <Controller
              control={control}
              name="pageCount"
              render={({ field }) => (
                <NumberStepper
                  id="print-pages"
                  label="Sayfa sayısı"
                  value={field.value}
                  min={1}
                  max={2000}
                  invalid={!!errors.pageCount}
                  onChange={(v) => {
                    setPageFromFile(false);
                    field.onChange(v);
                  }}
                />
              )}
            />
          </FormField>
          <FormField label="Kopya adedi" htmlFor="print-copies" required error={errors.copies?.message}>
            <Controller
              control={control}
              name="copies"
              render={({ field }) => (
                <NumberStepper
                  id="print-copies"
                  label="Kopya adedi"
                  value={field.value}
                  min={1}
                  max={1000}
                  invalid={!!errors.copies}
                  onChange={field.onChange}
                />
              )}
            />
          </FormField>
        </div>

        <FormField label="Renk" htmlFor="print-color" error={errors.color?.message}>
          <Controller
            control={control}
            name="color"
            render={({ field }) => (
              <SegmentedControl
                ariaLabel="Renk"
                value={field.value}
                onChange={field.onChange}
                options={[
                  { value: "BW", label: "Siyah-Beyaz" },
                  { value: "COLOR", label: "Renkli" },
                ]}
              />
            )}
          />
        </FormField>
        <FormField label="Baskı yüzü" htmlFor="print-sides" error={errors.sides?.message}>
          <Controller
            control={control}
            name="sides"
            render={({ field }) => (
              <SegmentedControl
                ariaLabel="Baskı yüzü"
                value={field.value}
                onChange={field.onChange}
                options={[
                  { value: "SINGLE", label: "Tek yüz" },
                  { value: "DOUBLE", label: "Çift yüz" },
                ]}
              />
            )}
          />
        </FormField>
        <FormField label="Kâğıt" htmlFor="print-paper" error={errors.paperSize?.message}>
          <Controller
            control={control}
            name="paperSize"
            render={({ field }) => (
              <SegmentedControl
                ariaLabel="Kâğıt"
                value={field.value}
                onChange={field.onChange}
                options={[
                  { value: "A4", label: "A4" },
                  { value: "A3", label: "A3" },
                ]}
              />
            )}
          />
        </FormField>

        <div
          className={cn(
            "rounded-lg px-4 py-3 text-[15px]",
            tooLarge ? "bg-destructive/10 text-destructive" : "bg-muted",
          )}
          aria-live="polite"
        >
          {valid ? (
            <>
              Toplam <b>{total.toLocaleString("tr-TR")} baskı</b> · {sheetCount(pages, copies, sides).toLocaleString("tr-TR")}{" "}
              yaprak
              {tooLarge && <p className="mt-1 text-[13px]">{MAX_TOTAL_MESSAGE}</p>}
            </>
          ) : (
            "Sayfa ve kopya sayısını girin."
          )}
        </div>
      </section>

      <section className="space-y-4">
        <h2 className="text-[15px] font-semibold">Teslim</h2>
        <FormField
          label="İstenen teslim tarihi ve saati"
          htmlFor="print-due"
          required
          error={errors.dueAt?.message}
          hint={`En erken ${ctx.minLeadMinutes} dakika sonrası seçilebilir.`}
        >
          <Input
            id="print-due"
            type="datetime-local"
            min={minValue}
            className="h-11 md:h-10"
            aria-invalid={!!errors.dueAt}
            {...register("dueAt")}
          />
        </FormField>
        <FormField label="Not" htmlFor="print-note" error={errors.note?.message}>
          <Textarea
            id="print-note"
            rows={3}
            maxLength={500}
            placeholder="Ör. Yalnızca 2–5. sayfalar, zımbalı olsun."
            aria-invalid={!!errors.note}
            {...register("note")}
          />
        </FormField>
      </section>

      <section>
        <h2 className="mb-1 text-[15px] font-semibold">Gizlilik</h2>
        <Controller
          control={control}
          name="secret"
          render={({ field }) => (
            <CheckboxField
              id="print-secret"
              label="Gizli evrak (sınav vb.)"
              hint="Dosyayı yalnızca siz, onaylayıcınız ve fotokopi sorumlusu görebilir; listelerde açıklama 'Gizli evrak' olarak görünür."
              checked={field.value}
              onCheckedChange={field.onChange}
            />
          )}
        />
      </section>

      <Button type="submit" className="h-11 w-full" disabled={formState.isSubmitting || uploading}>
        Talebi Gönder
      </Button>
    </form>
  );
}
