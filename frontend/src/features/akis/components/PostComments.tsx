"use client";

import { useEffect, useRef, useState } from "react";
import { Loader2, SendHorizontal } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { DateText } from "@/components/common/DateText";
import { UserAvatar } from "@/components/common/UserAvatar";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useMe } from "@/lib/me";
import { useIsDesktop } from "@/lib/media";
import { linkify } from "@/lib/linkify";
import { useAddComment, useDeleteComment, usePostComments } from "../api";
import { commentSchema } from "../schemas";
import type { CommentDto, PostDto } from "../types";

function CommentItem({ comment, writable, onDelete }: { comment: CommentDto; writable: boolean; onDelete: () => void }) {
  return (
    <li className="flex gap-2">
      <UserAvatar userId={comment.author.id} name={comment.author.fullName} size="sm" />
      <div className="min-w-0 flex-1">
        <div className="inline-block max-w-full rounded-2xl bg-muted px-3 py-2">
          <p className="text-[13px] font-semibold">{comment.author.fullName}</p>
          <p className="text-sm break-words whitespace-pre-wrap">{linkify(comment.content)}</p>
        </div>
        <div className="mt-0.5 flex items-center gap-3 px-2 text-xs text-muted-foreground">
          <DateText value={comment.createdAt} relative />
          {writable && comment.canDelete && (
            <button type="button" className="inline-flex h-8 items-center hover:underline" onClick={onDelete}>
              Sil
            </button>
          )}
        </div>
      </div>
    </li>
  );
}

function CommentBox({ postId, autoFocus }: { postId: number; autoFocus: boolean }) {
  const me = useMe().data;
  const desktop = useIsDesktop();
  const add = useAddComment(postId);
  const [text, setText] = useState("");
  const [error, setError] = useState<string>();
  const ref = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (autoFocus) ref.current?.focus();
  }, [autoFocus]);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = `${Math.min(el.scrollHeight, 4 * 22 + 16)}px`;
  }, [text]);

  async function send() {
    const parsed = commentSchema.safeParse(text);
    if (!parsed.success) {
      setError(parsed.error.issues[0].message);
      return;
    }
    setError(undefined);
    try {
      await add.mutateAsync(parsed.data);
      setText("");
    } catch {
      // Hata genel toast ile gösterilir; metin korunur.
    }
  }

  return (
    <div className="flex items-start gap-2">
      {me && <UserAvatar userId={me.id} name={me.fullName} size="sm" />}
      <div className="min-w-0 flex-1">
        <div className="flex items-end gap-1">
          <Textarea
            ref={ref}
            rows={1}
            value={text}
            placeholder="Yorum yazın…"
            aria-label="Yorum yazın"
            className="min-h-11 resize-none rounded-2xl py-2.5 md:min-h-10 md:py-2"
            onChange={(e) => {
              setText(e.target.value);
              setError(undefined);
            }}
            onKeyDown={(e) => {
              if (desktop && e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                void send();
              }
            }}
          />
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="size-11 shrink-0 text-primary md:size-10"
            aria-label="Yorumu gönder"
            disabled={add.isPending || text.trim() === ""}
            onClick={() => void send()}
          >
            {add.isPending ? <Loader2 className="animate-spin" aria-hidden /> : <SendHorizontal aria-hidden />}
          </Button>
        </div>
        {text.length > 900 && (
          <p className="px-2 text-xs text-muted-foreground">{text.length}/1000</p>
        )}
        {error && (
          <p className="px-2 text-[13px] text-destructive" role="alert">
            {error}
          </p>
        )}
      </div>
    </div>
  );
}

type Props = {
  post: PostDto;
  showAll: boolean;
  onShowAll: () => void;
  boxOpen: boolean;
  focusBox: boolean;
  writable: boolean;
};

export function PostComments({ post, showAll, onShowAll, boxOpen, focusBox, writable }: Props) {
  const all = usePostComments(post.id, showAll);
  const del = useDeleteComment();
  const [deleting, setDeleting] = useState<CommentDto>();

  const loaded = all.data?.pages.flatMap((p) => p.items);
  const comments = showAll && loaded ? loaded : post.latestComments;
  const hasMore = !!all.hasNextPage;

  if (comments.length === 0 && !boxOpen) return null;

  return (
    <div className="space-y-3 border-t px-4 py-3">
      {!showAll && post.commentCount > post.latestComments.length && (
        <button type="button" className="inline-flex h-8 items-center text-sm text-muted-foreground hover:underline" onClick={onShowAll}>
          Tüm yorumları gör ({post.commentCount})
        </button>
      )}
      {showAll && all.isPending && (
        <Loader2 className="size-4 animate-spin text-muted-foreground" aria-label="Yükleniyor" />
      )}
      {comments.length > 0 && (
        <ul className="space-y-3">
          {comments.map((c) => (
            <CommentItem key={c.id} comment={c} writable={writable} onDelete={() => setDeleting(c)} />
          ))}
        </ul>
      )}
      {showAll && hasMore && (
        <Button
          variant="outline"
          className="h-11 md:h-10"
          disabled={all.isFetchingNextPage}
          onClick={() => void all.fetchNextPage()}
        >
          {all.isFetchingNextPage && <Loader2 className="animate-spin" aria-hidden />}
          Daha fazla yorum
        </Button>
      )}
      {writable && boxOpen && <CommentBox postId={post.id} autoFocus={focusBox} />}
      <ConfirmDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(undefined)}
        title="Yorum silinsin mi?"
        description="Yorum silinecek. Bu işlem geri alınamaz."
        confirmLabel="Sil"
        destructive
        onConfirm={async () => {
          if (!deleting) return;
          await del.mutateAsync(deleting.id);
          toast.success("Yorum silindi.");
        }}
      />
    </div>
  );
}
