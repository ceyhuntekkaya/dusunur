"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { TargetRef } from "@/features/core/types";
import type {
  DmsLevel,
  FileRow,
  FolderContent,
  FolderNode,
  FolderRow,
  GrantRow,
  OnConflict,
  RestoreResult,
  SearchResult,
  SpaceRow,
  TrashRow,
} from "./types";

function useRefresh() {
  const qc = useQueryClient();
  return () => qc.invalidateQueries({ queryKey: ["dms"] });
}

export function useSpaces() {
  return useQuery({ queryKey: ["dms", "spaces"], queryFn: () => api<SpaceRow[]>("/api/dms/spaces") });
}

export function useSpace(id: number) {
  return useQuery({
    queryKey: ["dms", "space", id],
    queryFn: () => api<SpaceRow>(`/api/dms/spaces/${id}`),
    enabled: id > 0,
  });
}

export function useCreateSpace() {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: (v: { name: string; description?: string }) =>
      api<SpaceRow>("/api/dms/spaces", { method: "POST", json: v }),
    onSuccess: () => refresh(),
  });
}

export function useUpdateSpace(id: number) {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: (v: { name: string; description?: string }) =>
      api<SpaceRow>(`/api/dms/spaces/${id}`, { method: "PUT", json: v }),
    onSuccess: () => refresh(),
  });
}

export function useDeleteSpace(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => api<void>(`/api/dms/spaces/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      qc.removeQueries({ queryKey: ["dms", "space", id] });
      qc.removeQueries({ queryKey: ["dms", "items", id] });
      void qc.invalidateQueries({ queryKey: ["dms", "spaces"] });
    },
  });
}

export function useGrants(spaceId: number, enabled: boolean) {
  return useQuery({
    queryKey: ["dms", "grants", spaceId],
    queryFn: () => api<GrantRow[]>(`/api/dms/spaces/${spaceId}/grants`),
    enabled,
  });
}

export function useAddGrant(spaceId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { subject: TargetRef; level: DmsLevel }) =>
      api<GrantRow>(`/api/dms/spaces/${spaceId}/grants`, { method: "POST", json: v }),
    onSettled: () => qc.invalidateQueries({ queryKey: ["dms", "grants", spaceId] }),
  });
}

export function useUpdateGrant(spaceId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: number; level: DmsLevel }) =>
      api<GrantRow>(`/api/dms/spaces/${spaceId}/grants/${v.id}`, { method: "PUT", json: { level: v.level } }),
    onSettled: () => qc.invalidateQueries({ queryKey: ["dms"] }),
  });
}

export function useRemoveGrant(spaceId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/dms/spaces/${spaceId}/grants/${id}`, { method: "DELETE" }),
    onSettled: () => qc.invalidateQueries({ queryKey: ["dms"] }),
  });
}

export function useFolderContent(spaceId: number, folderId?: number) {
  return useQuery({
    queryKey: ["dms", "items", spaceId, folderId ?? "root"],
    queryFn: () =>
      api<FolderContent>(
        `/api/dms/spaces/${spaceId}/items${folderId !== undefined ? `?folderId=${folderId}` : ""}`,
      ),
    enabled: spaceId > 0,
  });
}

export function useFolderNodes(spaceId: number, enabled: boolean) {
  return useQuery({
    queryKey: ["dms", "folders", spaceId],
    queryFn: () => api<FolderNode[]>(`/api/dms/spaces/${spaceId}/folders`),
    enabled,
    staleTime: 0,
  });
}

export function useCreateFolder(spaceId: number) {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: (v: { parentId?: number; name: string }) =>
      api<FolderRow>(`/api/dms/spaces/${spaceId}/folders`, { method: "POST", json: v }),
    onSuccess: () => refresh(),
  });
}

export function useRenameItem(kind: "folder" | "file") {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: (v: { id: number; name: string }) =>
      api<FolderRow | FileRow>(`/api/dms/${kind}s/${v.id}`, { method: "PUT", json: { name: v.name } }),
    onSuccess: () => refresh(),
  });
}

export function useMoveItem() {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: (v: { kind: "folder" | "file"; id: number; targetFolderId?: number }) =>
      api<void>(`/api/dms/${v.kind}s/${v.id}/move`, {
        method: "POST",
        json: { targetFolderId: v.targetFolderId },
      }),
    onSuccess: () => refresh(),
  });
}

export function useTrashItem() {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: (v: { kind: "folder" | "file"; id: number }) =>
      api<void>(`/api/dms/${v.kind}s/${v.id}`, { method: "DELETE" }),
    onSuccess: () => refresh(),
  });
}

export function attachFile(
  spaceId: number,
  body: { folderId?: number; fileId: string; onConflict: OnConflict },
) {
  return api<FileRow>(`/api/dms/spaces/${spaceId}/files`, { method: "POST", json: body });
}

export function useTrash(spaceId: number, enabled: boolean) {
  return useQuery({
    queryKey: ["dms", "trash", spaceId],
    queryFn: () => api<TrashRow[]>(`/api/dms/spaces/${spaceId}/trash`),
    enabled,
  });
}

export function useRestoreItem() {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: (v: { kind: "folder" | "file"; id: number }) =>
      api<RestoreResult>(`/api/dms/trash/${v.kind}/${v.id}/restore`, { method: "POST" }),
    onSuccess: () => refresh(),
  });
}

export function usePurgeItem() {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: (v: { kind: "folder" | "file"; id: number }) =>
      api<void>(`/api/dms/trash/${v.kind}/${v.id}`, { method: "DELETE" }),
    onSuccess: () => refresh(),
  });
}

export function useEmptyTrash(spaceId: number) {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: () => api<{ purged: number }>(`/api/dms/spaces/${spaceId}/trash`, { method: "DELETE" }),
    onSuccess: () => refresh(),
  });
}

export function useDmsSearch(q: string) {
  return useQuery({
    queryKey: ["dms", "search", q],
    queryFn: () => api<SearchResult[]>(`/api/dms/search?q=${encodeURIComponent(q)}`),
    enabled: q.length >= 2,
  });
}

export function useRecentFiles() {
  return useQuery({ queryKey: ["dms", "recent"], queryFn: () => api<SearchResult[]>("/api/dms/recent") });
}

export function useLocateFile(fileId: number) {
  return useQuery({
    queryKey: ["dms", "locate", fileId],
    queryFn: () => api<{ spaceId: number; folderId?: number }>(`/api/dms/files/${fileId}/locate`),
    enabled: fileId > 0,
    retry: false,
    meta: { silentError: true },
  });
}
