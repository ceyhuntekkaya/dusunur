"use client";

import { useState } from "react";
import { Calendar, Download, Eye, FileText, MapPin } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { DateText } from "@/components/common/DateText";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { Button } from "@/components/ui/button";
import { extensionOf } from "@/lib/files";
import { formatFileSize } from "@/lib/format";
import { linkify } from "@/lib/linkify";
import { useDeleteEntry, entryFileUrl } from "../api";
import { entryTimeText } from "../convert";
import type { CalendarDto, EntryDto } from "../types";

type Props = {
  entry: EntryDto;
  calendar?: CalendarDto;
  onClose: () => void;
  onEdit: () => void;
  onDeleted: () => void;
};

const PREVIEWABLE = ["pdf", "jpg", "jpeg", "png"];

export function EntryDetailDialog({ entry, calendar, onClose, onEdit, onDeleted }: Props) {
  const del = useDeleteEntry();
  const [confirm, setConfirm] = useState(false);
  const color = calendar?.color ?? "#2453C7";
  const file = entry.file;

  return (
    <ResponsiveDialog open onOpenChange={(o) => !o && onClose()} title={entry.title}>
      <div className="space-y-4">
        <div className="h-1 rounded-full" style={{ backgroundColor: color }} aria-hidden />
        <ul className="space-y-3 text-[15px]">
          <li className="flex items-start gap-3">
            <Calendar className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden />
            <span>{entryTimeText(entry)}</span>
          </li>
          {entry.location && (
            <li className="flex items-start gap-3">
              <MapPin className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden />
              <span>{entry.location}</span>
            </li>
          )}
          {calendar && (
            <li className="flex items-center gap-3">
              <span className="mx-[3px] inline-block size-2.5 shrink-0 rounded-full" style={{ backgroundColor: color }} aria-hidden />
              <span>{calendar.name}</span>
            </li>
          )}
        </ul>
        {entry.description && <p className="text-[15px] whitespace-pre-wrap">{linkify(entry.description)}</p>}
        {file && (
          <div className="flex flex-wrap items-center gap-2 rounded-lg border bg-card p-3">
            <FileText className="size-5 shrink-0 text-muted-foreground" aria-hidden />
            <div className="min-w-0 flex-1">
              <p className="truncate text-[15px]">{file.name}</p>
              <p className="text-[13px] text-muted-foreground">{formatFileSize(file.sizeBytes)}</p>
            </div>
            {PREVIEWABLE.includes(extensionOf(file.name)) && (
              <Button
                variant="outline"
                className="h-11 md:h-10"
                render={<a href={entryFileUrl(entry.id)} target="_blank" rel="noopener noreferrer" />}
                nativeButton={false}
              >
                <Eye aria-hidden />
                Önizle
              </Button>
            )}
            <Button
              variant="outline"
              className="h-11 md:h-10"
              render={<a href={entryFileUrl(entry.id, true)} />}
              nativeButton={false}
            >
              <Download aria-hidden />
              İndir
            </Button>
          </div>
        )}
        <p className="text-[13px] text-muted-foreground">
          {entry.createdBy.fullName} ekledi · <DateText value={entry.createdAt} />
        </p>
        <div className="flex flex-col-reverse gap-2 pt-1 md:flex-row md:justify-end">
          {entry.canEdit && (
            <Button variant="destructive" className="h-11 md:mr-auto md:h-10" onClick={() => setConfirm(true)}>
              Sil
            </Button>
          )}
          {entry.canEdit && (
            <Button variant="outline" className="h-11 md:h-10" onClick={onEdit}>
              Düzenle
            </Button>
          )}
          <Button className="h-11 md:h-10" onClick={onClose}>
            Kapat
          </Button>
        </div>
      </div>
      <ConfirmDialog
        open={confirm}
        onOpenChange={setConfirm}
        title="Kaydı sil"
        description={`'${entry.title}' silinecek. Bu işlem geri alınamaz.`}
        confirmLabel="Sil"
        destructive
        onConfirm={async () => {
          await del.mutateAsync(entry.id);
          toast.success("Kayıt silindi.");
          onDeleted();
          onClose();
        }}
      />
    </ResponsiveDialog>
  );
}
