"use client";

import { useState } from "react";
import Link from "next/link";
import { BellOff, Lock, SearchX, SquarePen } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { SearchInput } from "@/components/common/SearchInput";
import { ListSkeleton } from "@/components/common/Skeletons";
import { Button } from "@/components/ui/button";
import { formatBadgeCount } from "@/lib/format";
import { fold } from "@/lib/text";
import { cn } from "@/lib/utils";
import { useConversations } from "../api";
import type { ConversationRow } from "../types";
import { formatListTime } from "../utils";
import { ConversationAvatar } from "./ConversationAvatar";
import { NewChatDialog } from "./NewChatDialog";

function summary(c: ConversationRow): React.ReactNode {
  const m = c.lastMessage;
  if (!m) return "Henüz mesaj yok";
  if (m.deleted) return <em>Bu mesaj silindi</em>;
  if (m.mine) return `Sen: ${m.snippet}`;
  if (c.type !== "DIRECT") return `${m.senderFirstName}: ${m.snippet}`;
  return m.snippet;
}

function Row({ c, active }: { c: ConversationRow; active: boolean }) {
  const unread = c.unreadCount > 0;
  return (
    <Link
      href={`/mesajlar/${c.id}`}
      aria-current={active ? "page" : undefined}
      className={cn(
        "flex h-16 items-center gap-3 px-3 outline-none hover:bg-muted focus-visible:bg-muted",
        active && "bg-accent hover:bg-accent",
      )}
    >
      <ConversationAvatar conversation={c} />
      <div className="min-w-0 flex-1">
        <div className="flex items-baseline justify-between gap-2">
          <p className={cn("truncate text-[15px]", unread && "font-semibold")}>
            {c.title}
            {c.type === "DIRECT" && c.other?.former && (
              <span className="font-normal text-muted-foreground"> (Eski personel)</span>
            )}
          </p>
          {c.lastMessage && (
            <span className={cn("shrink-0 text-xs", unread ? "text-primary" : "text-muted-foreground")}>
              {formatListTime(c.lastMessage.createdAt)}
            </span>
          )}
        </div>
        <div className="flex items-center justify-between gap-2">
          <p className={cn("truncate text-[13px] text-muted-foreground", unread && "text-foreground")}>
            {summary(c)}
          </p>
          <span className="flex shrink-0 items-center gap-1">
            {c.readOnly && <Lock className="size-3.5 text-muted-foreground" aria-label="Salt okunur" />}
            {c.muted && <BellOff className="size-3.5 text-muted-foreground" aria-label="Sessizde" />}
            {unread && (
              <span
                className={cn(
                  "inline-flex h-5 min-w-5 items-center justify-center rounded-full px-1.5 text-[11px] font-semibold text-white",
                  c.muted ? "bg-muted-foreground" : "bg-primary",
                )}
                aria-label={`${c.unreadCount} okunmamış mesaj`}
              >
                {formatBadgeCount(c.unreadCount)}
              </span>
            )}
          </span>
        </div>
      </div>
    </Link>
  );
}

export function ConversationList({ activeId }: { activeId: number | null }) {
  const [query, setQuery] = useState("");
  const [newOpen, setNewOpen] = useState(false);
  const list = useConversations();

  const q = fold(query);
  const rows = (list.data ?? []).filter((c) => !q || fold(c.title).includes(q));

  let body: React.ReactNode;
  if (list.isPending) {
    body = (
      <div className="p-3">
        <ListSkeleton />
      </div>
    );
  } else if (list.isError) {
    body = <ErrorState error={list.error} onRetry={() => void list.refetch()} />;
  } else if (list.data.length === 0) {
    body = (
      <EmptyState
        icon={SquarePen}
        message="Henüz mesajınız yok."
        action={{ label: "Yeni Sohbet", onClick: () => setNewOpen(true) }}
      />
    );
  } else if (rows.length === 0) {
    body = <EmptyState icon={SearchX} message="Aramanızla eşleşen sohbet bulunamadı." />;
  } else {
    body = (
      <ul>
        {rows.map((c) => (
          <li key={c.id}>
            <Row c={c} active={c.id === activeId} />
          </li>
        ))}
      </ul>
    );
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="space-y-3 p-3 pt-0 md:pt-3">
        <div className="flex items-center justify-between gap-2">
          <h1 className="hidden text-xl font-semibold md:block">Mesajlar</h1>
          <Button
            variant="ghost"
            size="icon"
            className="ml-auto size-11 md:size-10"
            aria-label="Yeni Sohbet"
            onClick={() => setNewOpen(true)}
          >
            <SquarePen className="size-5" aria-hidden />
          </Button>
        </div>
        <SearchInput value={query} onChange={setQuery} label="Sohbetlerde ara" placeholder="Sohbetlerde ara" />
      </div>
      <div className="min-h-0 flex-1 overflow-y-auto">{body}</div>
      <NewChatDialog open={newOpen} onOpenChange={setNewOpen} />
    </div>
  );
}
