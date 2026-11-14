"use client";

import { useEffect, useState } from "react";
import { ChevronLeft, ChevronRight, Download, File, FileArchive, FileSpreadsheet, FileText, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { extensionOf } from "@/lib/files";
import { formatFileSize } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { FeedAttachment } from "../types";

const fileUrl = (postId: number, fileId: string, download = false) =>
  `/api/feed/posts/${postId}/files/${fileId}${download ? "?download=true" : ""}`;

function FileIcon({ name }: { name: string }) {
  const ext = extensionOf(name);
  const cls = "size-5 shrink-0 text-muted-foreground";
  if (["xls", "xlsx", "ods", "csv"].includes(ext)) return <FileSpreadsheet className={cls} aria-hidden />;
  if (["zip", "rar", "7z"].includes(ext)) return <FileArchive className={cls} aria-hidden />;
  if (["pdf", "doc", "docx", "odt", "txt", "ppt", "pptx", "odp"].includes(ext))
    return <FileText className={cls} aria-hidden />;
  return <File className={cls} aria-hidden />;
}

function ImageViewer({
  postId,
  images,
  index,
  onIndexChange,
  onClose,
}: {
  postId: number;
  images: FeedAttachment[];
  index: number | null;
  onIndexChange: (i: number) => void;
  onClose: () => void;
}) {
  const open = index !== null;
  const n = images.length;

  useEffect(() => {
    if (index === null) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "ArrowLeft") onIndexChange((index! - 1 + n) % n);
      if (e.key === "ArrowRight") onIndexChange((index! + 1) % n);
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [index, n, onIndexChange]);

  const current = index !== null ? images[index] : undefined;
  const prev = () => onIndexChange((index! - 1 + n) % n);
  const next = () => onIndexChange((index! + 1) % n);

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent
        showCloseButton={false}
        className="top-0 left-0 h-dvh max-h-none w-screen max-w-none translate-x-0 translate-y-0 rounded-none bg-black p-0 text-white ring-0 sm:max-w-none"
      >
        <DialogTitle className="sr-only">{current?.name ?? "Görsel"}</DialogTitle>
        {current && (
          <div className="relative flex h-full w-full items-center justify-center">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={fileUrl(postId, current.fileId)} alt={current.name} className="max-h-full max-w-full object-contain" />
            {n > 1 && (
              <>
                <button
                  type="button"
                  aria-label="Önceki görsel"
                  className="absolute inset-y-0 left-0 flex w-1/3 items-center justify-start pl-2 outline-none md:w-20"
                  onClick={prev}
                >
                  <ChevronLeft className="hidden size-8 rounded-full bg-black/50 md:block" aria-hidden />
                </button>
                <button
                  type="button"
                  aria-label="Sonraki görsel"
                  className="absolute inset-y-0 right-0 flex w-1/3 items-center justify-end pr-2 outline-none md:w-20"
                  onClick={next}
                >
                  <ChevronRight className="hidden size-8 rounded-full bg-black/50 md:block" aria-hidden />
                </button>
              </>
            )}
            <div className="absolute inset-x-0 top-0 flex items-center justify-between gap-2 bg-gradient-to-b from-black/70 to-transparent p-3">
              <span className="text-sm">
                {index! + 1} / {n}
              </span>
              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  className="h-11 md:h-10"
                  nativeButton={false}
                  render={<a href={fileUrl(postId, current.fileId, true)} download />}
                >
                  <Download aria-hidden />
                  İndir
                </Button>
                <Button variant="secondary" className="h-11 md:h-10" onClick={onClose}>
                  <X aria-hidden />
                  Kapat
                </Button>
              </div>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

function ImageGrid({
  postId,
  images,
  onOpen,
}: {
  postId: number;
  images: FeedAttachment[];
  onOpen: (i: number) => void;
}) {
  const cell = (img: FeedAttachment, i: number, className: string, overlay?: number) => (
    <button
      key={img.fileId}
      type="button"
      aria-label={`${img.name} görselini büyüt`}
      onClick={() => onOpen(i)}
      className={cn("relative overflow-hidden rounded-lg bg-muted outline-none focus-visible:ring-2 focus-visible:ring-ring", className)}
    >
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={fileUrl(postId, img.fileId)}
        alt={img.name}
        loading="lazy"
        className="size-full object-cover"
      />
      {overlay !== undefined && overlay > 0 && (
        <span className="absolute inset-0 flex items-center justify-center bg-black/50 text-2xl font-semibold text-white">
          +{overlay}
        </span>
      )}
    </button>
  );

  if (images.length === 1) {
    const img = images[0];
    return (
      <button
        type="button"
        aria-label={`${img.name} görselini büyüt`}
        onClick={() => onOpen(0)}
        className="block w-full overflow-hidden rounded-lg bg-muted outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={fileUrl(postId, img.fileId)}
          alt={img.name}
          loading="lazy"
          className="mx-auto max-h-[480px] w-full rounded-lg object-contain"
        />
      </button>
    );
  }
  if (images.length === 2) {
    return <div className="grid grid-cols-2 gap-1">{images.map((m, i) => cell(m, i, "aspect-square"))}</div>;
  }
  if (images.length === 3) {
    return (
      <div className="grid aspect-[4/3] grid-cols-2 grid-rows-2 gap-1">
        {cell(images[0], 0, "row-span-2")}
        {cell(images[1], 1, "")}
        {cell(images[2], 2, "")}
      </div>
    );
  }
  return (
    <div className="grid grid-cols-2 gap-1">
      {images.slice(0, 4).map((m, i) => cell(m, i, "aspect-square", i === 3 ? images.length - 4 : undefined))}
    </div>
  );
}

export function PostAttachments({ postId, attachments }: { postId: number; attachments: FeedAttachment[] }) {
  const [viewer, setViewer] = useState<number | null>(null);
  const images = attachments.filter((a) => a.kind === "IMAGE");
  const videos = attachments.filter((a) => a.kind === "VIDEO");
  const files = attachments.filter((a) => a.kind === "FILE");
  if (attachments.length === 0) return null;

  return (
    <div className="mt-3 space-y-2">
      {images.length > 0 && <ImageGrid postId={postId} images={images} onOpen={setViewer} />}
      {videos.map((v) => (
        <video
          key={v.fileId}
          controls
          preload="metadata"
          className="max-h-[480px] w-full rounded-lg"
          src={fileUrl(postId, v.fileId)}
          aria-label={v.name}
        />
      ))}
      {files.map((f) => (
        <div key={f.fileId} className="flex items-center gap-3 rounded-lg border bg-card px-3 py-2">
          <FileIcon name={f.name} />
          <div className="min-w-0 flex-1">
            <p className="truncate text-[15px]">{f.name}</p>
            <p className="text-xs text-muted-foreground">{formatFileSize(f.sizeBytes)}</p>
          </div>
          <Button
            variant="ghost"
            size="icon"
            className="size-11 md:size-9"
            aria-label={`${f.name} dosyasını indir`}
            nativeButton={false}
            render={<a href={fileUrl(postId, f.fileId, true)} download />}
          >
            <Download className="size-4" aria-hidden />
          </Button>
        </div>
      ))}
      <ImageViewer
        postId={postId}
        images={images}
        index={viewer}
        onIndexChange={setViewer}
        onClose={() => setViewer(null)}
      />
    </div>
  );
}
