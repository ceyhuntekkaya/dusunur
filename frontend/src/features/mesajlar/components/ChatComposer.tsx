"use client";

import { useRef, useState } from "react";
import { Send, X } from "lucide-react";
import { FileUploader } from "@/components/common/FileUploader";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import type { UploadedFileDto } from "@/features/core/types";
import { useIsDesktop } from "@/lib/media";
import { messageSnippet } from "../api";
import { messageContentSchema } from "../schemas";
import type { MessageDto } from "../types";

type Props = {
  replyTo: MessageDto | null;
  onCancelReply: () => void;
  onSend: (v: { content?: string; files: UploadedFileDto[] }) => void;
};

export function ChatComposer({ replyTo, onCancelReply, onSend }: Props) {
  const desktop = useIsDesktop();
  const [text, setText] = useState("");
  const [files, setFiles] = useState<UploadedFileDto[]>([]);
  const [uploading, setUploading] = useState(false);
  const [uploaderKey, setUploaderKey] = useState(0);
  const area = useRef<HTMLTextAreaElement>(null);

  const parsed = messageContentSchema.safeParse(text);
  const error = parsed.success ? undefined : parsed.error.issues[0]?.message;
  const trimmed = parsed.success ? parsed.data : text.trim();
  const canSubmit = !error && !uploading && (trimmed.length > 0 || files.length > 0);

  function grow() {
    const el = area.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = `${Math.min(el.scrollHeight, 120)}px`;
  }

  function submit() {
    if (!canSubmit) return;
    onSend({ content: trimmed || undefined, files });
    setText("");
    setFiles([]);
    setUploaderKey((k) => k + 1);
    requestAnimationFrame(() => {
      if (area.current) area.current.style.height = "auto";
      area.current?.focus();
    });
  }

  return (
    <div className="border-t bg-card px-3 pt-2 pb-[calc(0.5rem+env(safe-area-inset-bottom))]">
      {replyTo && (
        <div className="mb-2 flex items-center gap-2 rounded-lg border-l-[3px] border-primary bg-muted px-3 py-1.5">
          <div className="min-w-0 flex-1 text-[13px]">
            <p className="truncate font-semibold">{replyTo.sender.fullName} kişisine yanıt</p>
            <p className="truncate text-muted-foreground">{messageSnippet(replyTo)}</p>
          </div>
          <Button
            variant="ghost"
            size="icon"
            className="size-11 shrink-0 md:size-9"
            aria-label="Yanıtı iptal et"
            onClick={onCancelReply}
          >
            <X className="size-4" aria-hidden />
          </Button>
        </div>
      )}
      <div className="flex flex-wrap items-end gap-2">
        <FileUploader
          key={uploaderKey}
          compact
          purpose="CHAT"
          multiple
          compressImages
          maxFiles={10}
          maxFilesMessage="Bir mesaja en fazla 10 dosya eklenebilir."
          onUploadingChange={setUploading}
          onChange={setFiles}
        />
        <Textarea
          ref={area}
          rows={1}
          value={text}
          placeholder="Mesaj yazın…"
          aria-label="Mesaj"
          aria-invalid={!!error}
          className="max-h-[120px] min-h-11 min-w-0 flex-1 resize-none py-2.5 text-[15px]"
          onChange={(e) => {
            setText(e.target.value);
            grow();
          }}
          onKeyDown={(e) => {
            if (desktop && e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
              e.preventDefault();
              submit();
            }
          }}
        />
        <Button
          type="button"
          size="icon"
          className="size-11 shrink-0 rounded-full"
          aria-label="Gönder"
          disabled={!canSubmit}
          onClick={submit}
        >
          <Send className="size-5" aria-hidden />
        </Button>
      </div>
      {error && (
        <p className="mt-1 text-[13px] text-destructive" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
