"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, apiUpload, type PageResponse } from "@/lib/api";
import type {
  ActivationPreview,
  AdminUserRow,
  AuditActionOption,
  AuditRow,
  BranchRow,
  BulkUpdateRequest,
  CopyMembershipsRequest,
  CopyMembershipsResult,
  DeactivationCheck,
  DirBranch,
  DirUser,
  GroupDetail,
  GroupRow,
  MembershipFilter,
  MembershipInput,
  MembershipRow,
  SeasonInput,
  SeasonRow,
  UserInput,
} from "@/features/core/types";

function qs(params: Record<string, string | number | boolean | undefined | null>): string {
  const sp = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== "") sp.set(k, String(v));
  }
  const s = sp.toString();
  return s ? `?${s}` : "";
}

function useRefreshCore() {
  const queryClient = useQueryClient();
  return (opts: { me?: boolean } = {}) => {
    void queryClient.invalidateQueries({ queryKey: ["core"] });
    if (opts.me) void queryClient.invalidateQueries({ queryKey: ["me"] });
  };
}

export function useActiveBranches() {
  return useQuery({
    queryKey: ["core", "branches"],
    queryFn: () => api<DirBranch[]>("/api/branches"),
    staleTime: 60_000,
  });
}

export function useAdminBranches() {
  return useQuery({
    queryKey: ["core", "admin", "branches"],
    queryFn: () => api<BranchRow[]>("/api/admin/branches"),
  });
}

export function useSaveBranch() {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (v: { id?: number; name: string }) =>
      v.id === undefined
        ? api<BranchRow>("/api/admin/branches", { method: "POST", json: { name: v.name } })
        : api<BranchRow>(`/api/admin/branches/${v.id}`, { method: "PUT", json: { name: v.name } }),
    onSuccess: () => refresh(),
  });
}

export function useSetBranchActive() {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (v: { id: number; active: boolean }) =>
      api<void>(`/api/admin/branches/${v.id}/${v.active ? "activate" : "deactivate"}`, { method: "POST" }),
    onSuccess: () => refresh(),
  });
}

export type AdminUsersParams = {
  q: string;
  status: "ACTIVE" | "PASSIVE" | "ALL";
  branchId?: number;
  assigned?: boolean;
  passwordSet?: boolean;
  page: number;
};

export function useAdminUsers(p: AdminUsersParams) {
  return useQuery({
    queryKey: ["core", "admin", "users", p],
    queryFn: () =>
      api<PageResponse<AdminUserRow>>(
        `/api/admin/users${qs({ ...p, size: 25 })}`,
      ),
    placeholderData: keepPreviousData,
  });
}

export function useAdminUserLookup(q: string, seasonId: number | undefined, enabled: boolean) {
  return useQuery({
    queryKey: ["core", "admin", "lookup", { q, seasonId }],
    queryFn: () => api<DirUser[]>(`/api/admin/users/lookup${qs({ q, seasonId })}`),
    enabled: enabled && q.length >= 2,
    placeholderData: keepPreviousData,
    staleTime: 10_000,
  });
}

export function useTitles() {
  return useQuery({
    queryKey: ["core", "admin", "titles"],
    queryFn: () => api<string[]>("/api/admin/titles"),
    staleTime: 60_000,
  });
}

export function useCreateUser() {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (body: UserInput) =>
      api<{ user: AdminUserRow; mailSent: boolean }>("/api/admin/users", { method: "POST", json: body }),
    onSuccess: () => refresh(),
  });
}

export function useUpdateUser() {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (v: { id: number; body: UserInput }) =>
      api<AdminUserRow>(`/api/admin/users/${v.id}`, { method: "PUT", json: v.body }),
    onSuccess: () => refresh({ me: true }),
  });
}

export function useSendSetupLink() {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/admin/users/${id}/send-setup-link`, { method: "POST" }),
    onSuccess: () => refresh(),
  });
}

export function useDeactivationCheck(id: number | undefined) {
  return useQuery({
    queryKey: ["core", "admin", "deactivation-check", id],
    queryFn: () => api<DeactivationCheck>(`/api/admin/users/${id}/deactivation-check`),
    enabled: id !== undefined,
    gcTime: 0,
    staleTime: 0,
  });
}

export function useDeactivateUser() {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (v: { id: number; successorUserId?: number }) =>
      api<void>(`/api/admin/users/${v.id}/deactivate`, {
        method: "POST",
        json: { successorUserId: v.successorUserId },
      }),
    onSuccess: () => refresh(),
  });
}

export function useReactivateUser() {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/admin/users/${id}/reactivate`, { method: "POST" }),
    onSuccess: () => refresh(),
  });
}

