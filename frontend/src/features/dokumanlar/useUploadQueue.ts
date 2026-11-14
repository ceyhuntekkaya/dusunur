"use client";

import { useCallback, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { apiUpload, ApiError } from "@/lib/api";
import type { UploadedFileDto } from "@/features/core/types";
import { extensionOf, maxSizeMbFor, PURPOSE_EXTENSIONS } from "@/lib/files";
import { attachFile } from "./api";

export type UploadItem = {
  key: number;
  file: File;
  spaceId: number;
  folderId?: number;
  status: "waiting" | "uploading" | "done" | "error" | "skipped";
  progress: number;
  error?: string;
  fileId?: string;
};

export type ConflictChoice = "OVERWRITE" | "RENAME" | "SKIP";
export type ConflictRequest = { name: string; resolve: (c: { choice: ConflictChoice; all: boolean }) => void };

function validate(file: File): string | undefined {
  const ext = extensionOf(file.name);
  if (!PURPOSE_EXTENSIONS.DMS.includes(ext)) {
    return `Bu dosya türüne izin verilmiyor. İzin verilen türler: ${PURPOSE_EXTENSIONS.DMS.join(", ")}.`;
  }
  const limit = maxSizeMbFor("DMS", ext);
  if (file.size > limit * 1024 * 1024) return `Dosya çok büyük. En fazla ${limit} MB yükleyebilirsiniz.`;
  if (file.size === 0) return "Dosya boş. Başka bir dosya seçin.";
  return undefined;
}

export function useUploadQueue() {
  const qc = useQueryClient();
  const [items, setItems] = useState<UploadItem[]>([]);
  const [conflict, setConflict] = useState<ConflictRequest | null>(null);
  const itemsRef = useRef<UploadItem[]>([]);
  const running = useRef(false);
  const counter = useRef(0);
  const sticky = useRef<ConflictChoice | null>(null);
  const batch = useRef({ done: 0, skipped: 0 });

  const commit = useCallback((next: UploadItem[]) => {
    itemsRef.current = next;
    setItems(next);
  }, []);

  const patch = useCallback(
    (key: number, change: Partial<UploadItem>) =>
      commit(itemsRef.current.map((i) => (i.key === key ? { ...i, ...change } : i))),
    [commit],
  );

  const ask = useCallback(
    (name: string) =>
      new Promise<{ choice: ConflictChoice; all: boolean }>((resolve) => {
        setConflict({
          name,
          resolve: (c) => {
            setConflict(null);
            resolve(c);
          },
        });
      }),
    [],
  );

  const processItem = useCallback(
    async (item: UploadItem) => {
      const problem = validate(item.file);
      if (problem) {
        patch(item.key, { status: "error", error: problem });
        return;
      }
      patch(item.key, { status: "uploading", progress: 0, error: undefined });
      try {
        let fileId = item.fileId;
        if (!fileId) {
          const form = new FormData();
          form.append("file", item.file, item.file.name);
          form.append("purpose", "DMS");
          const up = await apiUpload<UploadedFileDto>("/api/files", form, {
            onProgress: (p) => patch(item.key, { progress: p }),
          });
          fileId = up.id;
          patch(item.key, { fileId, progress: 100 });
        }
        const target = { folderId: item.folderId, fileId };
        const first = sticky.current === "OVERWRITE" || sticky.current === "RENAME" ? sticky.current : "ASK";
        try {
          await attachFile(item.spaceId, { ...target, onConflict: first });
        } catch (err) {
          if (!(err instanceof ApiError) || err.code !== "DMS_NAME_CONFLICT") throw err;
          let choice = sticky.current;
          if (!choice) {
            const answer = await ask(err.details[0] ?? item.file.name);
            choice = answer.choice;
            if (answer.all) sticky.current = choice;
          }
          if (choice === "SKIP") {
            batch.current.skipped++;
            patch(item.key, { status: "skipped" });
            return;
          }
          await attachFile(item.spaceId, { ...target, onConflict: choice });
        }
        batch.current.done++;
        patch(item.key, { status: "done", progress: 100 });
      } catch (err) {
        const message = err instanceof ApiError ? err.message : "Dosya yüklenemedi. Lütfen tekrar deneyin.";
        patch(item.key, { status: "error", error: message });
      }
    },
    [ask, patch],
  );

  const run = useCallback(async () => {
    if (running.current) return;
    running.current = true;
    batch.current = { done: 0, skipped: 0 };
    sticky.current = null;
    for (;;) {
      const next = itemsRef.current.find((i) => i.status === "waiting");
      if (!next) break;
      await processItem(next);
    }
    running.current = false;
    const { done, skipped } = batch.current;
    if (done > 0 || skipped > 0) {
      toast.success(
        skipped > 0 ? `${done} dosya yüklendi, ${skipped} dosya atlandı.` : `${done} dosya yüklendi.`,
      );
    }
    void qc.invalidateQueries({ queryKey: ["dms"] });
  }, [processItem, qc]);

  const enqueue = useCallback(
    (files: File[], spaceId: number, folderId?: number) => {
      const added: UploadItem[] = files.map((file) => ({
        key: ++counter.current,
        file,
        spaceId,
        folderId,
        status: "waiting",
        progress: 0,
      }));
      commit([...itemsRef.current, ...added]);
      void run();
    },
    [commit, run],
  );

  const retry = useCallback(
    (key: number) => {
      patch(key, { status: "waiting", error: undefined });
      void run();
    },
    [patch, run],
  );

  const dismiss = useCallback(() => commit([]), [commit]);

  return { items, enqueue, retry, dismiss, conflict };
}
