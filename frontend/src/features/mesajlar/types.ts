export type ChatPerson = { id: number; fullName: string; title?: string; branchName?: string; former: boolean };
export type ChatAttachment = {
  fileId: string;
  name: string;
  contentType: string;
  sizeBytes: number;
  kind: "IMAGE" | "VIDEO" | "FILE";
};
export type MessageDto = {
  id: number;
  conversationId: number;
  seasonId: number;
  archived: boolean;
  clientId: string;
  sender: ChatPerson;
  content?: string;
  attachments: ChatAttachment[];
  replyTo?: { id: number; senderName: string; snippet: string; deleted: boolean };
  createdAt: string;
  deleted: boolean;
  mine: boolean;
  canDelete: boolean;
};
export type ConversationType = "DIRECT" | "GROUP" | "FREE";
export type ConversationRow = {
  id: number;
  type: ConversationType;
  title: string;
  other?: { id: number; fullName: string; former: boolean };
  memberCount: number;
  muted: boolean;
  readOnly: boolean;
  unreadCount: number;
  lastMessage?: { mine: boolean; senderFirstName: string; snippet: string; createdAt: string; deleted: boolean };
};
export type ConversationDetail = ConversationRow & {
  members: (ChatPerson & { role: "OWNER" | "MEMBER" })[];
  canSend: boolean;
  readOnlyReason?: string;
  canManage: boolean;
  canLeave: boolean;
  otherLastReadMessageId?: number;
};
export type SendMessageRequest = { clientId: string; content?: string; fileIds: string[]; replyToId?: number };

export type ChatMessageEvent = { conversationId: number; message: MessageDto };
export type ChatMessageDeletedEvent = { conversationId: number; messageId: number };
export type ChatReadEvent = { conversationId: number; lastReadMessageId: number };
export type ChatConversationChangedEvent = { conversationId: number };