export function useImportUsers() {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (v: { file: File; seasonId: number }) => {
      const form = new FormData();
      form.append("file", v.file);
      form.append("seasonId", String(v.seasonId));
      return apiUpload<{ created: number }>("/api/admin/users/import", form);
    },
    onSuccess: () => refresh(),
  });
}

export function useSeasons() {
  return useQuery({
    queryKey: ["core", "admin", "seasons"],
    queryFn: () => api<SeasonRow[]>("/api/admin/seasons"),
  });
}

export function useSeason(id: number) {
  return useQuery({
    queryKey: ["core", "admin", "season", id],
    queryFn: () => api<SeasonRow>(`/api/admin/seasons/${id}`),
  });
}

export function useSaveSeason() {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (v: { id?: number; body: SeasonInput }) =>
      v.id === undefined
        ? api<SeasonRow>("/api/admin/seasons", { method: "POST", json: v.body })
        : api<SeasonRow>(`/api/admin/seasons/${v.id}`, { method: "PUT", json: v.body }),
    onSuccess: () => refresh({ me: true }),
  });
}

export function useDeleteSeason() {
  const queryClient = useQueryClient();
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/admin/seasons/${id}`, { method: "DELETE" }),
    onSuccess: (_data, id) => {
      queryClient.removeQueries({ queryKey: ["core", "admin", "season", id] });
      queryClient.removeQueries({ queryKey: ["core", "admin", "memberships", id] });
      queryClient.removeQueries({ queryKey: ["core", "admin", "unassigned", id] });
      refresh();
    },
  });
}

export type SeasonMembershipsParams = {
  q: string;
  branchId?: number;
  filter?: MembershipFilter;
  page: number;
};

export function useSeasonMemberships(seasonId: number, p: SeasonMembershipsParams, enabled = true) {
  return useQuery({
    queryKey: ["core", "admin", "memberships", seasonId, p],
    queryFn: () =>
      api<PageResponse<MembershipRow>>(
        `/api/admin/seasons/${seasonId}/memberships${qs({ ...p, size: 50 })}`,
      ),
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useAllSeasonMemberships(seasonId: number | undefined) {
  return useQuery({
    queryKey: ["core", "admin", "memberships-all", seasonId],
    queryFn: async () => {
      const items: MembershipRow[] = [];
      for (let page = 0; ; page++) {
        const res = await api<PageResponse<MembershipRow>>(
          `/api/admin/seasons/${seasonId}/memberships${qs({ page, size: 50 })}`,
        );
        items.push(...res.items);
        if (res.items.length === 0 || items.length >= res.total) return items;
      }
    },
    enabled: seasonId !== undefined,
  });
}

export function useUnassignedUsers(seasonId: number, q: string, enabled = true) {
  return useQuery({
    queryKey: ["core", "admin", "unassigned", seasonId, q],
    queryFn: () => api<DirUser[]>(`/api/admin/seasons/${seasonId}/unassigned-users${qs({ q })}`),
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useAddMembership(seasonId: number) {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (body: { userId: number } & MembershipInput) =>
      api<MembershipRow>(`/api/admin/seasons/${seasonId}/memberships`, { method: "POST", json: body }),
    onSuccess: () => refresh({ me: true }),
  });
}

export function useUpdateMembership(seasonId: number) {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (v: { membershipId: number; body: MembershipInput }) =>
      api<MembershipRow>(`/api/admin/seasons/${seasonId}/memberships/${v.membershipId}`, {
        method: "PUT",
        json: v.body,
      }),
    onSuccess: () => refresh({ me: true }),
  });
}

export function useRemoveMembership(seasonId: number) {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (membershipId: number) =>
      api<void>(`/api/admin/seasons/${seasonId}/memberships/${membershipId}`, { method: "DELETE" }),
    onSuccess: () => refresh({ me: true }),
  });
}

export function useBatchAssign(seasonId: number) {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (body: { userIds: number[]; branchId: number }) =>
      api<{ created: number }>(`/api/admin/seasons/${seasonId}/memberships/batch`, {
        method: "POST",
        json: body,
      }),
    onSuccess: () => refresh({ me: true }),
  });
}

export function useBulkUpdate(seasonId: number) {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (body: BulkUpdateRequest) =>
      api<{ updated: number; skippedSelf: number }>(
        `/api/admin/seasons/${seasonId}/memberships/bulk-update`,
        { method: "POST", json: body },
      ),
    onSuccess: () => refresh({ me: true }),
  });
}

export function useBulkDelete(seasonId: number) {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (membershipIds: number[]) =>
      api<{ deleted: number }>(`/api/admin/seasons/${seasonId}/memberships/bulk-delete`, {
        method: "POST",
        json: { membershipIds },
      }),
    onSuccess: () => refresh({ me: true }),
  });
}

export function useCopyMemberships(seasonId: number) {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (body: CopyMembershipsRequest) =>
      api<CopyMembershipsResult>(`/api/admin/seasons/${seasonId}/memberships/copy`, {
        method: "POST",
        json: body,
      }),
    onSuccess: () => refresh({ me: true }),
  });
}

export function useActivationPreview(seasonId: number, enabled: boolean) {
  return useQuery({
    queryKey: ["core", "admin", "activation-preview", seasonId],
    queryFn: () => api<ActivationPreview>(`/api/admin/seasons/${seasonId}/activation-preview`),
    enabled,
    gcTime: 0,
    staleTime: 0,
  });
}

export function useActivateSeason() {
  const queryClient = useQueryClient();
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (id: number) => api<SeasonRow>(`/api/admin/seasons/${id}/activate`, { method: "POST" }),
    onSuccess: async () => {
      refresh();
      await queryClient.invalidateQueries({ queryKey: ["me"] });
    },
  });
}

export function useGroups(p: { q: string; archived: boolean }) {
  return useQuery({
    queryKey: ["core", "groups", p],
    queryFn: () => api<GroupRow[]>(`/api/groups${qs({ q: p.q, archived: p.archived })}`),
    placeholderData: keepPreviousData,
  });
}

export function useGroup(id: number) {
  return useQuery({
    queryKey: ["core", "group", id],
    queryFn: () => api<GroupDetail>(`/api/groups/${id}`),
  });
}

export function useCreateGroup() {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (body: { name: string; description?: string; memberIds: number[] }) =>
      api<GroupDetail>("/api/groups", { method: "POST", json: body }),
    onSuccess: () => refresh(),
  });
}

export function useUpdateGroup(id: number) {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (body: { name: string; description?: string }) =>
      api<GroupDetail>(`/api/groups/${id}`, { method: "PUT", json: body }),
    onSuccess: () => refresh(),
  });
}

export function useAddGroupMembers(id: number) {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (userIds: number[]) =>
      api<GroupDetail>(`/api/groups/${id}/members`, { method: "POST", json: { userIds } }),
    onSuccess: () => refresh(),
  });
}

export function useRemoveGroupMember(id: number) {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (userId: number) => api<void>(`/api/groups/${id}/members/${userId}`, { method: "DELETE" }),
    onSuccess: () => refresh(),
  });
}

export function useSetGroupArchived(id: number) {
  const refresh = useRefreshCore();
  return useMutation({
    mutationFn: (archived: boolean) =>
      api<void>(`/api/groups/${id}/${archived ? "archive" : "unarchive"}`, { method: "POST" }),
    onSuccess: () => refresh(),
  });
}

export type AuditParams = { from: string; to: string; actorId?: number; action?: string; page: number };

export function useAudit(p: AuditParams) {
  return useQuery({
    queryKey: ["core", "admin", "audit", p],
    queryFn: () => api<PageResponse<AuditRow>>(`/api/admin/audit${qs({ ...p, size: 50 })}`),
    placeholderData: keepPreviousData,
  });
}

export function useAuditActions() {
  return useQuery({
    queryKey: ["core", "admin", "audit-actions"],
    queryFn: () => api<AuditActionOption[]>("/api/admin/audit/actions"),
    staleTime: 60_000,
  });
}
