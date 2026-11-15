"use client";

import { Suspense, useEffect } from "react";
import { usePathname } from "next/navigation";
import { Loader2 } from "lucide-react";
import { ConnectionBanner } from "@/components/common/ConnectionBanner";
import { InstallCard } from "@/components/common/InstallCard";
import { PushPromptCard } from "@/components/common/PushPromptCard";
import { ErrorState } from "@/components/common/ErrorState";
import { PageSkeleton } from "@/components/common/Skeletons";
import { BottomNav } from "@/components/layout/BottomNav";
import { TopBar } from "@/components/layout/TopBar";
import { RealtimeBridge } from "@/features/core/components/RealtimeBridge";
import { KvkkDialog } from "@/features/core/components/KvkkDialog";
import { ApiError } from "@/lib/api";
import { useMe } from "@/lib/me";
import { SESSION_ENDING_CODES } from "@/lib/query-client";

export function FullScreenLoading() {
  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-4" aria-busy="true">
      <img src="/logo.svg" alt="" className="size-14" />
      <Loader2 className="size-6 animate-spin text-primary" aria-label="Yükleniyor" />
    </div>
  );
}

export function AppShell({ children }: { children: React.ReactNode }) {
  const me = useMe();
  const pathname = usePathname();

  useEffect(() => {
    if ("serviceWorker" in navigator) {
      navigator.serviceWorker.register("/sw.js").catch(() => {});
    }
  }, []);

  if (me.isPending) return <FullScreenLoading />;

  if (me.isError) {
    const redirecting = me.error instanceof ApiError && SESSION_ENDING_CODES.includes(me.error.code);
    if (redirecting) return <FullScreenLoading />;
    return (
      <div className="flex min-h-dvh items-center justify-center">
        <ErrorState error={me.error} onRetry={() => void me.refetch()} />
      </div>
    );
  }

  const user = me.data;

  return (
    <div className="min-h-dvh">
      <ConnectionBanner />
      <TopBar me={user} />
      <div>
        <main className="mx-auto max-w-[1128px] px-4 pt-4 pb-[calc(64px+env(safe-area-inset-bottom)+16px)] md:px-6 md:pt-6 md:pb-6">
          {!user.kvkkRequired && (
            <>
              {pathname === "/akis" && <InstallCard />}
              <PushPromptCard />
            </>
          )}
          {user.kvkkRequired ? <PageSkeleton /> : <Suspense fallback={<PageSkeleton />}>{children}</Suspense>}
        </main>
      </div>
      <BottomNav me={user} />
      {!user.kvkkRequired && <RealtimeBridge />}
      {user.kvkkRequired && <KvkkDialog />}
    </div>
  );
}
