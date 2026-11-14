"use client";

import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type {
  BadgeMap,
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
    queryKey: ["core", "badges"],
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
    signOut: () =>
      logout.mutate(undefined, {
        onSuccess: () => {
          queryClient.clear();
          router.replace("/giris");
        },
      }),
  };
}
