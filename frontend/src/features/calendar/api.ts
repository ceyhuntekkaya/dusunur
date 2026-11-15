"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type {
  CalendarDto,
  CalendarInput,
  CalShareRow,
  EntryDto,
  EntryInput,
  ShareInput,
} from "./types";

export function useCalendars() {
  return useQuery({
    queryKey: ["calendar", "calendars"],
    queryFn: () => api<CalendarDto[]>("/api/calendar/calendars"),
  });
}

export function fetchEntries(from: string, to: string, calendarIds: number[]) {
  const params = new URLSearchParams({ from, to, calendarIds: calendarIds.join(",") });
  return api<EntryDto[]>(`/api/calendar/entries?${params.toString()}`);
}

export function useUpcomingEntries(days = 7) {
  const hour = new Date();
  hour.setMinutes(0, 0, 0);
  const from = hour.toISOString();
  const to = new Date(hour.getTime() + days * 86_400_000).toISOString();
  return useQuery({
    queryKey: ["calendar", "upcoming", from, to],
    queryFn: () => api<EntryDto[]>(`/api/calendar/entries?${new URLSearchParams({ from, to }).toString()}`),
  });
}

function useRefreshCalendars() {
  const qc = useQueryClient();
  return () => void qc.invalidateQueries({ queryKey: ["calendar", "calendars"] });
}

export function useCreateCalendar() {
  const refresh = useRefreshCalendars();
  return useMutation({
    mutationFn: (v: CalendarInput) => api<CalendarDto>("/api/calendar/calendars", { method: "POST", json: v }),
    onSuccess: refresh,
  });
}

export function useCreateResource() {
  const refresh = useRefreshCalendars();
  return useMutation({
    mutationFn: (v: CalendarInput) => api<CalendarDto>("/api/calendar/resources", { method: "POST", json: v }),
    onSuccess: refresh,
  });
}

export function useUpdateCalendar() {
  const refresh = useRefreshCalendars();
  return useMutation({
    mutationFn: ({ id, ...v }: CalendarInput & { id: number }) =>
      api<CalendarDto>(`/api/calendar/calendars/${id}`, { method: "PUT", json: v }),
    onSuccess: refresh,
  });
}

export function useDeleteCalendar() {
  const refresh = useRefreshCalendars();
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/calendar/calendars/${id}`, { method: "DELETE" }),
    onSuccess: refresh,
  });
}

export function useShares(calendarId: number) {
  return useQuery({
    queryKey: ["calendar", "shares", calendarId],
    queryFn: () => api<CalShareRow[]>(`/api/calendar/calendars/${calendarId}/shares`),
  });
}

function useRefreshShares(calendarId: number) {
  const qc = useQueryClient();
  return () => void qc.invalidateQueries({ queryKey: ["calendar", "shares", calendarId] });
}

export function useAddShare(calendarId: number) {
  const refresh = useRefreshShares(calendarId);
  return useMutation({
    mutationFn: (v: ShareInput) =>
      api<CalShareRow>(`/api/calendar/calendars/${calendarId}/shares`, { method: "POST", json: v }),
    onSettled: refresh,
  });
}

export function useUpdateShare(calendarId: number) {
  const refresh = useRefreshShares(calendarId);
  return useMutation({
    mutationFn: (v: { sid: number; level: CalShareRow["level"] }) =>
      api<CalShareRow>(`/api/calendar/calendars/${calendarId}/shares/${v.sid}`, {
        method: "PUT",
        json: { level: v.level },
      }),
    onSettled: refresh,
  });
}

export function useDeleteShare(calendarId: number) {
  const refresh = useRefreshShares(calendarId);
  return useMutation({
    mutationFn: (sid: number) =>
      api<void>(`/api/calendar/calendars/${calendarId}/shares/${sid}`, { method: "DELETE" }),
    onSettled: refresh,
  });
}

export function useSaveEntry() {
  const qc = useQueryClient();
  return useMutation({
    onSuccess: () => void qc.invalidateQueries({ queryKey: ["calendar"] }),
    mutationFn: (v: { id?: number; input: EntryInput }) =>
      v.id === undefined
        ? api<EntryDto>("/api/calendar/entries", { method: "POST", json: v.input })
        : api<EntryDto>(`/api/calendar/entries/${v.id}`, { method: "PUT", json: v.input }),
  });
}

export function useDeleteEntry() {
  const qc = useQueryClient();
  return useMutation({
    onSuccess: () => void qc.invalidateQueries({ queryKey: ["calendar"] }),
    mutationFn: (id: number) => api<void>(`/api/calendar/entries/${id}`, { method: "DELETE" }),
  });
}

export const entryFileUrl = (id: number, download = false) =>
  `/api/calendar/entries/${id}/file${download ? "?download=true" : ""}`;
