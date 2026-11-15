"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowUp, ChevronDown, Image as ImageIcon, MoreHorizontal, Paperclip, Plus, Users, X } from "lucide-react";
import { ArchiveBanner } from "@/components/common/ArchiveBanner";
import { EmptyState } from "@/components/common/EmptyState";
import { InfiniteList } from "@/components/common/InfiniteList";
import { UserAvatar } from "@/components/common/UserAvatar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useFeedSeasons, useMyGroups, usePinnedPosts } from "@/features/akis/api";
import { Composer } from "@/features/akis/components/Composer";
import { PostCard } from "@/features/akis/components/PostCard";
import { ProfileCard } from "@/features/akis/components/ProfileCard";
import { SideSummary } from "@/features/akis/components/SideSummary";
import { UnreadImportantCard } from "@/features/akis/components/UnreadImportantCard";
import type { FeedFilter, NewPostEvent, PostDto } from "@/features/akis/types";
import { useBadges } from "@/features/core/api";
import { useMe } from "@/lib/me";
import { useUrlParams } from "@/lib/url-state";
import { useWsEvent, useWsReconnect } from "@/lib/ws";
import { cn } from "@/lib/utils";

const FILTER_BY_PARAM: Record<string, FeedFilter> = {
  onemli: "important",
  okunmamis: "unreadImportant",
  "bana-ozel": "direct",
  benim: "mine",
  grup: "group",
};

const PARAM_BY_FILTER: Partial<Record<FeedFilter, string>> = {
  important: "onemli",
  unreadImportant: "okunmamis",
  direct: "bana-ozel",
  mine: "benim",
  group: "grup",
};

const CHIPS: { filter: FeedFilter; label: string }[] = [
  { filter: "all", label: "Tümü" },
  { filter: "important", label: "Önemli" },
  { filter: "direct", label: "Bana Özel" },
  { filter: "mine", label: "Paylaşımlarım" },
];

const EMPTY: Record<FeedFilter, { message: string; share?: boolean }> = {
  all: { message: "Henüz paylaşım yok.", share: true },
  important: { message: "Önemli duyuru yok." },
  unreadImportant: { message: "Okunmamış önemli duyurunuz yok. Harika!" },
  direct: { message: "Size özel bir paylaşım yok." },
  mine: { message: "Henüz bir şey paylaşmadınız.", share: true },
  group: { message: "Bu grupta henüz paylaşım yok." },
};

function Chip({ active, onClick, children }: { active: boolean; onClick?: () => void; children: React.ReactNode }) {
  return (
    <Button
      type="button"
      variant={active ? "default" : "outline"}
      className={cn("h-10 shrink-0 rounded-full px-4", active && "border-success bg-success hover:bg-success/90")}
      aria-pressed={active}
      onClick={onClick}
    >
      {children}
    </Button>
  );
}

