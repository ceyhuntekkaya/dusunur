"use client";

import { useParams } from "next/navigation";
import { SearchX } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { ChatView } from "@/features/mesajlar/components/ChatView";

export default function ChatPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  if (!Number.isInteger(id) || id <= 0) {
    return (
      <EmptyState
        icon={SearchX}
        message="Sohbet bulunamadı veya bu sohbete erişiminiz yok."
        action={{ label: "Mesajlara dön", href: "/mesajlar" }}
      />
    );
  }
  return <ChatView key={id} id={id} />;
}
