"use client";

import { useParams } from "next/navigation";
import { SearchX } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { SpaceExplorer } from "@/features/dokumanlar/components/SpaceExplorer";

export default function Page() {
  const params = useParams<{ spaceId: string }>();
  const id = Number(params.spaceId);
  if (!Number.isInteger(id) || id <= 0) {
    return (
      <EmptyState
        icon={SearchX}
        message="Doküman alanı bulunamadı veya bu alana erişiminiz yok."
        action={{ label: "Dokümanlar'a dön", href: "/dokumanlar" }}
      />
    );
  }
  return <SpaceExplorer spaceId={id} />;
}
