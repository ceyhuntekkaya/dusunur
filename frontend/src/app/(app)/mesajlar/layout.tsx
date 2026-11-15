"use client";

import { useParams } from "next/navigation";
import { ConversationList } from "@/features/mesajlar/components/ConversationList";
import { useChatRealtime } from "@/features/mesajlar/useChatRealtime";
import { cn } from "@/lib/utils";

export default function MessagesLayout({ children }: { children: React.ReactNode }) {
  const params = useParams<{ id?: string }>();
  const parsed = params.id ? Number(params.id) : null;
  const activeId = parsed !== null && Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  useChatRealtime(activeId);

  return (
    <div className="lg:flex lg:h-[calc(100dvh-100px)] lg:overflow-hidden lg:rounded-xl lg:border lg:bg-card">
      <aside
        className={cn(
          "min-h-0 flex-col lg:flex lg:w-[340px] lg:shrink-0 lg:border-r",
          params.id ? "hidden" : "flex",
        )}
      >
        <ConversationList activeId={activeId} />
      </aside>
      <section className={cn("min-w-0 flex-1 lg:flex lg:flex-col", !params.id && "hidden")}>{children}</section>
    </div>
  );
}
