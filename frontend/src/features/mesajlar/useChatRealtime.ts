"use client";

import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { api, ApiError } from "@/lib/api";
import { useWsEvent } from "@/lib/ws";
import {
  applyMessageToList,
  chatKeys,
  insertMessage,
  markMessageDeleted,
} from "./api";
import type {
  ChatConversationChangedEvent,
  ChatMessageDeletedEvent,
  ChatMessageEvent,
  ChatReadEvent,
  ConversationDetail,
} from "./types";

export function useChatRealtime(activeId: number | null) {
  const qc = useQueryClient();
  const router = useRouter();

  useWsEvent<ChatMessageEvent>("CHAT_MESSAGE", ({ conversationId, message }) => {
    insertMessage(qc, message);
    const watching = conversationId === activeId && document.visibilityState === "visible";
    applyMessageToList(qc, message, !watching);
  });

  useWsEvent<ChatMessageDeletedEvent>("CHAT_MESSAGE_DELETED", ({ conversationId, messageId }) => {
    markMessageDeleted(qc, conversationId, messageId);
    void qc.invalidateQueries({ queryKey: chatKeys.list });
  });

  useWsEvent<ChatReadEvent>("CHAT_READ", ({ conversationId, lastReadMessageId }) => {
    qc.setQueryData<ConversationDetail>(chatKeys.detail(conversationId), (d) =>
      d ? { ...d, otherLastReadMessageId: lastReadMessageId } : d,
    );
  });

  useWsEvent<ChatConversationChangedEvent>("CHAT_CONVERSATION_CHANGED", ({ conversationId }) => {
    void qc.invalidateQueries({ queryKey: chatKeys.list });
    if (conversationId !== activeId) {
      void qc.invalidateQueries({ queryKey: chatKeys.detail(conversationId) });
      return;
    }
    qc.fetchQuery({
      queryKey: chatKeys.detail(conversationId),
      queryFn: () => api<ConversationDetail>(`/api/chat/conversations/${conversationId}`),
      staleTime: 0,
      meta: { silentError: true },
    }).catch((err) => {
      if (err instanceof ApiError && err.code === "CHAT_CONVERSATION_NOT_FOUND") {
        toast("Bu sohbete artık erişiminiz yok.");
        router.replace("/mesajlar");
      }
    });
  });
}
