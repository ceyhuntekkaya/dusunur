"use client";

import { useWsConnectionLost } from "@/lib/ws";

export function ConnectionBanner() {
  if (!useWsConnectionLost()) return null;
  return (
    <div
      role="status"
      className="sticky top-0 z-30 border-b border-warning/30 bg-warning-bg px-4 py-1 text-center text-[13px] font-medium text-warning"
    >
      Bağlantı yeniden kuruluyor…
    </div>
  );
}
