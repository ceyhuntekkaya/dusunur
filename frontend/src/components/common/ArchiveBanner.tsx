import { Archive } from "lucide-react";

export function ArchiveBanner({ seasonName }: { seasonName: string }) {
  return (
    <div className="mb-4 flex items-center gap-2 rounded-lg bg-muted px-3 py-2.5 text-sm text-muted-foreground">
      <Archive className="size-4 shrink-0" aria-hidden />
      <span>Bu içerik {seasonName} eğitim-öğretim yılına ait; yalnızca görüntülenebilir.</span>
    </div>
  );
}
