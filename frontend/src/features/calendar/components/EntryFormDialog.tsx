"use client";

import { useState } from "react";
import { addHours, differenceInMinutes, addMinutes, parseISO } from "date-fns";
import { X } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { FileUploader } from "@/components/common/FileUploader";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectGroup, SelectItem, SelectLabel, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api";
import { formatFileSize } from "@/lib/format";
import { useApiForm } from "@/lib/form";
import { useDeleteEntry, useSaveEntry } from "../api";
import { localInputToOffsetIso, toDateInput, toLocalInput } from "../convert";
import { entryFormSchema } from "../schemas";
import type { CalendarDto, CalendarSection, EntryDto, EntryInput } from "../types";

export type EntryDefaults = {
  calendarId?: number;
  allDay?: boolean;
  start: Date;
  end: Date;
};

type Props = {
  calendars: CalendarDto[];
  entry?: EntryDto;
  defaults?: EntryDefaults;
  onClose: () => void;
  onSaved: () => void;
};

const SECTION_TITLES: Record<CalendarSection, string> = {
  INSTITUTION: "Kurum",
  BRANCH: "Şubeler",
  MINE: "Takvimlerim",
  SHARED: "Benimle Paylaşılanlar",
  GROUP: "Gruplarım",
  OTHER_GROUP: "Diğer Grup Takvimleri",
  RESOURCE: "Salonlar ve Laboratuvarlar",
};
const SECTION_ORDER = Object.keys(SECTION_TITLES) as CalendarSection[];

function Dot({ color }: { color: string }) {
  return <span className="inline-block size-2.5 shrink-0 rounded-full" style={{ backgroundColor: color }} aria-hidden />;
}

