"use client";

import "./globals.css";
import { ErrorState } from "@/components/common/ErrorState";

export default function GlobalError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <html lang="tr" className="h-full antialiased">
      <body className="flex min-h-full flex-col">
        <div className="flex min-h-dvh items-center justify-center">
          <ErrorState message="Bir sorun oluştu. Sayfayı yenileyin; sorun sürerse okul yönetimiyle iletişime geçin." onRetry={reset} />
        </div>
      </body>
    </html>
  );
}
