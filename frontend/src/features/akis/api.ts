"use client";

import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query";
import { api, type CursorResponse } from "@/lib/api";
import type { DirGroup } from "@/features/core/types";
import type {
  CommentDto,
  CreatePostRequest,
  CreatePostResponse,
  FeedSeason,
  LikeResponse,
  PostDto,
  ReadStatusResponse,
} from "./types";

type Patch = (p: PostDto) => PostDto;

function mapShape(data: unknown, fn: Patch, id: number): unknown {
  if (!data) return data;
  if (Array.isArray(data)) return data.map((p: PostDto) => (p.id === id ? fn(p) : p));
  const obj = data as Record<string, unknown>;
  if (Array.isArray(obj.pages)) {
    return {
      ...obj,
      pages: (obj.pages as CursorResponse<PostDto>[]).map((pg) => ({
        ...pg,
        items: pg.items.map((p) => (p.id === id ? fn(p) : p)),
      })),
    };
  }
  if (Array.isArray(obj.items)) {
    return { ...obj, items: (obj.items as PostDto[]).map((p) => (p.id === id ? fn(p) : p)) };
  }
  if (obj.id === id) return fn(obj as unknown as PostDto);
  return data;
}

export function patchPost(qc: QueryClient, id: number, fn: Patch) {
  for (const key of ["posts", "pinned", "post", "unread-important"]) {
    qc.setQueriesData({ queryKey: ["feed", key] }, (d: unknown) => mapShape(d, fn, id));
  }
}

function refreshBadges(qc: QueryClient) {
  void qc.invalidateQueries({ queryKey: ["me", "badges"] });
  void qc.invalidateQueries({ queryKey: ["feed", "unread-important"] });
}

export function useFeedSeasons() {
  return useQuery({
    queryKey: ["feed", "seasons"],
    queryFn: () => api<FeedSeason[]>("/api/feed/seasons"),
    staleTime: 5 * 60_000,
  });
}

export function useMyGroups() {
  return useQuery({
    queryKey: ["groups", "mine"],
    queryFn: () => api<DirGroup[]>("/api/groups/mine"),
    staleTime: 60_000,
  });
}

export function usePinnedPosts(enabled: boolean) {
  return useQuery({
    queryKey: ["feed", "pinned"],
    queryFn: () => api<PostDto[]>("/api/feed/pinned"),
    enabled,
  });
}

export function useUnreadImportant() {
  return useQuery({
    queryKey: ["feed", "unread-important"],
    queryFn: () => api<CursorResponse<PostDto>>("/api/feed/posts?filter=unreadImportant&size=5"),
  });
}

export function usePost(id: number) {
  return useQuery({
    queryKey: ["feed", "post", id],
    queryFn: () => api<PostDto>(`/api/feed/posts/${id}`),
    retry: false,
    enabled: id > 0,
    meta: { silentError: true },
  });
}

export function usePostComments(id: number, enabled: boolean) {
  return useInfiniteQuery({
    queryKey: ["feed", "comments", id],
    initialPageParam: undefined as number | undefined,
    queryFn: ({ pageParam }) => {
      const qs = new URLSearchParams({ size: "50" });
      if (pageParam !== undefined) qs.set("after", String(pageParam));
      return api<CursorResponse<CommentDto>>(`/api/feed/posts/${id}/comments?${qs.toString()}`);
    },
    getNextPageParam: (last) => last.nextCursor ?? undefined,
    enabled,
  });
}

export function useCreatePost() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreatePostRequest) =>
      api<CreatePostResponse>("/api/feed/posts", { method: "POST", json: body }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["feed"] });
      refreshBadges(qc);
    },
  });
}

export function useEditPost() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: number; content?: string }) =>
      api<PostDto>(`/api/feed/posts/${v.id}`, { method: "PUT", json: { content: v.content } }),
    onSuccess: (post) => patchPost(qc, post.id, () => post),
  });
}

export function useDeletePost() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/feed/posts/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["feed"] });
      refreshBadges(qc);
    },
  });
}

export function usePinPost() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: number; pin: boolean }) =>
      api<void>(`/api/feed/posts/${v.id}/${v.pin ? "pin" : "unpin"}`, { method: "POST" }),
    onSuccess: () => void qc.invalidateQueries({ queryKey: ["feed"] }),
  });
}

export function useToggleLike() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { post: PostDto; like: boolean }) =>
      api<LikeResponse>(`/api/feed/posts/${v.post.id}/like`, { method: v.like ? "POST" : "DELETE" }),
    onMutate: ({ post, like }) => {
      patchPost(qc, post.id, (p) => ({
        ...p,
        liked: like,
        likeCount: Math.max(0, p.likeCount + (like === p.liked ? 0 : like ? 1 : -1)),
      }));
    },
    onError: (_e, { post }) => {
      patchPost(qc, post.id, (p) => ({ ...p, liked: post.liked, likeCount: post.likeCount }));
    },
    onSuccess: (res, { post }) => {
      patchPost(qc, post.id, (p) => ({ ...p, liked: res.liked, likeCount: res.likeCount }));
    },
  });
}

export function useMarkRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/feed/posts/${id}/read`, { method: "POST" }),
    onSuccess: (_v, id) => {
      patchPost(qc, id, (p) => ({ ...p, readByMe: true }));
      refreshBadges(qc);
    },
  });
}

export function useReadStatus(id: number, enabled: boolean) {
  return useQuery({
    queryKey: ["feed", "reads", id],
    queryFn: () => api<ReadStatusResponse>(`/api/feed/posts/${id}/reads`),
    enabled,
    staleTime: 0,
  });
}

export function useRemind(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => api<{ notified: number }>(`/api/feed/posts/${id}/remind`, { method: "POST" }),
    onSuccess: () => void qc.invalidateQueries({ queryKey: ["feed", "reads", id] }),
  });
}

export function useAddComment(postId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (content: string) =>
      api<CommentDto>(`/api/feed/posts/${postId}/comments`, { method: "POST", json: { content } }),
    onSuccess: (c) => {
      patchPost(qc, postId, (p) => ({
        ...p,
        commentCount: p.commentCount + 1,
        latestComments: [...p.latestComments, c].slice(-2),
      }));
      void qc.invalidateQueries({ queryKey: ["feed", "comments", postId] });
    },
  });
}

export function useDeleteComment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/feed/comments/${id}`, { method: "DELETE" }),
    onSuccess: () => void qc.invalidateQueries({ queryKey: ["feed"] }),
  });
}

