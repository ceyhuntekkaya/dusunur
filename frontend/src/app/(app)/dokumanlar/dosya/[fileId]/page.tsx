"use client";

import { useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { SearchX } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { ListSkeleton } from "@/components/common/Skeletons";
import { useLocateFile } from "@/features/dokumanlar/api";

export default function Page() {
  const params = useParams<{ fileId: string }>();
  const router = useRouter();
  const id = Number(params.fileId);
  const valid = Number.isInteger(id) && id > 0;
  const locate = useLocateFile(valid ? id : 0);

  useEffect(() => {
    if (!locate.data) return;
    const q = new URLSearchParams();
    if (locate.data.folderId !== undefined) q.set("klasor", String(locate.data.folderId));
    q.set("vurgu", String(id));
    router.replace(`/dokumanlar/${locate.data.spaceId}?${q.toString()}`);
  }, [locate.data, id, router]);

  if (!valid || locate.isError) {
    return (
      <EmptyState
        icon={SearchX}
        message="Bu dosya bulunamadı veya erişiminiz yok. Erişime ihtiyacınız varsa alanın yöneticisiyle iletişime geçin."
        action={{ label: "Dokümanlar'a dön", href: "/dokumanlar" }}
      />
    );
  }
  return <ListSkeleton rows={2} />;
}