export default function FeedPage() {
  const me = useMe().data;
  const url = useUrlParams();
  const qc = useQueryClient();
  const badges = useBadges(true);
  const seasons = useFeedSeasons();
  const groups = useMyGroups();
  const [composer, setComposer] = useState(false);
  const [hasNew, setHasNew] = useState(false);

  const activeSeasonId = me?.season?.id;
  const yil = url.get("yil");
  const seasonId = yil && Number(yil) !== activeSeasonId ? Number(yil) : undefined;
  const archived = seasonId !== undefined;
  const groupParam = url.get("grup");
  const rawFilter = FILTER_BY_PARAM[url.get("filtre")] ?? "all";
  const groupId = groupParam ? Number(groupParam) : undefined;
  const filter: FeedFilter = rawFilter === "group" && !groupId ? "all" : rawFilter;
  const group = groups.data?.find((g) => g.id === groupId);
  const unread = badges.data?.feedImportant ?? 0;

  const pinned = usePinnedPosts(filter === "all" && !archived);

  const refresh = () => void qc.invalidateQueries({ queryKey: ["feed"] });
  const toTop = () => window.scrollTo({ top: 0, behavior: "smooth" });

  useWsEvent<NewPostEvent>("FEED_NEW_POST", (e) => {
    if (e.seasonId !== activeSeasonId || archived) return;
    if (window.scrollY < 80) refresh();
    else setHasNew(true);
    void qc.invalidateQueries({ queryKey: ["feed", "unread-important"] });
  });
  useWsReconnect(refresh);

  useEffect(() => {
    if (!hasNew) return;
    function onScroll() {
      if (window.scrollY < 80) setHasNew(false);
    }
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, [hasNew]);

  function setFilter(next: FeedFilter, grup?: number) {
    url.set({ filtre: PARAM_BY_FILTER[next], grup: grup ? String(grup) : undefined });
  }

  const empty = archived ? (
    <EmptyState icon={Plus} message="Bu yılda görüntüleyebileceğiniz paylaşım yok." />
  ) : (
    <EmptyState
      icon={Plus}
      message={EMPTY[filter].message}
      action={EMPTY[filter].share ? { label: "Paylaşım Yap", onClick: () => setComposer(true) } : undefined}
    />
  );

  const seasonName = seasons.data?.find((s) => s.id === seasonId)?.name ?? "";

  return (
    <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
      <div className="min-w-0">
        <div className="mb-3 hidden rounded-xl border bg-card p-3 md:block">
          <div className="flex items-center gap-3">
            {me && <UserAvatar userId={me.id} name={me.fullName} size="md" className="size-12" />}
            <button
              type="button"
              onClick={() => setComposer(true)}
              className="flex h-12 flex-1 items-center rounded-full border border-input px-4 text-left text-[15px] font-medium text-muted-foreground outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              Ne paylaşmak istersiniz?
            </button>
          </div>
          <div className="mt-2 flex gap-1">
            <button
              type="button"
              onClick={() => setComposer(true)}
              className="flex h-10 flex-1 items-center justify-center gap-2 rounded-lg text-sm font-medium text-muted-foreground hover:bg-muted"
            >
              <ImageIcon className="size-5 text-[#378FE9]" aria-hidden />
              Fotoğraf
            </button>
            <button
              type="button"
              onClick={() => setComposer(true)}
              className="flex h-10 flex-1 items-center justify-center gap-2 rounded-lg text-sm font-medium text-muted-foreground hover:bg-muted"
            >
              <Paperclip className="size-5 text-[#C37D16]" aria-hidden />
              Dosya
            </button>
          </div>
        </div>

        {unread > 0 && (
          <div className="mb-4 flex items-center gap-3 rounded-lg border-l-4 border-l-important bg-important-bg px-4 py-3 lg:hidden">
            <p className="flex-1 text-sm">Okumanız gereken {unread} önemli duyuru var.</p>
            <Button className="h-11 md:h-10" variant="outline" onClick={() => setFilter("unreadImportant")}>
              Göster
            </Button>
          </div>
        )}

        <div className="mb-4 flex items-center gap-1">
          <div className="-mx-1 flex min-w-0 flex-1 gap-2 overflow-x-auto px-1 py-1">
            {CHIPS.map((c) => (
              <Chip key={c.filter} active={filter === c.filter} onClick={() => setFilter(c.filter)}>
                {c.label}
              </Chip>
            ))}
            {filter === "group" ? (
              <Chip active onClick={() => setFilter("all")}>
                Grup: {group?.name ?? "…"}
                <X className="size-4" aria-label="Grup filtresini kaldır" />
              </Chip>
            ) : (
              <DropdownMenu>
                <DropdownMenuTrigger render={<Button variant="outline" className="h-10 shrink-0 rounded-full px-4 lg:hidden" />}>
                  Gruplar
                  <ChevronDown aria-hidden />
                </DropdownMenuTrigger>
                <DropdownMenuContent align="start" className="min-w-48">
                  {groups.data && groups.data.length > 0 ? (
                    groups.data.map((g) => (
                      <DropdownMenuItem key={g.id} className="min-h-11" onClick={() => setFilter("group", g.id)}>
                        {g.name}
                      </DropdownMenuItem>
                    ))
                  ) : (
                    <DropdownMenuItem disabled className="min-h-11">
                      Üyesi olduğunuz grup yok.
                    </DropdownMenuItem>
                  )}
                </DropdownMenuContent>
              </DropdownMenu>
            )}
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger
              aria-label="Diğer seçenekler"
              className="flex size-11 shrink-0 items-center justify-center rounded-lg outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 md:size-10"
            >
              <MoreHorizontal className="size-5" aria-hidden />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="min-w-48">
              <DropdownMenuSub>
                <DropdownMenuSubTrigger className="min-h-11">Eğitim-öğretim yılı</DropdownMenuSubTrigger>
                <DropdownMenuSubContent>
                  <DropdownMenuRadioGroup
                    value={String(seasonId ?? activeSeasonId ?? "")}
                    onValueChange={(v) => url.set({ yil: Number(v) === activeSeasonId ? undefined : v })}
                  >
                    {(seasons.data ?? []).map((s) => (
                      <DropdownMenuRadioItem key={s.id} value={String(s.id)} className="min-h-11">
                        {s.name}
                      </DropdownMenuRadioItem>
                    ))}
                  </DropdownMenuRadioGroup>
                </DropdownMenuSubContent>
              </DropdownMenuSub>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        {archived && (
          <div className="mb-1">
            <ArchiveBanner seasonName={seasonName} />
            <Button
              variant="link"
              className="-mt-2 mb-3 h-auto p-0"
              nativeButton={false}
              render={<Link href="/akis" />}
            >
              Bu yıla dön
            </Button>
          </div>
        )}

        {hasNew && (
          <div className="sticky top-16 z-20 flex justify-center">
            <Button
              className="h-10 rounded-full shadow-lg"
              onClick={() => {
                setHasNew(false);
                toTop();
                refresh();
              }}
            >
              <ArrowUp aria-hidden />
              Yeni paylaşımlar
            </Button>
          </div>
        )}

        {filter === "all" && !archived && (pinned.data?.length ?? 0) > 0 && (
          <div className="mb-3 space-y-3">
            {pinned.data!.map((p: PostDto) => (
              <PostCard key={p.id} post={p} />
            ))}
          </div>
        )}

        <InfiniteList<PostDto>
          queryKey={["feed", "posts", { seasonId, filter, groupId }]}
          path="/api/feed/posts"
          params={{ seasonId, filter, groupId: filter === "group" ? groupId : undefined }}
          size={10}
          getKey={(p) => p.id}
          renderItem={(p) => <PostCard post={p} />}
          empty={empty}
        />
      </div>

      <aside className="sticky top-[68px] hidden max-h-[calc(100vh-84px)] space-y-2 overflow-y-auto lg:block">
        {me && <ProfileCard me={me} />}
        {groups.data && groups.data.length > 0 && (
          <section className="rounded-xl border bg-card py-3" aria-label="Gruplarım">
            <h2 className="px-4 pb-1 text-sm font-semibold">Gruplarım</h2>
            <ul>
              {groups.data.map((g) => (
                <li key={g.id}>
                  <button
                    type="button"
                    onClick={() => setFilter("group", g.id)}
                    className={cn(
                      "flex min-h-10 w-full items-center gap-2 px-4 text-left text-[13px] font-medium hover:bg-muted",
                      filter === "group" && groupId === g.id && "text-primary",
                    )}
                  >
                    <Users className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                    <span className="truncate">{g.name}</span>
                  </button>
                </li>
              ))}
            </ul>
          </section>
        )}
        <UnreadImportantCard />
        <SideSummary />
      </aside>

      <Button
        aria-label="Paylaş"
        className="fixed right-4 bottom-[calc(64px+env(safe-area-inset-bottom)+16px)] z-30 size-14 rounded-full shadow-lg md:hidden"
        onClick={() => setComposer(true)}
      >
        <Plus className="size-6" aria-hidden />
      </Button>

      <Composer
        open={composer}
        onOpenChange={setComposer}
        onPosted={() => {
          toTop();
          refresh();
        }}
      />
    </div>
  );
}
