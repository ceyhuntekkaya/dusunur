"use client";

import { useInfiniteQuery, useMutation, useQuery, useQueryClient, type QueryClient, type QueryKey } from "@tanstack/react-query";
import { toast } from "sonner";
import { api, type PageResponse } from "@/lib/api";
import { useWsEvent, useWsReconnect } from "@/lib/ws";
import type {
  CreateDelegationRequest,
  CreatePrintRequest,
  DelegationDto,
  MonthlyReport,
  PrintAction,
  PrintChangedEvent,
  PrintContext,
  PrintRequestDetail,
  PrintRequestRow,
  PrintSeason,
  QueueBoard,
} from "./types";

function refresh(qc: QueryClient) {
  void qc.invalidateQueries({ queryKey: ["print"] });
  void qc.invalidateQueries({ queryKey: ["me", "badges"] });
}

export function usePrintLiveRefresh() {
  const qc = useQueryClient();
  useWsEvent<PrintChangedEvent>("PRINT_REQUEST_CHANGED", () => void qc.invalidateQueries({ queryKey: ["print"] }));
  useWsReconnect(() => void qc.invalidateQueries({ queryKey: ["print"] }));
}

export function usePrintContext() {
  return useQuery({
    queryKey: ["print", "context"],
    queryFn: () => api<PrintContext>("/api/print/context"),
  });
}

export function useMyActiveRequests() {
  return useQuery({
    queryKey: ["print", "summary", "active"],
    queryFn: async () => {
      const [open, ready] = await Promise.all([
        api<PageResponse<PrintRequestRow>>("/api/print/requests/mine?group=open&size=5"),
        api<PageResponse<PrintRequestRow>>("/api/print/requests/mine?group=ready&size=5"),
      ]);
      return [...open.items, ...ready.items].sort((a, b) => a.dueAt.localeCompare(b.dueAt)).slice(0, 5);
    },
  });
}

export function usePendingApprovals() {
  return useQuery({
    queryKey: ["print", "summary", "approvals"],
    queryFn: () => api<PageResponse<PrintRequestRow>>("/api/print/approvals?tab=pending&size=5"),
  });
}

export function usePrintSeasons() {
  return useQuery({
    queryKey: ["print", "seasons"],
    queryFn: () => api<PrintSeason[]>("/api/print/seasons"),
    staleTime: 5 * 60_000,
  });
}

export function usePrintRequest(id: number) {
  return useQuery({
    queryKey: ["print", "request", id],
    queryFn: () => api<PrintRequestDetail>(`/api/print/requests/${id}`),
    retry: false,
    enabled: id > 0,
    meta: { silentError: true },
  });
}

export function usePagedRequests<T>(
  queryKey: QueryKey,
  path: string,
  params: Record<string, string | undefined>,
  size = 20,
) {
  return useInfiniteQuery({
    queryKey,
    initialPageParam: 0,
    queryFn: ({ pageParam }) => {
      const qs = new URLSearchParams();
      for (const [k, v] of Object.entries(params)) {
        if (v !== undefined && v !== "") qs.set(k, v);
      }
      qs.set("page", String(pageParam));
      qs.set("size", String(size));
      return api<PageResponse<T>>(`${path}?${qs.toString()}`);
    },
    getNextPageParam: (last) => ((last.page + 1) * last.size < last.total ? last.page + 1 : undefined),
  });
}

export function useQueueBoard(enabled: boolean) {
  return useQuery({
    queryKey: ["print", "board"],
    queryFn: () => api<QueueBoard>("/api/print/queue/board"),
    enabled,
  });
}

export function useMonthlyReport(month: string, branchId: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: ["print", "report", month, branchId ?? ""],
    queryFn: () => {
      const qs = new URLSearchParams({ month });
      if (branchId) qs.set("branchId", branchId);
      return api<MonthlyReport>(`/api/print/reports/monthly?${qs.toString()}`);
    },
    enabled,
  });
}

export function useDelegations() {
  return useQuery({
    queryKey: ["print", "delegations"],
    queryFn: () => api<DelegationDto[]>("/api/print/delegations"),
  });
}

export function useCreateDelegation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateDelegationRequest) =>
      api<DelegationDto>("/api/print/delegations", { method: "POST", json: body }),
    onSuccess: () => void qc.invalidateQueries({ queryKey: ["print", "delegations"] }),
  });
}

export function useDeleteDelegation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/print/delegations/${id}`, { method: "DELETE" }),
    onSuccess: () => void qc.invalidateQueries({ queryKey: ["print", "delegations"] }),
  });
}

export function useCreatePrintRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreatePrintRequest) =>
      api<PrintRequestDetail>("/api/print/requests", { method: "POST", json: body }),
    onSuccess: () => refresh(qc),
  });
}

export function usePageCount() {
  return useMutation({
    mutationFn: (fileId: string) => api<{ pageCount: number | null }>(`/api/print/files/${fileId}/page-count`),
  });
}

const SUCCESS: Record<PrintAction, string> = {
  approve: "Talep onaylandı.",
  reject: "Talep reddedildi.",
  cancel: "Talep iptal edildi.",
  start: "Talep hazırlanıyor olarak işaretlendi.",
  ready: "Talep hazır olarak işaretlendi; talep sahibine bildirim gönderildi.",
  deliver: "Teslim edildi.",
};

export function usePrintTransition() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: number; action: PrintAction; reason?: string }) =>
      api<PrintRequestDetail>(`/api/print/requests/${v.id}/${v.action}`, {
        method: "POST",
        json: v.action === "reject" ? { reason: v.reason } : undefined,
      }),
    onSuccess: (detail, v) => {
      qc.setQueryData(["print", "request", v.id], detail);
      toast.success(SUCCESS[v.action]);
      refresh(qc);
    },
    onError: () => refresh(qc),
  });
}

export function printFileUrl(id: number, download: boolean): string {
  return `/api/print/requests/${id}/file?download=${download}`;
}
