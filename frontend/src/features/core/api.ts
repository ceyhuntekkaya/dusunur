"use client";

import { useRouter } from "next/navigation";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { disablePushQuietly } from "@/lib/push";
import type {
  BadgeMap,
  ChangePasswordRequest,
  DirectorySearchResponse,
  KvkkResponse,
  LoginRequest,
  MeResponse,
  PasswordTokenInfo,
} from "@/features/core/types";

export function useLogin() {
  return useMutation({
    mutationFn: (body: LoginRequest) =>
      api<MeResponse>("/api/auth/login", { method: "POST", json: body }),
    meta: { silentError: true },
  });
}

export function useLogout() {
  return useMutation({
    mutationFn: () => api<void>("/api/auth/logout", { method: "POST" }),
  });
}

export function useForgotPassword() {
  return useMutation({
    mutationFn: (body: { email: string }) =>
      api<{ message: string }>("/api/auth/forgot-password", { method: "POST", json: body }),
  });
}

export function usePasswordToken(token: string | null) {
  return useQuery({
    queryKey: ["core", "password-token", token],
    queryFn: () => api<PasswordTokenInfo>(`/api/auth/password-token/${encodeURIComponent(token ?? "")}`),
    enabled: !!token,
    retry: false,
    staleTime: Infinity,
    meta: { silentError: true },
  });
}

export function useSetPassword() {
  return useMutation({
    mutationFn: (body: { token: string; newPassword: string }) =>
      api<void>("/api/auth/password-token", { method: "POST", json: body }),
  });
}

export function useKvkk(enabled: boolean) {
  return useQuery({
    queryKey: ["core", "kvkk"],
    queryFn: () => api<KvkkResponse>("/api/auth/kvkk"),
    enabled,
    staleTime: Infinity,
  });
}

export function useAcceptKvkk() {
  return useMutation({
    mutationFn: () => api<void>("/api/auth/kvkk/accept", { method: "POST" }),
  });
}

export function useBadges(enabled: boolean) {
  return useQuery({
    queryKey: ["me", "badges"],
    queryFn: () => api<BadgeMap>("/api/me/badges"),
    enabled,
    refetchInterval: 60_000,
    meta: { silentError: true },
  });
}

export function useSignOut() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const logout = useLogout();
  return {
    pending: logout.isPending,
    signOut: async () => {
      await disablePushQuietly();
      logout.mutate(undefined, {
        onSuccess: () => {
          queryClient.clear();
          router.replace("/giris");
        },
      });
    },
  };
}

export function useChangePassword() {
  return useMutation({
    mutationFn: (body: ChangePasswordRequest) =>
      api<void>("/api/auth/change-password", { method: "POST", json: body }),
  });
}

function useRefreshNotifications() {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: ["notifications"] });
    void queryClient.invalidateQueries({ queryKey: ["me", "badges"] });
  };
}

export function useMarkNotificationRead() {
  const refresh = useRefreshNotifications();
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/notifications/${id}/read`, { method: "POST" }),
    onSuccess: refresh,
    meta: { silentError: true },
  });
}

export function useMarkAllNotificationsRead() {
  const refresh = useRefreshNotifications();
  return useMutation({
    mutationFn: () => api<void>("/api/notifications/read-all", { method: "POST" }),
    onSuccess: refresh,
  });
}

export function useDirectorySearch(params: {
  q: string;
  kinds: ("user" | "group" | "branch")[];
  groupScope?: "mine" | "all";
  enabled?: boolean;
}) {
  const { q, kinds, groupScope = "mine", enabled = true } = params;
  return useQuery({
    queryKey: ["core", "directory", { q, kinds, groupScope }],
    queryFn: () => {
      const qs = new URLSearchParams({ q, kinds: kinds.join(","), groupScope, limit: "20" });
      return api<DirectorySearchResponse>(`/api/directory/search?${qs.toString()}`);
    },
    enabled: enabled && kinds.length > 0,
    placeholderData: keepPreviousData,
    staleTime: 10_000,
  });
}
