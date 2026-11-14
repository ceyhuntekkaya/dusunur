import { File, FileArchive, FileImage, FileSpreadsheet, FileText, Folder, type LucideIcon } from "lucide-react";
import { toast } from "sonner";
import { PALETTE } from "@/components/common/UserAvatar";
import { extensionOf } from "@/lib/files";
import { toastError } from "@/lib/query-client";
import type { DmsLevel, RestoreResult } from "./types";

export const LEVEL_LABEL: Record<DmsLevel, string> = {
  VIEW: "Görüntüleme",
  EDIT: "Düzenleme",
  MANAGE: "Yönetim",
};

export const LEVEL_RANK: Record<DmsLevel, number> = { VIEW: 1, EDIT: 2, MANAGE: 3 };

export const LEVEL_BADGE: Record<DmsLevel, string> = {
  VIEW: "bg-muted text-muted-foreground",
  EDIT: "bg-blue-100 text-blue-800",
  MANAGE: "bg-purple-100 text-purple-800",
};

export const LEVEL_OPTIONS = (["VIEW", "EDIT", "MANAGE"] as const).map((v) => ({
  value: v,
  label: LEVEL_LABEL[v],
}));

export function spaceColor(id: number) {
  const [bg, fg] = PALETTE[Math.abs(id) % PALETTE.length];
  return { backgroundColor: bg, color: fg };
}

export function iconFor(name: string, contentType?: string): { Icon: LucideIcon; className: string } {
  const ext = extensionOf(name);
  if (ext === "pdf" || contentType === "application/pdf") return { Icon: FileText, className: "text-red-600" };
  if (["xls", "xlsx", "ods", "csv"].includes(ext)) return { Icon: FileSpreadsheet, className: "text-green-600" };
  if (contentType?.startsWith("image/") || ["jpg", "jpeg", "png", "webp", "gif"].includes(ext))
    return { Icon: FileImage, className: "text-purple-600" };
  if (["zip", "rar", "7z"].includes(ext)) return { Icon: FileArchive, className: "text-gray-500" };
  return { Icon: File, className: "text-muted-foreground" };
}

export const FOLDER_ICON = { Icon: Folder, className: "text-yellow-500" };

export const contentUrl = (fileId: number, download: boolean) =>
  `/api/dms/files/${fileId}/content?download=${download}`;

export function downloadFile(fileId: number) {
  const a = document.createElement("a");
  a.href = contentUrl(fileId, true);
  a.download = "";
  document.body.appendChild(a);
  a.click();
  a.remove();
}

export function openPdf(fileId: number) {
  window.open(contentUrl(fileId, false), "_blank", "noopener");
}

export async function copyFileLink(fileId: number) {
  try {
    await navigator.clipboard.writeText(`${window.location.origin}/dokumanlar/dosya/${fileId}`);
  } catch {
    toastError("Bağlantı kopyalanamadı. Tarayıcınızın pano iznini kontrol edip tekrar deneyin.");
    return;
  }
  toast.success("Bağlantı kopyalandı. Yalnızca bu alana erişimi olan kişiler açabilir.");
}

export function notifyRestored(r: RestoreResult) {
  let message = "Geri yüklendi.";
  if (r.movedToRoot) message += " Eski klasörü bulunamadığı için ana dizine geri yüklendi.";
  if (r.renamedTo) message += ` Aynı adda öğe olduğu için '${r.renamedTo}' olarak geri yüklendi.`;
  toast.success(message);
}

export function splitExtension(name: string): { base: string; ext: string } {
  const i = name.lastIndexOf(".");
  return i > 0 ? { base: name.slice(0, i), ext: name.slice(i) } : { base: name, ext: "" };
}
