"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, SearchX } from "lucide-react";
import { ArchiveBanner } from "@/components/common/ArchiveBanner";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { ListSkeleton } from "@/components/common/Skeletons";
import { useFeedSeasons, usePost } from "@/features/akis/api";
import { PostCard } from "@/features/akis/components/PostCard";
import { ApiError } from "@/lib/api";

const NOT_FOUND = "Gönderi bulunamadı veya silinmiş. Size gönderilmemiş bir paylaşım da olabilir.";

export default function PostDetailPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const valid = Number.isInteger(id) && id > 0;
  const post = usePost(valid ? id : 0);
  const seasons = useFeedSeasons();

  let body: React.ReactNode;
  if (!valid || (post.isError && post.error instanceof ApiError && post.error.status === 404)) {
    body = <EmptyState icon={SearchX} message={NOT_FOUND} action={{ label: "Akışa dön", href: "/akis" }} />;
  } else if (post.isPending) {
    body = <ListSkeleton rows={1} />;
  } else if (post.isError) {
    body = <ErrorState error={post.error} onRetry={() => void post.refetch()} />;
  } else {
    const p = post.data;
    body = (
      <>
        {p.archived && <ArchiveBanner seasonName={seasons.data?.find((s) => s.id === p.seasonId)?.name ?? ""} />}
        <PostCard post={p} expanded onDeleted={() => router.replace("/akis")} />
      </>
    );
  }

  return (
    <div className="mx-auto max-w-[640px]">
      <Link href="/akis" className="mb-4 inline-flex h-11 items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground md:h-10">
        <ArrowLeft className="size-4" aria-hidden />
        Akış
      </Link>
      {body}
    </div>
  );
}
