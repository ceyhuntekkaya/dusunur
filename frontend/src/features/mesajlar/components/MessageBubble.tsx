"use client";

import { useRef, useState } from "react";
import { Ban, Clock, FileText } from "lucide-react";
import { toast } from "sonner";
import { RowMenu, type RowMenuItem } from "@/components/common/RowMenu";
import { UserAvatar } from "@/components/common/UserAvatar";
import { Button } from "@/components/ui/button";
import { Drawer, DrawerContent, DrawerHeader, DrawerTitle } from "@/components/ui/drawer";
import { linkify } from "@/lib/linkify";
import { cn } from "@/lib/utils";
import type { ConversationType, MessageDto } from "../types";
import { formatClock, nameColor } from "../utils";
import { MessageAttachments } from "./MessageAttachments";

export type PendingState = "sending" | "failed";

type Props = {
  message: MessageDto;
  type: ConversationType;
  first: boolean;
  last: boolean;
  showSender: boolean;
  pending?: PendingState;
  pendingFileNames?: string[];
  highlighted: boolean;
  seen: boolean;
  canReply: boolean;
  onReply: (m: MessageDto) => void;
  onDelete: (m: MessageDto) => void;
  onQuoteClick: (id: number) => void;
  onRetry?: () => void;
};

export function MessageBubble({
  message: m,
  type,
  first,
  last,
  showSender,
  pending,
  pendingFileNames,
  highlighted,
  seen,
  canReply,
  onReply,
  onDelete,
  onQuoteClick,
  onRetry,
}: Props) {
  const [sheet, setSheet] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pressed = useRef(false);
  const group = type !== "DIRECT";

  const items: RowMenuItem[] = [
    { label: "Yanıtla", onSelect: () => onReply(m), hidden: !canReply || m.archived || !!pending },
    {
      label: "Kopyala",
      onSelect: () => {
        void navigator.clipboard.writeText(m.content ?? "").then(() => toast.success("Mesaj kopyalandı."));
      },
      hidden: !m.content,
    },
    { label: "Sil", onSelect: () => onDelete(m), destructive: true, hidden: !m.canDelete || !!pending },
  ];
  const hasMenu = !m.deleted && items.some((i) => !i.hidden);

  function startPress() {
    if (!hasMenu) return;
    pressed.current = false;
    timer.current = setTimeout(() => {
      pressed.current = true;
      setSheet(true);
    }, 500);
  }
  function cancelPress() {
    if (timer.current) clearTimeout(timer.current);
    timer.current = null;
  }

  const avatar = group && !m.mine && (
    <div className="w-8 shrink-0 self-end">
      {showSender && <UserAvatar userId={m.sender.id} name={m.sender.fullName} size="md" />}
    </div>
  );

  if (m.deleted) {
    return (
      <div className={cn("flex gap-2", m.mine ? "justify-end" : "justify-start", !first && "mt-0.5", first && "mt-2")}>
        {avatar}
        <p
          data-message-id={m.id}
          className="flex items-center gap-1.5 px-2 py-1 text-[13px] text-muted-foreground italic"
        >
          <Ban className="size-3.5" aria-hidden />
          Bu mesaj silindi
        </p>
      </div>
    );
  }

  const mine = m.mine;

  return (
    <div className={cn("group/msg flex gap-2", mine ? "justify-end" : "justify-start", first ? "mt-2" : "mt-0.5")}>
      {avatar}
      {mine && hasMenu && (
        <div className="hidden self-center opacity-0 group-hover/msg:opacity-100 focus-within:opacity-100 md:block">
          <RowMenu items={items} label="Mesaj işlemleri" />
        </div>
      )}
      <div className={cn("flex max-w-[85%] flex-col md:max-w-[75%]", mine ? "items-end" : "items-start")}>
        <div
          data-message-id={m.id}
          onTouchStart={startPress}
          onTouchEnd={cancelPress}
          onTouchMove={cancelPress}
          onTouchCancel={cancelPress}
          onContextMenu={(e) => {
            if (pressed.current) e.preventDefault();
          }}
          className={cn(
            "min-w-0 space-y-1.5 rounded-2xl border px-3 py-2 text-[15px] transition-shadow [-webkit-touch-callout:none]",
            mine ? "border-transparent bg-primary text-primary-foreground [&_a]:text-primary-foreground [&_a]:underline" : "bg-card",
            mine ? (first ? "" : "rounded-tr-md") : first ? "" : "rounded-tl-md",
            mine ? (last ? "rounded-br-sm" : "rounded-br-md") : last ? "rounded-bl-sm" : "rounded-bl-md",
            m.archived && "opacity-75",
            highlighted && "ring-2 ring-primary/60 ring-offset-2",
          )}
        >
          {showSender && group && !mine && (
            <p className="text-[13px] font-semibold" style={{ color: nameColor(m.sender.id) }}>
              {m.sender.fullName}
            </p>
          )}
          {m.replyTo && (
            <button
              type="button"
              onClick={() => onQuoteClick(m.replyTo!.id)}
              className={cn(
                "block w-full rounded-md border-l-[3px] px-2 py-1 text-left text-[13px] outline-none focus-visible:ring-2 focus-visible:ring-ring",
                mine ? "border-white/70 bg-black/15" : "border-primary bg-muted",
              )}
            >
              <span className="block truncate font-semibold">{m.replyTo.senderName}</span>
              <span className={cn("block truncate", m.replyTo.deleted && "italic")}>
                {m.replyTo.deleted ? "Silinmiş mesaj" : m.replyTo.snippet}
              </span>
            </button>
          )}
          {pending && pendingFileNames && pendingFileNames.length > 0 ? (
            <ul className="space-y-1">
              {pendingFileNames.map((n, i) => (
                <li key={i} className="flex items-center gap-1.5 text-[13px]">
                  <FileText className="size-4 shrink-0" aria-hidden />
                  <span className="truncate">{n}</span>
                </li>
              ))}
            </ul>
          ) : (
            <MessageAttachments conversationId={m.conversationId} attachments={m.attachments} />
          )}
          {m.content && <p className="break-words whitespace-pre-wrap">{linkify(m.content)}</p>}
          <p className={cn("flex items-center justify-end gap-1 text-[11px] leading-none opacity-70")}>
            {pending === "sending" ? <Clock className="size-3" aria-label="Gönderiliyor" /> : formatClock(m.createdAt)}
          </p>
        </div>
        {pending === "failed" && (
          <p className="mt-1 text-[13px] text-destructive" role="alert">
            Gönderilemedi ·{" "}
            <button type="button" className="font-medium underline outline-none focus-visible:ring-2" onClick={onRetry}>
              Tekrar dene
            </button>
          </p>
        )}
        {seen && <p className="mt-0.5 text-[11px] text-muted-foreground">Görüldü</p>}
      </div>
      {!mine && hasMenu && (
        <div className="hidden self-center opacity-0 group-hover/msg:opacity-100 focus-within:opacity-100 md:block">
          <RowMenu items={items} label="Mesaj işlemleri" />
        </div>
      )}
      {sheet && (
      <Drawer open onOpenChange={setSheet}>
        <DrawerContent>
          <DrawerHeader className="sr-only">
            <DrawerTitle>Mesaj işlemleri</DrawerTitle>
          </DrawerHeader>
          <div className="flex flex-col px-4 pt-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
            {items
              .filter((i) => !i.hidden)
              .map((i) => (
                <Button
                  key={i.label}
                  variant="ghost"
                  className={cn("h-12 justify-start text-base", i.destructive && "text-destructive")}
                  onClick={() => {
                    setSheet(false);
                    i.onSelect();
                  }}
                >
                  {i.label}
                </Button>
              ))}
          </div>
        </DrawerContent>
      </Drawer>
      )}
    </div>
  );
}
