"use client";

import { useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowDown, ArrowLeft, Loader2, SearchX } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { RowMenu } from "@/components/common/RowMenu";
import { Skeleton } from "@/components/ui/skeleton";
import { useFeedSeasons } from "@/features/akis/api";
import type { UploadedFileDto } from "@/features/core/types";
import { ApiError } from "@/lib/api";
import { useMe } from "@/lib/me";
import { cn } from "@/lib/utils";
import {
  chatKeys,
  messageSnippet,
  useConversation,
  useDeleteMessage,
  useLeaveConversation,
  useMarkRead,
  useMessages,
  useMuteConversation,
  useSendMessage,
} from "../api";
import type { ConversationDetail, MessageDto, SendMessageRequest } from "../types";
import { formatDayLabel, gapMinutes, sameDay } from "../utils";
import { ChatComposer } from "./ChatComposer";
import { ChatInfoSheet } from "./ChatInfoSheet";
import { ConversationAvatar } from "./ConversationAvatar";
import { MessageBubble, type PendingState } from "./MessageBubble";

type Pending = {
  request: SendMessageRequest;
  status: PendingState;
  createdAt: string;
  fileNames: string[];
  replyTo?: MessageDto;
};

type Item = { key: string; m: MessageDto; pending?: PendingState; fileNames?: string[] };

const BOTTOM_THRESHOLD = 80;

function Header({
  detail,
  onInfo,
  onLeave,
}: {
  detail: ConversationDetail;
  onInfo: () => void;
  onLeave: () => void;
}) {
  const mute = useMuteConversation(detail.id);
  const other = detail.type === "DIRECT" ? detail.members.find((m) => m.id === detail.other?.id) : undefined;
  const sub =
    detail.type === "DIRECT"
      ? [other?.title, other?.branchName].filter(Boolean).join(" · ")
      : `${detail.memberCount} üye`;

  return (
    <header className="flex h-14 shrink-0 items-center gap-1 border-b bg-card px-2 pt-[env(safe-area-inset-top)] box-content">
      <Link
        href="/mesajlar"
        aria-label="Mesajlara dön"
        className="flex size-11 shrink-0 items-center justify-center rounded-lg outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 lg:hidden"
      >
        <ArrowLeft className="size-5" aria-hidden />
      </Link>
      <button
        type="button"
        onClick={onInfo}
        className="flex min-w-0 flex-1 items-center gap-3 rounded-lg p-1 text-left outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        <ConversationAvatar conversation={detail} />
        <span className="min-w-0">
          <span className="block truncate text-[15px] font-semibold">
            {detail.title}
            {detail.type === "DIRECT" && detail.other?.former && (
              <span className="font-normal text-muted-foreground"> (Eski personel)</span>
            )}
          </span>
          {sub && <span className="block truncate text-[13px] text-muted-foreground">{sub}</span>}
        </span>
      </button>
      <RowMenu
        label="Sohbet işlemleri"
        items={[
          { label: "Sohbet bilgisi", onSelect: onInfo },
          {
            label: detail.muted ? "Sesi aç" : "Sessize al",
            onSelect: () => mute.mutate(!detail.muted),
          },
          { label: "Sohbetten ayrıl", onSelect: onLeave, destructive: true, hidden: !detail.canLeave },
        ]}
      />
    </header>
  );
}

function HeaderSkeleton() {
  return (
    <div className="flex h-14 shrink-0 items-center gap-3 border-b bg-card px-4">
      <Skeleton className="size-10 rounded-full" />
      <div className="space-y-2">
        <Skeleton className="h-4 w-40" />
        <Skeleton className="h-3 w-24" />
      </div>
    </div>
  );
}

