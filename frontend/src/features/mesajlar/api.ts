"use client";

import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
  type InfiniteData,
  type QueryClient,
} from "@tanstack/react-query";
import { api, type CursorResponse } from "@/lib/api";
import type {
  ConversationDetail,
  ConversationRow,
  MessageDto,
  SendMessageRequest,
} from "./types";

export const chatKeys = {
  list: ["chat", "conversations"] as const,
  detail: (id: number) => ["chat", "conversation", id] as const,
  messages: (id: number) => ["chat", "messages", id] as const,
};

type MsgPages = InfiniteData<CursorResponse<MessageDto>, number | undefined>;

function refreshBadges(qc: QueryClient) {
  void qc.invalidateQueries({ queryKey: ["me", "badges"] });
}

export function insertMessage(qc: QueryClient, msg: MessageDto) {
  qc.setQueryData<MsgPages>(chatKeys.messages(msg.conversationId), (d) => {
    if (!d) return d;
    const known = d.pages.some((p) => p.items.some((m) => m.id === msg.id || m.clientId === msg.clientId));
    if (known) {
      return {
        ...d,
        pages: d.pages.map((p) => ({
          ...p,
          items: p.items.map((m) => (m.id === msg.id || m.clientId === msg.clientId ? msg : m)),
        })),
      };
    }
    const [first, ...rest] = d.pages;
    return { ...d, pages: [{ ...first, items: [msg, ...first.items] }, ...rest] };
  });
}

export function markMessageDeleted(qc: QueryClient, conversationId: number, messageId: number) {
  qc.setQueryData<MsgPages>(chatKeys.messages(conversationId), (d) => {
    if (!d) return d;
    return {
      ...d,
      pages: d.pages.map((p) => ({
        ...p,
        items: p.items.map((m) => {
          if (m.id === messageId) {
            return { ...m, deleted: true, content: undefined, attachments: [], canDelete: false };
          }
          if (m.replyTo?.id === messageId) return { ...m, replyTo: { ...m.replyTo, deleted: true } };
          return m;
        }),
      })),
    };
  });
}

export function applyMessageToList(qc: QueryClient, msg: MessageDto, countUnread: boolean) {
  const rows = qc.getQueryData<ConversationRow[]>(chatKeys.list);
  if (!rows) return;
  const row = rows.find((r) => r.id === msg.conversationId);
  if (!row) {
    void qc.invalidateQueries({ queryKey: chatKeys.list });
    return;
  }
  const updated: ConversationRow = {
    ...row,
    unreadCount: !msg.mine && countUnread ? row.unreadCount + 1 : row.unreadCount,
    lastMessage: {
      mine: msg.mine,
      senderFirstName: msg.sender.fullName.split(" ")[0],
      snippet: messageSnippet(msg),
      createdAt: msg.createdAt,
      deleted: false,
    },
  };
  qc.setQueryData<ConversationRow[]>(chatKeys.list, [updated, ...rows.filter((r) => r.id !== row.id)]);
}

export function patchConversation(qc: QueryClient, id: number, patch: Partial<ConversationRow>) {
  qc.setQueryData<ConversationRow[]>(chatKeys.list, (rows) =>
    rows?.map((r) => (r.id === id ? { ...r, ...patch } : r)),
  );
  qc.setQueryData<ConversationDetail>(chatKeys.detail(id), (d) => (d ? { ...d, ...patch } : d));
}

export function messageSnippet(m: Pick<MessageDto, "content" | "attachments">): string {
  const text = m.content?.replace(/\s*\n+\s*/g, " ").trim();
  if (text) return text.slice(0, 80);
  const first = m.attachments[0];
  if (!first) return "";
  const base = first.kind === "IMAGE" ? "📷 Fotoğraf" : first.kind === "VIDEO" ? "🎥 Video" : "📎 Dosya";
  return m.attachments.length > 1 ? `${base} (+${m.attachments.length - 1})` : base;
}

export function useConversations() {
  return useQuery({
    queryKey: chatKeys.list,
    queryFn: () => api<ConversationRow[]>("/api/chat/conversations"),
    staleTime: 0,
  });
}

