"use client";

import { useState } from "react";
import { Download } from "lucide-react";
import { toast } from "sonner";
import { DetailList } from "@/components/common/DetailList";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { SimpleSelect } from "@/components/common/SimpleSelect";
import { Input } from "@/components/ui/input";
import { useImportUsers, useSeasons } from "@/features/core/admin-api";
import { ApiError } from "@/lib/api";

const COLUMNS: [string, string][] = [
  ["ad *", "Kişinin adı"],
  ["soyad *", "Kişinin soyadı"],
  ["eposta *", "E-posta adresi"],
  ["telefon", "05XX XXX XX XX biçiminde"],
  ["brans", "Örn. Matematik"],
  ["sube *", "Şube adıyla birebir aynı olmalı"],
  ["unvan", "Örn. Öğretmen"],
];

const FILE_MESSAGE = "Lütfen .csv uzantılı ve en fazla 2 MB boyutunda bir dosya seçin.";

function Body({ onClose }: { onClose: () => void }) {
  const seasons = useSeasons();
  const importUsers = useImportUsers();
  const [seasonId, setSeasonId] = useState<string>("");
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string>();
  const [details, setDetails] = useState<string[]>([]);
  const [pending, setPending] = useState(false);

  const open = (seasons.data ?? []).filter((s) => s.status !== "CLOSED");
  const active = open.find((s) => s.status === "ACTIVE");
  const selected = seasonId || (active ? String(active.id) : open[0] ? String(open[0].id) : "");
  const label = (s: (typeof open)[number]) => `${s.name} (${s.status === "ACTIVE" ? "Aktif" : "Hazırlık"})`;

  function pick(f: File | undefined) {
    setDetails([]);
    if (!f) {
      setFile(null);
      return;
    }
    if (!f.name.toLowerCase().endsWith(".csv") || f.size > 2 * 1024 * 1024) {
      setFile(null);
      setFileError(FILE_MESSAGE);
      return;
    }
    setFileError(undefined);
    setFile(f);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!file) {
      setFileError("Bir CSV dosyası seçin.");
      return;
    }
    setPending(true);
    setDetails([]);
    try {
      const res = await importUsers.mutateAsync({ file, seasonId: Number(selected) });
      toast.success(
        `${res.created} personel eklendi. Şifre belirleme e-postaları gönderiliyor; bitince bildirim alacaksınız.`,
      );
      onClose();
    } catch (err) {
      if (err instanceof ApiError && err.code === "IMPORT_INVALID") setDetails(err.details);
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4" noValidate>
      <p className="text-sm">
        Personeli Excel&apos;den hazırlayacağınız bir CSV dosyasıyla toplu ekleyebilirsiniz.{" "}
        <a
          href="/api/admin/users/import-template"
          download
          className="inline-flex items-center gap-1 font-medium text-primary underline-offset-2 hover:underline"
        >
          <Download className="size-3.5" aria-hidden />
          Şablonu indir
        </a>
      </p>
      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-[13px]">
          <tbody>
            {COLUMNS.map(([name, desc]) => (
              <tr key={name} className="border-b last:border-0">
                <td className="px-3 py-1.5 font-mono font-medium">{name}</td>
                <td className="px-3 py-1.5 text-muted-foreground">{desc}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="text-[13px] text-muted-foreground">
        Excel&apos;de &apos;Farklı Kaydet → CSV UTF-8&apos; ya da &apos;CSV (noktalı virgülle ayrılmış)&apos;
        seçin.
      </p>
      <FormField label="Eğitim-öğretim yılı" htmlFor="imp-season" required>
        <SimpleSelect
          id="imp-season"
          value={selected}
          onChange={setSeasonId}
          options={open.map((s) => ({ value: String(s.id), label: label(s) }))}
          placeholder="Yıl seçin"
        />
      </FormField>
      <FormField label="CSV dosyası" htmlFor="imp-file" required error={fileError}>
        <Input
          id="imp-file"
          type="file"
          accept=".csv"
          className="h-11 py-2 md:h-10"
          aria-invalid={!!fileError}
          onChange={(e) => pick(e.target.files?.[0])}
        />
      </FormField>
      <DetailList items={details} />
      <FormActions
        submitLabel="Kontrol Et ve Yükle"
        submitting={pending}
        disabled={!selected}
        onCancel={onClose}
      />
    </form>
  );
}

export function ImportUsersDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (o: boolean) => void }) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title="Toplu Personel Yükle">
      <Body onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