export function ChatView({ id }: { id: number }) {
  const router = useRouter();
  const qc = useQueryClient();
  const me = useMe().data;
  const seasons = useFeedSeasons().data;
  const detailQ = useConversation(id);
  const messagesQ = useMessages(id);
  const send = useSendMessage(id);
  const del = useDeleteMessage(id);
  const leave = useLeaveConversation(id);
  const { mutate: markReadMutate } = useMarkRead(id);

  const [pending, setPending] = useState<Pending[]>([]);
  const [replyTo, setReplyTo] = useState<MessageDto | null>(null);
  const [deleting, setDeleting] = useState<MessageDto | null>(null);
  const [infoOpen, setInfoOpen] = useState(false);
  const [leaveOpen, setLeaveOpen] = useState(false);
  const [highlight, setHighlight] = useState<number | null>(null);
  const [atBottom, setAtBottom] = useState(true);
  const [visible, setVisible] = useState(true);
  const [newCount, setNewCount] = useState(0);

  const scroller = useRef<HTMLDivElement>(null);
  const atBottomRef = useRef(true);
  const anchor = useRef<{ height: number; top: number } | null>(null);
  const initialScrolled = useRef(false);
  const lastKey = useRef<string | undefined>(undefined);
  const readSent = useRef(0);
  const readAt = useRef(0);

  const serverItems = useMemo(() => {
    const pages = messagesQ.data?.pages ?? [];
    return [...pages].reverse().flatMap((p) => [...p.items].reverse());
  }, [messagesQ.data]);

  const items: Item[] = useMemo(() => {
    const known = new Set(serverItems.map((m) => m.clientId));
    const list: Item[] = serverItems.map((m) => ({ key: m.clientId, m }));
    for (const p of pending) {
      if (known.has(p.request.clientId) || !me) continue;
      list.push({
        key: p.request.clientId,
        pending: p.status,
        fileNames: p.fileNames,
        m: {
          id: 0,
          conversationId: id,
          seasonId: me.season?.id ?? 0,
          archived: false,
          clientId: p.request.clientId,
          sender: { id: me.id, fullName: me.fullName, former: false },
          content: p.request.content,
          attachments: [],
          replyTo: p.replyTo && {
            id: p.replyTo.id,
            senderName: p.replyTo.sender.fullName,
            snippet: messageSnippet(p.replyTo),
            deleted: false,
          },
          createdAt: p.createdAt,
          deleted: false,
          mine: true,
          canDelete: false,
        },
      });
    }
    return list;
  }, [serverItems, pending, me, id]);

  const detail = detailQ.data;
  const pageCount = messagesQ.data?.pages.length ?? 0;
  const latestServerId = serverItems.length > 0 ? serverItems[serverItems.length - 1].id : 0;
  const { hasNextPage, isFetchingNextPage, fetchNextPage } = messagesQ;

  function scrollToBottom(smooth = false) {
    const el = scroller.current;
    if (el) el.scrollTo({ top: el.scrollHeight, behavior: smooth ? "smooth" : "auto" });
  }

  useLayoutEffect(() => {
    const el = scroller.current;
    if (!el || !messagesQ.data) return;
    if (!initialScrolled.current) {
      initialScrolled.current = true;
      el.scrollTop = el.scrollHeight;
      lastKey.current = items[items.length - 1]?.key;
      return;
    }
    if (anchor.current) {
      el.scrollTop = el.scrollHeight - anchor.current.height + anchor.current.top;
      anchor.current = null;
    }
  }, [pageCount, messagesQ.data, items]);

  const tailKey = items[items.length - 1]?.key;
  const tailMine = items[items.length - 1]?.m.mine ?? false;
  useLayoutEffect(() => {
    if (!initialScrolled.current || tailKey === lastKey.current) return;
    lastKey.current = tailKey;
    if (atBottomRef.current || tailMine) scrollToBottom();
     
    else setNewCount((n) => n + 1);
  }, [tailKey, tailMine]);

  useEffect(() => {
    const el = scroller.current;
    if (el && hasNextPage && !isFetchingNextPage && el.scrollHeight <= el.clientHeight) {
      void fetchNextPage();
    }
  }, [pageCount, hasNextPage, isFetchingNextPage, fetchNextPage]);

  useEffect(() => {
    const onVis = () => setVisible(document.visibilityState === "visible");
    document.addEventListener("visibilitychange", onVis);
    return () => document.removeEventListener("visibilitychange", onVis);
  }, []);

  useEffect(() => {
    if (!visible || !atBottom || latestServerId <= readSent.current) return;
    const wait = Math.max(0, 2000 - (Date.now() - readAt.current));
    const t = setTimeout(() => {
      readSent.current = latestServerId;
      readAt.current = Date.now();
      markReadMutate(latestServerId);
    }, wait);
    return () => clearTimeout(t);
  }, [visible, atBottom, latestServerId, markReadMutate]);

  function onScroll() {
    const el = scroller.current;
    if (!el) return;
    const bottom = el.scrollHeight - el.scrollTop - el.clientHeight < BOTTOM_THRESHOLD;
    atBottomRef.current = bottom;
    setAtBottom(bottom);
    if (bottom) setNewCount(0);
    if (el.scrollTop < 120 && hasNextPage && !isFetchingNextPage) {
      anchor.current = { height: el.scrollHeight, top: el.scrollTop };
      void fetchNextPage();
    }
  }

  async function deliver(p: Pending) {
    setPending((list) =>
      list.some((x) => x.request.clientId === p.request.clientId)
        ? list.map((x) => (x.request.clientId === p.request.clientId ? { ...x, status: "sending" } : x))
        : [...list, { ...p, status: "sending" }],
    );
    try {
      await send.mutateAsync(p.request);
      setPending((list) => list.filter((x) => x.request.clientId !== p.request.clientId));
    } catch {
      setPending((list) =>
        list.map((x) => (x.request.clientId === p.request.clientId ? { ...x, status: "failed" } : x)),
      );
    }
  }

  function onSend(v: { content?: string; files: UploadedFileDto[] }) {
    void deliver({
      request: {
        clientId: crypto.randomUUID(),
        content: v.content,
        fileIds: v.files.map((f) => f.id),
        replyToId: replyTo?.id,
      },
      status: "sending",
      createdAt: new Date().toISOString(),
      fileNames: v.files.map((f) => f.originalName),
      replyTo: replyTo ?? undefined,
    });
    setReplyTo(null);
  }

  function jumpTo(messageId: number) {
    const el = scroller.current?.querySelector(`[data-message-id="${messageId}"]`);
    if (!el) {
      toast("Mesaj daha eski sayfada.");
      return;
    }
    el.scrollIntoView({ block: "center", behavior: "smooth" });
    setHighlight(messageId);
    setTimeout(() => setHighlight(null), 1000);
  }

  const root =
    "fixed inset-0 z-40 flex flex-col bg-background lg:static lg:z-auto lg:min-h-0 lg:flex-1";

  if (detailQ.isError) {
    const err = detailQ.error;
    return (
      <div className={root}>
        <div className="m-auto">
          {err instanceof ApiError && err.status === 404 ? (
            <EmptyState
              icon={SearchX}
              message={err.message}
              action={{ label: "Mesajlara dön", href: "/mesajlar" }}
            />
          ) : (
            <ErrorState error={err} onRetry={() => void detailQ.refetch()} />
          )}
        </div>
      </div>
    );
  }

  const lastMine = detail?.type === "DIRECT" ? [...serverItems].reverse().find((m) => m.mine) : undefined;
  const seenId =
    lastMine && !lastMine.deleted && detail?.otherLastReadMessageId !== undefined && lastMine.id <= detail.otherLastReadMessageId
      ? lastMine.id
      : undefined;

  return (
    <div className={root}>
      {detail ? (
        <Header detail={detail} onInfo={() => setInfoOpen(true)} onLeave={() => setLeaveOpen(true)} />
      ) : (
        <HeaderSkeleton />
      )}

      <div className="relative min-h-0 flex-1">
        <div
          ref={scroller}
          onScroll={onScroll}
          onLoadCapture={() => {
            if (atBottomRef.current) scrollToBottom();
          }}
          className="h-full overflow-y-auto overscroll-contain px-3 py-3"
          aria-busy={messagesQ.isPending}
        >
          {messagesQ.isPending && (
            <div className="space-y-3" aria-label="Yükleniyor">
              <Skeleton className="h-10 w-2/3 rounded-2xl" />
              <Skeleton className="ml-auto h-10 w-1/2 rounded-2xl" />
              <Skeleton className="h-16 w-3/5 rounded-2xl" />
            </div>
          )}
          {messagesQ.isError && <ErrorState error={messagesQ.error} onRetry={() => void messagesQ.refetch()} />}
          {isFetchingNextPage && (
            <div className="flex justify-center py-2">
              <Loader2 className="size-4 animate-spin text-muted-foreground" aria-label="Eski mesajlar yükleniyor" />
            </div>
          )}
          {messagesQ.isSuccess && items.length === 0 && (
            <p className="py-8 text-center text-[13px] text-muted-foreground">
              Henüz mesaj yok. İlk mesajı siz yazın.
            </p>
          )}
          {detail &&
            items.map((it, i) => {
              const m = it.m;
              const prev = items[i - 1]?.m;
              const next = items[i + 1]?.m;
              const grouped = (a: MessageDto, b: MessageDto) =>
                a.sender.id === b.sender.id &&
                a.seasonId === b.seasonId &&
                sameDay(a.createdAt, b.createdAt) &&
                gapMinutes(a.createdAt, b.createdAt) < 5;
              const first = !prev || !grouped(prev, m);
              const last = !next || !grouped(m, next);
              const newDay = !prev || !sameDay(prev.createdAt, m.createdAt);
              const seasonChanged = !!prev && prev.seasonId !== m.seasonId;
              const seasonName = seasons?.find((s) => s.id === m.seasonId)?.name;
              return (
                <div key={it.key}>
                  {newDay && (
                    <div className="my-3 flex justify-center">
                      <span className="rounded-full bg-card px-3 py-1 text-xs text-muted-foreground shadow-sm">
                        {formatDayLabel(m.createdAt)}
                      </span>
                    </div>
                  )}
                  {seasonChanged && seasonName && (
                    <p className="my-3 text-center text-xs text-muted-foreground">
                      — {seasonName} eğitim-öğretim yılı —
                    </p>
                  )}
                  <MessageBubble
                    message={m}
                    type={detail.type}
                    first={first}
                    last={last}
                    showSender={first}
                    pending={it.pending}
                    pendingFileNames={it.fileNames}
                    highlighted={highlight === m.id && m.id > 0}
                    seen={seenId === m.id && m.id > 0}
                    canReply={detail.canSend}
                    onReply={setReplyTo}
                    onDelete={setDeleting}
                    onQuoteClick={jumpTo}
                    onRetry={() => {
                      const p = pending.find((x) => x.request.clientId === it.key);
                      if (p) void deliver(p);
                    }}
                  />
                </div>
              );
            })}
        </div>
        {newCount > 0 && (
          <button
            type="button"
            onClick={() => {
              scrollToBottom(true);
              setNewCount(0);
            }}
            className={cn(
              "absolute bottom-3 left-1/2 flex h-9 -translate-x-1/2 items-center gap-1.5 rounded-full bg-primary px-4 text-[13px] font-medium text-primary-foreground shadow-lg outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
            )}
          >
            <ArrowDown className="size-4" aria-hidden />
            Yeni mesajlar ({newCount})
          </button>
        )}
      </div>

      {detail &&
        (detail.canSend ? (
          <ChatComposer replyTo={replyTo} onCancelReply={() => setReplyTo(null)} onSend={onSend} />
        ) : (
          <div className="border-t bg-muted px-4 py-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))] text-center text-[13px] text-muted-foreground">
            {detail.readOnlyReason}
          </div>
        ))}

      {detail && (
        <ChatInfoSheet
          detail={detail}
          open={infoOpen}
          onOpenChange={setInfoOpen}
          onLeave={() => {
            setInfoOpen(false);
            setLeaveOpen(true);
          }}
        />
      )}
      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(o) => !o && setDeleting(null)}
        title="Mesaj silinsin mi?"
        description="Mesaj herkesten silinecek. Bu işlem geri alınamaz."
        confirmLabel="Sil"
        destructive
        onConfirm={async () => {
          if (!deleting) return;
          const target = deleting;
          await del.mutateAsync(target.id);
          if (replyTo?.id === target.id) setReplyTo(null);
        }}
      />
      <ConfirmDialog
        open={leaveOpen}
        onOpenChange={setLeaveOpen}
        title="Sohbetten ayrılınsın mı?"
        description="Sohbetten ayrılacaksınız ve mesajları artık göremeyeceksiniz."
        confirmLabel="Ayrıl"
        destructive
        onConfirm={async () => {
          await leave.mutateAsync();
          toast.success("Sohbetten ayrıldınız.");
          qc.removeQueries({ queryKey: chatKeys.messages(id) });
          router.replace("/mesajlar");
        }}
      />
    </div>
  );
}
