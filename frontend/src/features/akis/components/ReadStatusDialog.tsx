"use client";

import { useState } from "react";
import { BellRing, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { DateText } from "@/components/common/DateText";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { SearchInput } from "@/components/common/SearchInput";
import { ListSkeleton } from "@/components/common/Skeletons";
import { UserAvatar } from "@/components/common/UserAvatar";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { fold } from "@/lib/text";
import { useReadStatus, useRemind } from "../api";
import type { ReadUser } from "../types";

const DAY_MS = 24 * 60 * 60 * 1000;

function UserRow({ u }: { u: ReadUser }) {
  return (
    <li className="flex items-center gap-3 py-2">
      <UserAvatar userId={u.userId} name={u.fullName} size="md" />
      <div className="min-w-0 flex-1">
        <p className="truncate text-[15px]">{u.fullName}</p>
        <p className="truncate text-xs text-muted-foreground">
          {[u.title, u.branchName].filter(Boolean).join(" · ")}
        </p>
      </div>
      {u.readAt && <DateText value={u.readAt} className="shrink-0 text-xs text-muted-foreground" />}
    </li>
  );
}

function Content({ postId, archived }: { postId: number; archived: boolean }) {
  const status = useReadStatus(postId, true);
  const remind = useRemind(postId);
  const [tab, setTab] = useState<"unread" | "read">("unread");
  const [q, setQ] = useState("");
  const [now] = useState(() => Date.now());

  if (status.isPending) return <ListSkeleton rows={4} />;
  if (status.isError) return <ErrorState error={status.error} onRetry={() => void status.refetch()} />;

  const { read, unread, lastReminderAt } = status.data;
  const total = read.length + unread.length;
  const needle = fold(q);
  const list = (tab === "unread" ? unread : read).filter((u) => !needle || fold(u.fullName).includes(needle));
  const recent = !!lastReminderAt && now - new Date(lastReminderAt).getTime() < DAY_MS;

  return (
    <div className="space-y-3">
      <p className="text-[13px] text-muted-foreground">
        {read.length}/{total} kişi okudu
      </p>
      <Tabs value={tab} onValueChange={(v) => setTab(v as "unread" | "read")}>
        <TabsList className="h-11 w-full md:h-10">
          <TabsTrigger value="unread">Okumayanlar ({unread.length})</TabsTrigger>
          <TabsTrigger value="read">Okuyanlar ({read.length})</TabsTrigger>
        </TabsList>
      </Tabs>
      <SearchInput value={q} onChange={setQ} placeholder="Kişi ara" label="Kişi ara" />
      <div className="max-h-[50dvh] overflow-y-auto">
        {list.length === 0 ? (
          <EmptyState
            icon={BellRing}
            message={q ? "Eşleşen kişi bulunamadı." : tab === "unread" ? "Duyuruyu okumayan kimse kalmadı." : "Henüz okuyan yok."}
          />
        ) : (
          <ul className="divide-y">
            {list.map((u) => (
              <UserRow key={u.userId} u={u} />
            ))}
          </ul>
        )}
      </div>
      <div className="flex flex-col gap-2 border-t pt-3 md:flex-row md:items-center md:justify-between">
        {recent && lastReminderAt ? (
          <p className="text-[13px] text-muted-foreground">
            Son hatırlatma: <DateText value={lastReminderAt} />
          </p>
        ) : (
          <span />
        )}
        {!archived && (
        <Button
          className="h-11 md:h-10"
          disabled={unread.length === 0 || recent || remind.isPending}
          onClick={() =>
            remind.mutate(undefined, {
              onSuccess: (res) => toast.success(`${res.notified} kişiye hatırlatma gönderildi.`),
            })
          }
        >
          {remind.isPending && <Loader2 className="animate-spin" aria-hidden />}
          Okumayanlara Hatırlat
        </Button>
        )}
      </div>
    </div>
  );
}

export function ReadStatusDialog({
  postId,
  archived,
  open,
  onOpenChange,
}: {
  postId: number;
  archived: boolean;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title="Okuma Durumu">
      {open && <Content postId={postId} archived={archived} />}
    </ResponsiveDialog>
  );
}