function Form({ calendars, entry, defaults, onClose, onSaved, onCalendarChange }: Props & { onCalendarChange: (id: number) => void }) {
  const save = useSaveEntry();
  const del = useDeleteEntry();
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [fileId, setFileId] = useState<string>();
  const [removeFile, setRemoveFile] = useState(false);

  const creatable = calendars.filter((c) => c.canCreate);
  const initialCalendarId =
    entry?.calendarId ??
    defaults?.calendarId ??
    creatable.find((c) => c.isDefault)?.id ??
    creatable[0]?.id;

  const start = defaults?.start ?? new Date();
  const end = defaults?.end ?? addHours(start, 1);
  const form = useApiForm(entryFormSchema, {
    defaultValues: {
      calendarId: initialCalendarId ? String(initialCalendarId) : "",
      title: entry?.title ?? "",
      allDay: entry?.allDay ?? defaults?.allDay ?? false,
      startDate: entry?.startDate ?? toDateInput(start),
      endDate: entry?.endDate ?? toDateInput(end),
      startAt: toLocalInput(entry && !entry.allDay ? entry.startAt : start),
      endAt: toLocalInput(entry && !entry.allDay ? entry.endAt : end),
      location: entry?.location ?? "",
      description: entry?.description ?? "",
    },
  });
  const { register, formState, watch, setValue, getValues } = form;
  const allDay = watch("allDay");
  const calendarId = watch("calendarId");
  const selected = calendars.find((c) => String(c.id) === calendarId);
  const resource = selected?.type === "RESOURCE";
  const existingFile = entry?.file && !removeFile && !fileId ? entry.file : undefined;

  function shiftEnd(newStart: string) {
    const prev = getValues("startAt");
    const prevEnd = getValues("endAt");
    if (!newStart || !prev || !prevEnd) return;
    const duration = differenceInMinutes(parseISO(prevEnd), parseISO(prev));
    const minutes = duration > 0 ? duration : 60;
    setValue("endAt", toLocalInput(addMinutes(parseISO(newStart), minutes)), { shouldValidate: formState.isSubmitted });
  }

  const onSubmit = form.submit(async (v) => {
    const input: EntryInput = {
      calendarId: Number(v.calendarId),
      title: v.title,
      allDay: v.allDay,
      ...(v.allDay
        ? { startDate: v.startDate, endDate: v.endDate }
        : { startAt: localInputToOffsetIso(v.startAt), endAt: localInputToOffsetIso(v.endAt) }),
      location: resource ? undefined : v.location || undefined,
      description: v.description || undefined,
      ...(fileId ? { fileId } : {}),
      ...(removeFile && !fileId ? { removeFile: true } : {}),
    };
    try {
      await save.mutateAsync({ id: entry?.id, input });
    } catch (err) {
      if (err instanceof ApiError && err.code === "CAL_RESOURCE_CONFLICT") {
        form.setError(v.allDay ? "endDate" : "endAt", { message: err.message });
        form.setFocus(v.allDay ? "endDate" : "endAt");
        return;
      }
      throw err;
    }
    toast.success(entry ? "Kayıt güncellendi." : resource ? "Rezervasyon yapıldı." : "Kayıt eklendi.");
    onSaved();
    onClose();
  });

  const groups = SECTION_ORDER.map((s) => ({
    section: s,
    items: creatable.filter((c) => c.section === s),
  })).filter((g) => g.items.length > 0);
  const calendarOptions = (entry ? calendars : creatable).map((c) => ({ value: String(c.id), label: c.name }));

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <FormField label="Takvim" htmlFor="entry-calendar" required error={formState.errors.calendarId?.message}>
        <Select
          value={calendarId === "" ? null : calendarId}
          onValueChange={(v) => {
            setValue("calendarId", v ?? "", { shouldValidate: true });
            if (v) onCalendarChange(Number(v));
          }}
          items={calendarOptions}
          disabled={!!entry}
        >
          <SelectTrigger id="entry-calendar" className="h-11 w-full bg-card text-[15px] md:h-10" aria-invalid={!!formState.errors.calendarId}>
            <SelectValue placeholder="Takvim seçin">
              {selected && (
                <span className="flex items-center gap-2">
                  <Dot color={selected.color} />
                  {selected.name}
                </span>
              )}
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            {groups.map((g) => (
              <SelectGroup key={g.section}>
                <SelectLabel>{SECTION_TITLES[g.section]}</SelectLabel>
                {g.items.map((c) => (
                  <SelectItem key={c.id} value={String(c.id)} className="min-h-10">
                    <span className="flex items-center gap-2">
                      <Dot color={c.color} />
                      {c.name}
                    </span>
                  </SelectItem>
                ))}
              </SelectGroup>
            ))}
          </SelectContent>
        </Select>
      </FormField>

      <FormField label="Başlık" htmlFor="entry-title" required error={formState.errors.title?.message}>
        <Input
          id="entry-title"
          className="h-11 md:h-10"
          placeholder="Ör. Veli toplantısı"
          aria-invalid={!!formState.errors.title}
          {...register("title")}
        />
      </FormField>

      <div className="flex min-h-11 items-center justify-between gap-3">
        <Label htmlFor="entry-allday" className="text-[15px] font-medium">
          Tüm gün
        </Label>
        <Switch id="entry-allday" checked={allDay} onCheckedChange={(v) => setValue("allDay", v)} />
      </div>

      {allDay ? (
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Başlangıç tarihi" htmlFor="entry-start-date" required>
            <Input id="entry-start-date" type="date" className="h-11 md:h-10" {...register("startDate")} />
          </FormField>
          <FormField label="Bitiş tarihi" htmlFor="entry-end-date" required error={formState.errors.endDate?.message}>
            <Input
              id="entry-end-date"
              type="date"
              className="h-11 md:h-10"
              aria-invalid={!!formState.errors.endDate}
              {...register("endDate")}
            />
          </FormField>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Başlangıç" htmlFor="entry-start" required>
            <Input
              id="entry-start"
              type="datetime-local"
              className="h-11 md:h-10"
              {...register("startAt", { onChange: (e) => shiftEnd((e.target as HTMLInputElement).value) })}
            />
          </FormField>
          <FormField label="Bitiş" htmlFor="entry-end" required error={formState.errors.endAt?.message}>
            <Input
              id="entry-end"
              type="datetime-local"
              className="h-11 md:h-10"
              aria-invalid={!!formState.errors.endAt}
              {...register("endAt")}
            />
          </FormField>
        </div>
      )}

      {!resource && (
        <FormField label="Konum" htmlFor="entry-location" error={formState.errors.location?.message}>
          <Input id="entry-location" className="h-11 md:h-10" aria-invalid={!!formState.errors.location} {...register("location")} />
        </FormField>
      )}

      <FormField label="Açıklama" htmlFor="entry-desc" error={formState.errors.description?.message}>
        <Textarea id="entry-desc" rows={4} aria-invalid={!!formState.errors.description} {...register("description")} />
      </FormField>

      <div className="space-y-1.5">
        <Label className="text-sm font-medium">Dosya</Label>
        {existingFile && (
          <div className="flex items-center gap-2 rounded-lg border bg-card p-2 pl-3">
            <span className="min-w-0 flex-1 truncate text-[15px]">
              {existingFile.name} · {formatFileSize(existingFile.sizeBytes)}
            </span>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="size-11 md:size-10"
              aria-label="Dosyayı kaldır"
              onClick={() => setRemoveFile(true)}
            >
              <X className="size-4" aria-hidden />
            </Button>
          </div>
        )}
        {!existingFile && (
          <FileUploader
            purpose="CALENDAR"
            onUploadingChange={setUploading}
            onChange={(files) => setFileId(files[0]?.id)}
          />
        )}
      </div>

      <div className="flex flex-col-reverse gap-2 md:flex-row md:items-center">
        {entry?.canEdit && (
          <Button
            type="button"
            variant="destructive"
            className="h-11 md:mr-auto md:h-10"
            disabled={formState.isSubmitting}
            onClick={() => setConfirmDelete(true)}
          >
            Sil
          </Button>
        )}
        <div className="flex-1 md:flex-none">
          <FormActions
            submitLabel="Kaydet"
            submitting={formState.isSubmitting}
            disabled={uploading}
            onCancel={onClose}
          />
        </div>
      </div>

      {entry && (
        <ConfirmDialog
          open={confirmDelete}
          onOpenChange={setConfirmDelete}
          title="Kaydı sil"
          description={`'${entry.title}' silinecek. Bu işlem geri alınamaz.`}
          confirmLabel="Sil"
          destructive
          onConfirm={async () => {
            await del.mutateAsync(entry.id);
            toast.success("Kayıt silindi.");
            onSaved();
            onClose();
          }}
        />
      )}
    </form>
  );
}

export function EntryFormDialog(props: Props) {
  const [calendarId, setCalendarId] = useState<number | undefined>(
    props.entry?.calendarId ??
      props.defaults?.calendarId ??
      props.calendars.find((c) => c.canCreate && c.isDefault)?.id,
  );
  const resource = props.calendars.find((c) => c.id === calendarId)?.type === "RESOURCE";
  const title = props.entry
    ? resource
      ? "Rezervasyonu Düzenle"
      : "Kaydı Düzenle"
    : resource
      ? "Yeni Rezervasyon"
      : "Yeni Kayıt";
  return (
    <ResponsiveDialog open onOpenChange={(o) => !o && props.onClose()} title={title}>
      <Form {...props} onCalendarChange={setCalendarId} />
    </ResponsiveDialog>
  );
}

