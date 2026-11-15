"use client";

import { ErrorState } from "@/components/common/ErrorState";

export default function Error({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <div className="flex min-h-dvh items-center justify-center">
      <ErrorState message="Bir sorun oluştu. Sayfayı yenileyin; sorun sürerse okul yönetimiyle iletişime geçin." onRetry={reset} />
    </div>
  );
}
