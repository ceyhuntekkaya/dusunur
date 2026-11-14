import type { FilePurpose } from "@/features/core/types";

const IMAGE = ["jpg", "jpeg", "png", "webp", "gif"];
const VIDEO = ["mp4", "mov", "webm"];
const DOCS = ["pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "txt", "csv", "zip"];
const FEED = [...IMAGE, ...DOCS, ...VIDEO];

export const PURPOSE_EXTENSIONS: Record<FilePurpose, string[]> = {
  FEED,
  CHAT: FEED,
  PRINT: ["pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "jpg", "jpeg", "png"],
  DMS: [...FEED, "rtf", "rar", "7z", "mp3"],
  CALENDAR: ["pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png"],
};

export function extensionOf(name: string): string {
  const i = name.lastIndexOf(".");
  return i < 0 ? "" : name.slice(i + 1).toLowerCase();
}

export function maxSizeMbFor(purpose: FilePurpose, ext: string): number {
  switch (purpose) {
    case "FEED":
    case "CHAT":
      if (IMAGE.includes(ext)) return 20;
      if (VIDEO.includes(ext)) return 100;
      return 50;
    case "PRINT":
      return 50;
    case "DMS":
      return 100;
    case "CALENDAR":
      return 20;
  }
}
