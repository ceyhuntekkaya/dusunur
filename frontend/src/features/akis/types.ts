import type { TargetRef, TargetView } from "@/features/core/types";

export type FeedAuthor = { id: number; fullName: string; title?: string; branchName?: string; former: boolean };
export type FeedAttachment = {
  fileId: string;
  name: string;
  contentType: string;
  sizeBytes: number;
  kind: "IMAGE" | "VIDEO" | "FILE";
};
export type CommentDto = {
  id: number;
  postId: number;
  author: FeedAuthor;
  content: string;
  createdAt: string;
  canDelete: boolean;
};
export type PostDto = {
  id: number;
  seasonId: number;
  archived: boolean;
  author: FeedAuthor;
  content?: string;
  important: boolean;
  pinned: boolean;
  editedAt?: string;
  createdAt: string;
  targets: TargetView[];
  attachments: FeedAttachment[];
  likeCount: number;
  liked: boolean;
  commentCount: number;
  latestComments: CommentDto[];
  readByMe?: boolean;
  readStats?: { read: number; total: number };
  canEdit: boolean;
  canDelete: boolean;
  canPin: boolean;
};
export type CreatePostRequest = { targets: TargetRef[]; content?: string; fileIds: string[]; important: boolean };
export type CreatePostResponse = { post: PostDto; recipientCount: number };
export type FeedSeason = { id: number; name: string; active: boolean };
export type ReadUser = { userId: number; fullName: string; title?: string; branchName?: string; readAt?: string };
export type ReadStatusResponse = { read: ReadUser[]; unread: ReadUser[]; lastReminderAt?: string };
export type LikeResponse = { liked: boolean; likeCount: number };
export type NewPostEvent = { postId: number; seasonId: number };

export type FeedFilter = "all" | "important" | "unreadImportant" | "direct" | "mine" | "group";
