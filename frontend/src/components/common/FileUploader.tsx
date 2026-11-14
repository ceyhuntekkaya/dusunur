"use client";

import { useEffect, useRef, useState } from "react";
import { FileText, Loader2, Paperclip, Upload, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { apiUpload, ApiError } from "@/lib/api";
import type { FilePurpose, UploadedFileDto } from "@/features/core/types";
import { extensionOf, maxSizeMbFor, PURPOSE_EXTENSIONS } from "@/lib/files";
import { formatFileSize } from "@/lib/format";
import { toastError } from "@/lib/query-client";
import { cn } from "@/lib/utils";

type Props = {
  purpose: FilePurpose;
  multiple?: boolean;
  accept?: string[];
  maxSizeMb?: number;
  compressImages?: boolean;
  maxFiles?: number;
  maxFilesMessage?: string;
  compact?: boolean;
  onUploadingChange?: (uploading: boolean) => void;
  onChange: (files: UploadedFileDto[]) => void;
  className?: string;
};

type Row = {
  key: number;
  name: string;
  size: number;
  progress: number;
  status: "uploading" | "done" | "error";
  error?: string;
  preview?: string;
  result?: UploadedFileDto;
  abort: AbortController;
};

const COMPRESSIBLE = ["jpg", "jpeg", "png", "webp"];

async function maybeCompress(file: File): Promise<File> {
  const { default: imageCompression } = await import("browser-image-compression");
  const blob = await imageCompression(file, {
    maxWidthOrHeight: 1920,
    initialQuality: 0.8,
    fileType: "image/jpeg",
    useWebWorker: true,
  });
  const base = file.name.replace(/\.[^.]+$/, "");
  return new File([blob], `${base}.jpg`, { type: "image/jpeg" });
}

export function FileUploader({
  purpose,
  multiple = false,
  accept,
  maxSizeMb,
  compressImages = false,
  maxFiles,
  maxFilesMessage,
  compact = false,
  onUploadingChange,
  onChange,
  className,
}: Props) {
  const [rows, setRows] = useState<Row[]>([]);
  const [dragging, setDragging] = useState(false);
  const input = useRef<HTMLInputElement>(null);
  const counter = useRef(0);
  const onChangeRef = useRef(onChange);
  useEffect(() => {
    onChangeRef.current = onChange;
  });

  const allowed = (accept ?? PURPOSE_EXTENSIONS[purpose]).map((e) => e.replace(/^\./, "").toLowerCase());
  const doneKey = rows
    .filter((r) => r.status === "done")
    .map((r) => r.result?.id)
    .join(",");
  const firstRun = useRef(true);
  useEffect(() => {
    if (firstRun.current) {
      firstRun.current = false;
      return;
    }
    onChangeRef.current(rows.flatMap((r) => (r.status === "done" && r.result ? [r.result] : [])));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [doneKey]);

  const uploading = rows.some((r) => r.status === "uploading");
  const onUploadingRef = useRef(onUploadingChange);
  useEffect(() => {
    onUploadingRef.current = onUploadingChange;
  });
  useEffect(() => {
    onUploadingRef.current?.(uploading);
  }, [uploading]);

  const previews = useRef(new Map<number, string>());
  useEffect(() => {
    const map = previews.current;
    return () => map.forEach((u) => URL.revokeObjectURL(u));
  }, []);

  function patch(key: number, change: Partial<Row>) {
    setRows((prev) => prev.map((r) => (r.key === key ? { ...r, ...change } : r)));
  }

  function validate(file: File): string | undefined {
    const ext = extensionOf(file.name);
    if (!allowed.includes(ext)) {
      return `"${file.name}" bu dosya türüne izin verilmiyor. İzin verilen türler: ${allowed.join(", ")}.`;
    }
    const limit = maxSizeMb ?? maxSizeMbFor(purpose, ext);
    if (file.size > limit * 1024 * 1024) {
      return `"${file.name}" çok büyük. En fazla ${limit} MB yükleyebilirsiniz.`;
    }
    if (file.size === 0) return `"${file.name}" boş bir dosya. Başka bir dosya seçin.`;
    return undefined;
  }

  async function upload(file: File) {
    const key = ++counter.current;
    const abort = new AbortController();
    const problem = validate(file);
    if (problem) {
      setRows((prev) => [
        ...prev,
        { key, name: file.name, size: file.size, progress: 0, status: "error", error: problem, abort },
      ]);
      toastError(problem);
      return;
    }
    let preview: string | undefined;
    if (compact && file.type.startsWith("image/")) {
      preview = URL.createObjectURL(file);
      previews.current.set(key, preview);
    }
    setRows((prev) => [
      ...(multiple ? prev : []),
      { key, name: file.name, size: file.size, progress: 0, status: "uploading", preview, abort },
    ]);
    try {
      const ext = extensionOf(file.name);
      const body = compressImages && COMPRESSIBLE.includes(ext) ? await maybeCompress(file) : file;
      const form = new FormData();
      form.append("file", body, body.name);
      form.append("purpose", purpose);
      const result = await apiUpload<UploadedFileDto>("/api/files", form, {
        signal: abort.signal,
        onProgress: (p) => patch(key, { progress: p }),
      });
      patch(key, { status: "done", progress: 100, result });
    } catch (err) {
      if (err instanceof DOMException && err.name === "AbortError") return;
      const message =
        err instanceof ApiError ? err.message : "Dosya yüklenemedi. Lütfen tekrar deneyin.";
      patch(key, { status: "error", error: message });
      toastError(message);
    }
  }

  function handleFiles(list: FileList | null) {
    if (!list) return;
    let files = Array.from(list);
    if (maxFiles !== undefined) {
      const room = Math.max(0, maxFiles - rows.filter((r) => r.status !== "error").length);
      if (files.length > room) {
        toastError(maxFilesMessage ?? `Bir paylaşıma en fazla ${maxFiles} dosya eklenebilir.`);
        files = files.slice(0, room);
      }
    }
    (multiple ? files : files.slice(0, 1)).forEach((f) => void upload(f));
    if (input.current) input.current.value = "";
  }

  function remove(row: Row) {
    row.abort.abort();
    const url = previews.current.get(row.key);
    if (url) {
      URL.revokeObjectURL(url);
      previews.current.delete(row.key);
    }
    setRows((prev) => prev.filter((r) => r.key !== row.key));
  }

  if (compact) {
    return (
      <div className="contents">
        {rows.length > 0 && (
          <ul className="order-first flex w-full flex-wrap gap-2">
            {rows.map((r) => (
              <li
                key={r.key}
                className={cn(
                  "relative overflow-hidden rounded-lg border bg-card",
                  r.preview ? "size-16" : "flex h-10 max-w-48 items-center gap-2 pr-8 pl-2",
                  r.status === "error" && "border-destructive",
                )}
                title={r.status === "error" ? r.error : r.name}
              >
                {r.preview ? (
                   
                  <img src={r.preview} alt={r.name} className="size-full object-cover" />
                ) : (
                  <>
                    <FileText className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                    <span className="truncate text-xs">{r.name}</span>
                  </>
                )}
                {r.status === "uploading" && (
                  <Progress
                    value={r.progress}
                    className="absolute inset-x-0 bottom-0 gap-0"
                    aria-label={`${r.name} yükleniyor`}
                  />
                )}
                <button
                  type="button"
                  aria-label={`${r.name} dosyasını kaldır`}
                  className="absolute top-0.5 right-0.5 flex size-6 items-center justify-center rounded-full bg-black/60 text-white outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  onClick={() => remove(r)}
                >
                  <X className="size-3.5" aria-hidden />
                </button>
              </li>
            ))}
          </ul>
        )}
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="size-11 shrink-0 rounded-full"
          aria-label="Dosya ekle"
          onClick={() => input.current?.click()}
        >
          <Paperclip className="size-5" aria-hidden />
        </Button>
        <input
          ref={input}
          type="file"
          hidden
          multiple={multiple}
          accept={allowed.map((e) => `.${e}`).join(",")}
          onChange={(e) => handleFiles(e.target.files)}
        />
      </div>
    );
  }

  return (
    <div className={cn("space-y-3", className)}>
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragging(false);
          handleFiles(e.dataTransfer.files);
        }}
        className={cn(
          "flex flex-col items-center gap-2 rounded-xl border-2 border-dashed p-4 text-center transition-colors",
          dragging ? "border-primary bg-accent" : "border-border",
        )}
      >
        <Upload className="hidden size-5 text-muted-foreground md:block" aria-hidden />
        <p className="hidden text-[13px] text-muted-foreground md:block">
          Dosyaları buraya sürükleyin veya
        </p>
        <Button type="button" variant="outline" className="h-11 md:h-10" onClick={() => input.current?.click()}>
          Dosya seç
        </Button>
        <input
          ref={input}
          type="file"
          hidden
          multiple={multiple}
          accept={allowed.map((e) => `.${e}`).join(",")}
          onChange={(e) => handleFiles(e.target.files)}
        />
      </div>

      {rows.length > 0 && (
        <ul className="space-y-2">
          {rows.map((r) => (
            <li key={r.key} className="rounded-lg border bg-card p-3">
              <div className="flex items-center gap-3">
                <FileText className="size-5 shrink-0 text-muted-foreground" aria-hidden />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-[15px]">{r.name}</p>
                  <p className="text-xs text-muted-foreground">{formatFileSize(r.size)}</p>
                </div>
                {r.status === "uploading" && (
                  <Loader2 className="size-4 animate-spin text-muted-foreground" aria-label="Yükleniyor" />
                )}
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="size-11 md:size-9"
                  aria-label={`${r.name} dosyasını kaldır`}
                  onClick={() => remove(r)}
                >
                  <X className="size-4" aria-hidden />
                </Button>
              </div>
              {r.status === "uploading" && (
                <Progress value={r.progress} className="mt-2 gap-0" aria-label={`${r.name} yükleniyor`} />
              )}
              {r.status === "error" && (
                <p className="mt-1 text-[13px] text-destructive" role="alert">
                  {r.error}
                </p>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
