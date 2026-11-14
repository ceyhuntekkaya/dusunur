"use client";

import { useEffect } from "react";
import { ChevronLeft, ChevronRight, Download, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";

export function ImageViewer({
  fileUrl,
  images,
  index,
  onIndexChange,
  onClose,
}: {
  fileUrl: (fileId: string, download?: boolean) => string;
  images: { fileId: string; name: string }[];
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
            { }
            <img src={fileUrl(current.fileId)} alt={current.name} className="max-h-full max-w-full object-contain" />
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
                  render={<a href={fileUrl(current.fileId, true)} download />}
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
