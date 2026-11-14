"use client";

import { useEffect, useRef, useState } from "react";
import { Building2, Check, Megaphone, MapPin, MessageSquare, Pin, ThumbsUp, User, Users } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { DateText } from "@/components/common/DateText";
import { RowMenu } from "@/components/common/RowMenu";
import { UserAvatar } from "@/components/common/UserAvatar";
import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Textarea } from "@/components/ui/textarea";
import type { TargetType, TargetView } from "@/features/core/types";
import { linkify } from "@/lib/linkify";
import { useMe } from "@/lib/me";
import { cn } from "@/lib/utils";
import { useDeletePost, useEditPost, useMarkRead, usePinPost, useToggleLike } from "../api";
import type { PostDto } from "../types";
import { PostAttachments } from "./PostAttachments";
import { PostComments } from "./PostComments";
import { ReadStatusDialog } from "./ReadStatusDialog";

const TARGET_ICONS: Record<TargetType, typeof User> = {
  ALL: Building2,
  BRANCH: MapPin,
  GROUP: Users,
  USER: User,
};

function TargetLabel({ t }: { t: TargetView }) {
  const Icon = TARGET_ICONS[t.type];
  return (
    <span className="inline-flex items-center gap-1">
      <Icon className="size-3.5 shrink-0" aria-hidden />
      {t.name}
    </span>
  );
}

function Targets({ targets }: { targets: TargetView[] }) {
  const shown = targets.slice(0, 2);
  const rest = targets.length - shown.length;
  return (
    <>
      <span aria-hidden>·</span>
      <span aria-hidden>→</span>
      {shown.map((t, i) => (
        <span key={`${t.type}:${t.id ?? ""}`} className="inline-flex items-center">
          <TargetLabel t={t} />
          {i < shown.length - 1 && ","}
        </span>
      ))}
      {rest > 0 && (
        <Popover>
          <PopoverTrigger className="inline-flex h-6 items-center rounded px-1 font-medium text-foreground hover:bg-muted">
            +{rest}
          </PopoverTrigger>
          <PopoverContent align="start" className="w-64 space-y-2">
            {targets.map((t) => (
              <p key={`${t.type}:${t.id ?? ""}`} className="text-sm">
                <TargetLabel t={t} />
              </p>
            ))}
          </PopoverContent>
        </Popover>
      )}
    </>
  );
}

function PostText({ text }: { text: string }) {
  const ref = useRef<HTMLParagraphElement>(null);
  const [open, setOpen] = useState(false);
  const [overflow, setOverflow] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (el && !open) setOverflow(el.scrollHeight > el.clientHeight);
  }, [text, open]);

  return (
    <div className="px-4 pt-3">
      <p ref={ref} className={cn("text-[15px] break-words whitespace-pre-wrap", !open && "line-clamp-6")}>
        {linkify(text)}
      </p>
      {overflow && !open && (
        <button type="button" className="inline-flex h-8 items-center text-sm text-primary hover:underline" onClick={() => setOpen(true)}>
          Devamını gör
        </button>
      )}
    </div>
  );
}

function EditBox({ post, onClose }: { post: PostDto; onClose: () => void }) {
  const edit = useEditPost();
  const [text, setText] = useState(post.content ?? "");
  return (
    <div className="space-y-2 px-4 pt-3">
      <Textarea
        value={text}
        rows={4}
        aria-label="Gönderi metni"
        autoFocus
        onChange={(e) => setText(e.target.value)}
      />
      {text.trim().length > 5000 && (
        <p className="text-[13px] text-destructive" role="alert">
          Metin en fazla 5000 karakter olabilir.
        </p>
      )}
      <div className="flex gap-2">
        <Button
          className="h-11 md:h-10"
          disabled={edit.isPending || text.trim().length > 5000}
          onClick={() =>
            edit.mutate(
              { id: post.id, content: text.trim() || undefined },
              {
                onSuccess: () => {
                  toast.success("Gönderi güncellendi.");
                  onClose();
                },
              },
            )
          }
        >
          Kaydet
        </Button>
        <Button variant="outline" className="h-11 md:h-10" onClick={onClose}>
          Vazgeç
        </Button>
      </div>
    </div>
  );
}

type Props = { post: PostDto; expanded?: boolean; onDeleted?: () => void };