export function useConversation(id: number) {
  return useQuery({
    queryKey: chatKeys.detail(id),
    queryFn: () => api<ConversationDetail>(`/api/chat/conversations/${id}`),
    enabled: id > 0,
    retry: false,
    staleTime: 0,
    meta: { silentError: true },
  });
}

export function useMessages(id: number) {
  return useInfiniteQuery({
    queryKey: chatKeys.messages(id),
    initialPageParam: undefined as number | undefined,
    queryFn: ({ pageParam }) => {
      const qs = new URLSearchParams({ size: "50" });
      if (pageParam !== undefined) qs.set("before", String(pageParam));
      return api<CursorResponse<MessageDto>>(`/api/chat/conversations/${id}/messages?${qs.toString()}`);
    },
    getNextPageParam: (last) => last.nextCursor ?? undefined,
    enabled: id > 0,
    staleTime: 0,
    meta: { silentError: true },
  });
}

export function useOpenDirect() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (userId: number) =>
      api<ConversationRow>("/api/chat/direct", { method: "POST", json: { userId } }),
    onSuccess: () => void qc.invalidateQueries({ queryKey: chatKeys.list }),
  });
}

export function useCreateChatGroup() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { title: string; memberIds: number[] }) =>
      api<ConversationRow>("/api/chat/groups", { method: "POST", json: body }),
    onSuccess: () => void qc.invalidateQueries({ queryKey: chatKeys.list }),
  });
}

export function useRenameConversation(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (title: string) =>
      api<ConversationDetail>(`/api/chat/conversations/${id}`, { method: "PUT", json: { title } }),
    onSuccess: (d) => {
      qc.setQueryData(chatKeys.detail(id), d);
      void qc.invalidateQueries({ queryKey: chatKeys.list });
    },
  });
}

export function useAddMembers(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (userIds: number[]) =>
      api<ConversationDetail>(`/api/chat/conversations/${id}/members`, { method: "POST", json: { userIds } }),
    onSuccess: (d) => {
      qc.setQueryData(chatKeys.detail(id), d);
      void qc.invalidateQueries({ queryKey: chatKeys.list });
    },
  });
}

export function useRemoveMember(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (userId: number) =>
      api<void>(`/api/chat/conversations/${id}/members/${userId}`, { method: "DELETE" }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: chatKeys.detail(id) });
      void qc.invalidateQueries({ queryKey: chatKeys.list });
    },
  });
}

export function useLeaveConversation(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => api<void>(`/api/chat/conversations/${id}/leave`, { method: "POST" }),
    onSuccess: () => {
      qc.removeQueries({ queryKey: chatKeys.detail(id) });
      qc.removeQueries({ queryKey: chatKeys.messages(id) });
      void qc.invalidateQueries({ queryKey: chatKeys.list });
      refreshBadges(qc);
    },
  });
}

export function useMuteConversation(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (muted: boolean) =>
      api<void>(`/api/chat/conversations/${id}/mute`, { method: "POST", json: { muted } }),
    onSuccess: (_v, muted) => {
      patchConversation(qc, id, { muted });
      refreshBadges(qc);
    },
  });
}

export function useSendMessage(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: SendMessageRequest) =>
      api<MessageDto>(`/api/chat/conversations/${id}/messages`, { method: "POST", json: body }),
    onSuccess: (msg) => {
      insertMessage(qc, msg);
      applyMessageToList(qc, msg, false);
    },
  });
}

export function useDeleteMessage(conversationId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (messageId: number) => api<void>(`/api/chat/messages/${messageId}`, { method: "DELETE" }),
    onSuccess: (_v, messageId) => {
      markMessageDeleted(qc, conversationId, messageId);
      void qc.invalidateQueries({ queryKey: chatKeys.list });
    },
  });
}

export function useMarkRead(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (lastMessageId: number) =>
      api<void>(`/api/chat/conversations/${id}/read`, { method: "POST", json: { lastMessageId } }),
    onSuccess: () => {
      patchConversation(qc, id, { unreadCount: 0 });
      refreshBadges(qc);
    },
    meta: { silentError: true },
  });
}
