"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/ErrorState";
import { FullScreenLoading } from "@/components/layout/AppShell";
import { ApiError } from "@/lib/api";
import { useMe } from "@/lib/me";
import { SESSION_ENDING_CODES } from "@/lib/query-client";

export default function Home() {
  const router = useRouter();
  const me = useMe();
  const error = me.error;
  const sessionCode =
    error instanceof ApiError && SESSION_ENDING_CODES.includes(error.code) ? error.code : null;

  useEffect(() => {
    if (me.data) router.replace(me.data.permissions.member ? "/akis" : "/yonetim");
    else if (sessionCode) {
      router.replace(sessionCode === "UNAUTHENTICATED" ? "/giris" : `/giris?neden=${sessionCode}`);
    }
  }, [me.data, sessionCode, router]);

  if (me.isError && !sessionCode) {
    return (
      <div className="flex min-h-dvh items-center justify-center">
        <ErrorState error={error} onRetry={() => void me.refetch()} />
      </div>
    );
  }
  return <FullScreenLoading />;
}