export function PostCard({ post, expanded = false, onDeleted }: Props) {
  const me = useMe().data;
  const like = useToggleLike();
  const read = useMarkRead();
  const pin = usePinPost();
  const del = useDeletePost();
  const [editing, setEditing] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [readStatus, setReadStatus] = useState(false);
  const [commentOpen, setCommentOpen] = useState(false);
  const [showAll, setShowAll] = useState(expanded);

  const writable = !post.archived;
  const isAuthor = me?.id === post.author.id;
  const showReadStats = post.important && post.readStats !== undefined;
  const meta = [post.author.title, post.author.branchName].filter(Boolean).join(" · ");
  const hasCounts = post.likeCount > 0 || post.commentCount > 0 || showReadStats;

  const menu = [
    { label: "Düzenle", onSelect: () => setEditing(true), hidden: !post.canEdit },
    {
      label: post.pinned ? "Sabitlemeyi kaldır" : "Sabitle",
      onSelect: () =>
        pin.mutate(
          { id: post.id, pin: !post.pinned },
          { onSuccess: () => toast.success(post.pinned ? "Sabitleme kaldırıldı." : "Gönderi sabitlendi.") },
        ),
      hidden: !post.canPin,
    },
    { label: "Okuma durumu", onSelect: () => setReadStatus(true), hidden: !showReadStats },
    { label: "Sil", onSelect: () => setConfirmDelete(true), destructive: true, hidden: !post.canDelete },
  ];

  return (
    <article
      className={cn(
        "overflow-hidden rounded-xl border bg-card",
        post.important && "border-l-4 border-l-important",
      )}
    >
      <div className={cn("px-4 pt-3 pb-2", post.important && "bg-important-bg")}>
        {post.pinned && writable && (
          <p className="mb-1 flex items-center gap-1 text-xs text-muted-foreground">
            <Pin className="size-3.5" aria-hidden />
            Sabitlendi
          </p>
        )}
        {post.important && (
          <p className="mb-1.5 inline-flex items-center gap-1.5 rounded-full bg-important px-2.5 py-0.5 text-xs font-semibold text-white">
            <Megaphone className="size-3.5" aria-hidden />
            ÖNEMLİ DUYURU
          </p>
        )}
        <div className="flex items-start gap-3">
          <UserAvatar userId={post.author.id} name={post.author.fullName} size="md" />
          <div className="min-w-0 flex-1">
            <p className="text-[15px]">
              <span className="font-semibold">{post.author.fullName}</span>
              {post.author.former && <span className="text-xs text-muted-foreground"> (Eski personel)</span>}
              {meta && <span className="ml-1.5 text-xs text-muted-foreground">{meta}</span>}
            </p>
            <div className="flex flex-wrap items-center gap-x-1.5 text-xs text-muted-foreground">
              <DateText value={post.createdAt} />
              {post.editedAt && <span>· düzenlendi</span>}
              <Targets targets={post.targets} />
            </div>
          </div>
          <RowMenu items={menu} label="Gönderi işlemleri" />
        </div>
      </div>

      {editing ? (
        <EditBox post={post} onClose={() => setEditing(false)} />
      ) : (
        post.content && <PostText text={post.content} />
      )}

      {post.attachments.length > 0 && (
        <div className="px-4 pb-1">
          <PostAttachments postId={post.id} attachments={post.attachments} />
        </div>
      )}

      {hasCounts && (
        <div className="flex flex-wrap items-center gap-x-4 px-4 pt-3 text-[13px] text-muted-foreground">
          {post.likeCount > 0 && (
            <span className="inline-flex items-center gap-1">
              <ThumbsUp className="size-3.5" aria-hidden />
              {post.likeCount}
            </span>
          )}
          {post.commentCount > 0 && (
            <button
              type="button"
              className="inline-flex h-8 items-center hover:underline"
              onClick={() => setShowAll(true)}
            >
              {post.commentCount} yorum
            </button>
          )}
          {showReadStats && post.readStats && (
            <button
              type="button"
              className="ml-auto inline-flex h-8 items-center gap-1 hover:underline"
              onClick={() => setReadStatus(true)}
            >
              <Check className="size-3.5" aria-hidden />
              {post.readStats.read}/{post.readStats.total} okudu
            </button>
          )}
        </div>
      )}

      {writable && (
        <div className="mx-4 mt-2 flex gap-1 border-t py-1">
          <Button
            variant="ghost"
            className={cn("h-10 flex-1", post.liked && "text-primary")}
            aria-pressed={post.liked}
            onClick={() => like.mutate({ post, like: !post.liked })}
          >
            <ThumbsUp className={cn(post.liked && "fill-current")} aria-hidden />
            Beğen
          </Button>
          <Button variant="ghost" className="h-10 flex-1" onClick={() => setCommentOpen(true)}>
            <MessageSquare aria-hidden />
            Yorum Yap
          </Button>
          {post.important && post.readByMe === false && (
            <Button
              className="h-10 flex-1 bg-important text-white hover:bg-important/90"
              disabled={read.isPending}
              onClick={() =>
                read.mutate(post.id, { onSuccess: () => toast.success("Okuduğunuz kaydedildi.") })
              }
            >
              Okudum, anladım
            </Button>
          )}
          {post.important && post.readByMe === true && (
            <span className="flex h-10 flex-1 items-center justify-center gap-1.5 rounded-lg bg-success/10 text-sm font-medium text-success">
              <Check className="size-4" aria-hidden />
              Okudunuz
            </span>
          )}
        </div>
      )}
      {!writable && <div className="pb-3" />}

      <PostComments
        post={post}
        showAll={showAll}
        onShowAll={() => setShowAll(true)}
        boxOpen={expanded || commentOpen}
        focusBox={commentOpen}
        writable={writable}
      />

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title="Gönderi silinsin mi?"
        description={`Gönderi silinecek. Bu işlem geri alınamaz.${
          isAuthor ? "" : ` Gönderi ${post.author.fullName} tarafından paylaşılmıştı.`
        }`}
        confirmLabel="Sil"
        destructive
        onConfirm={async () => {
          await del.mutateAsync(post.id);
          toast.success("Gönderi silindi.");
          onDeleted?.();
        }}
      />
      {showReadStats && <ReadStatusDialog postId={post.id} open={readStatus} onOpenChange={setReadStatus} />}
    </article>
  );
}
