"use client";

import { useState } from "react";
import { FolderOpen } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { PageHeader } from "@/components/common/PageHeader";
import { SearchInput } from "@/components/common/SearchInput";
import { Skeleton } from "@/components/ui/skeleton";
import { useMe } from "@/lib/me";
import { useUrlParams } from "@/lib/url-state";
import { useSpaces } from "../api";
import { SearchResults } from "./SearchResults";
import { SpaceCard } from "./SpaceCard";
import { SpaceCreateDialog } from "./SpaceCreateDialog";

export function SpaceListView() {
  const params = useUrlParams();
  const q = params.get("ara");
  const me = useMe();
  const spaces = useSpaces();
  const [creating, setCreating] = useState(false);
  const canCreate = me.data?.permissions.canCreateDmsSpace === true;
  const searching = q.length >= 2;

  let body: React.ReactNode;
  if (searching) {
    body = <SearchResults q={q} />;
  } else if (spaces.isPending) {
    body = (
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3" aria-busy="true" aria-label="Yükleniyor">
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} className="h-36 rounded-xl" />
        ))}
      </div>
    );
  } else if (spaces.isError) {
    body = <ErrorState error={spaces.error} onRetry={() => void spaces.refetch()} />;
  } else if (spaces.data.length === 0) {
    body = canCreate ? (
      <EmptyState
        icon={FolderOpen}
        message="Henüz doküman alanı yok."
        action={{ label: "Alan Oluştur", onClick: () => setCreating(true) }}
      />
    ) : (
      <EmptyState
        icon={FolderOpen}
        message="Size açılmış bir doküman alanı yok. Bir dokümana erişmeniz gerekiyorsa yöneticinizle iletişime geçin."
      />
    );
  } else {
    body = (
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {spaces.data.map((s) => (
          <SpaceCard key={s.id} space={s} isOwner={s.owner.id === me.data?.id} />
        ))}
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Dokümanlar"
        primaryAction={canCreate ? { label: "Alan Oluştur", onClick: () => setCreating(true) } : undefined}
      />
      <SearchInput
        className="mb-4"
        label="Tüm dokümanlarda ara"
        placeholder="Tüm dokümanlarda ara (dosya veya klasör adı)"
        value={q}
        onChange={(v) => params.set({ ara: v })}
      />
      {body}
      {canCreate && <SpaceCreateDialog open={creating} onOpenChange={setCreating} />}
    </div>
  );
}
