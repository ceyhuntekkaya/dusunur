"use client";

import { forwardRef, useEffect, useImperativeHandle, useRef, useState } from "react";
import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import timeGridPlugin from "@fullcalendar/timegrid";
import listPlugin from "@fullcalendar/list";
import interactionPlugin from "@fullcalendar/interaction";
import trLocale from "@fullcalendar/core/locales/tr";
import type { DatesSetArg, EventClickArg } from "@fullcalendar/core";
import { addHours, setHours, startOfDay } from "date-fns";
import { ChevronLeft, ChevronRight, ListIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { SegmentedControl } from "@/components/common/SegmentedControl";
import { ApiError } from "@/lib/api";
import { toastError } from "@/lib/query-client";
import { fetchEntries } from "../api";
import { fcEndToApiEndDate, toDateInput, toFcEvent } from "../convert";
import type { CalendarDto, EntryDto } from "../types";
import type { EntryDefaults } from "./EntryFormDialog";

export type ViewKey = "ay" | "hafta" | "gun" | "liste";
export const FC_VIEW: Record<ViewKey, string> = {
  ay: "dayGridMonth",
  hafta: "timeGridWeek",
  gun: "timeGridDay",
  liste: "listWeek",
};
const REVERSE: Record<string, ViewKey> = Object.fromEntries(
  Object.entries(FC_VIEW).map(([k, v]) => [v, k as ViewKey]),
);

export type CalendarViewHandle = { refetch: () => void };

type Props = {
  desktop: boolean;
  view: ViewKey;
  initialDate?: string;
  calendars: CalendarDto[];
  visibleIds: number[];
  canCreateAny: boolean;
  onViewChange: (view: ViewKey, date: string) => void;
  onOpenEntry: (entry: EntryDto) => void;
  onCreateAt: (defaults: EntryDefaults) => void;
  onOpenCalendars: () => void;
};

export const CalendarView = forwardRef<CalendarViewHandle, Props>(function CalendarView(
  { desktop, view, initialDate, calendars, visibleIds, canCreateAny, onViewChange, onOpenEntry, onCreateAt, onOpenCalendars },
  ref,
) {
  const fc = useRef<FullCalendar>(null);
  const [title, setTitle] = useState("");
  const [loading, setLoading] = useState(false);
  const colors = useRef(new Map<number, string>());
  const idsRef = useRef(visibleIds);
  const idsKey = visibleIds.join(",");

  useEffect(() => {
    colors.current = new Map(calendars.map((c) => [c.id, c.color]));
  }, [calendars]);

  useEffect(() => {
    idsRef.current = visibleIds;
    fc.current?.getApi().refetchEvents();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [idsKey]);

  useEffect(() => {
    const api = fc.current?.getApi();
    if (api && api.view.type !== FC_VIEW[view]) api.changeView(FC_VIEW[view]);
  }, [view]);

  useImperativeHandle(ref, () => ({ refetch: () => fc.current?.getApi().refetchEvents() }));

  function onDatesSet(info: DatesSetArg) {
    setTitle(info.view.title);
    const key = REVERSE[info.view.type];
    if (key) onViewChange(key, toDateInput(fc.current!.getApi().getDate()));
  }

  function defaultsForDay(date: Date): EntryDefaults {
    const start = setHours(startOfDay(date), 9);
    return { start, end: addHours(start, 1) };
  }

  const toolbarBtn = "h-11 md:h-10";
  const api = () => fc.current!.getApi();

  return (
    <div className="min-w-0">
      {!desktop && (
        <div className="mb-3 space-y-3">
          <SegmentedControl
            ariaLabel="Görünüm"
            value={view === "hafta" ? "liste" : (view as "liste" | "gun" | "ay")}
            onChange={(v) => onViewChange(v, toDateInput(api().getDate()))}
            options={[
              { value: "liste", label: "Liste" },
              { value: "gun", label: "Gün" },
              { value: "ay", label: "Ay" },
            ]}
          />
          <div className="flex items-center gap-1">
            <Button variant="outline" size="icon" className="size-11" aria-label="Önceki" onClick={() => api().prev()}>
              <ChevronLeft aria-hidden />
            </Button>
            <Button variant="outline" className={toolbarBtn} onClick={() => api().today()}>
              Bugün
            </Button>
            <Button variant="outline" size="icon" className="size-11" aria-label="Sonraki" onClick={() => api().next()}>
              <ChevronRight aria-hidden />
            </Button>
            <h2 className="min-w-0 flex-1 truncate px-2 text-[15px] font-semibold first-letter:capitalize">{title}</h2>
            <Button variant="outline" className={toolbarBtn} onClick={onOpenCalendars}>
              <ListIcon aria-hidden />
              Takvimler
            </Button>
          </div>
        </div>
      )}
      <div
        className={
          "rounded-xl border bg-card p-2 transition-opacity md:p-3 " +
          "[--fc-border-color:var(--border)] [--fc-page-bg-color:var(--card)] [--fc-neutral-bg-color:var(--muted)] " +
          "[--fc-list-event-hover-bg-color:var(--muted)] [--fc-today-bg-color:color-mix(in_oklab,var(--primary)_8%,transparent)] " +
          "[&_.fc-button]:min-h-10 [&_.fc-toolbar-title]:text-lg [&_.fc-toolbar-title]:font-semibold [&_.fc-event]:rounded-md [&_.fc-event]:cursor-pointer " +
          (loading ? "opacity-60" : "")
        }
        aria-busy={loading}
      >
        <FullCalendar
          ref={fc}
          plugins={[dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin]}
          locale={trLocale}
          firstDay={1}
          timeZone="local"
          initialView={FC_VIEW[view]}
          initialDate={initialDate}
          headerToolbar={
            desktop
              ? { left: "prev,next today", center: "title", right: "dayGridMonth,timeGridWeek,timeGridDay,listWeek" }
              : false
          }
          buttonText={{ today: "Bugün", month: "Ay", week: "Hafta", day: "Gün", list: "Liste" }}
          allDayText="Tüm gün"
          noEventsText="Bu aralıkta kayıt yok."
          slotMinTime="07:00"
          slotMaxTime="22:00"
          slotDuration="00:30"
          nowIndicator
          dayMaxEvents={3}
          eventTimeFormat={{ hour: "2-digit", minute: "2-digit", hour12: false }}
          height="auto"
          editable={false}
          selectable={desktop}
          selectMirror
          unselectAuto
          datesSet={onDatesSet}
          loading={setLoading}
          events={(info, ok, fail) => {
            const ids = idsRef.current;
            if (ids.length === 0) {
              ok([]);
              return;
            }
            fetchEntries(info.startStr, info.endStr, ids)
              .then((entries) => ok(entries.map((e) => toFcEvent(e, colors.current.get(e.calendarId) ?? "#2453C7"))))
              .catch((err: unknown) => {
                toastError(err instanceof ApiError ? err.message : "Kayıtlar yüklenemedi. Lütfen tekrar deneyin.");
                fail(err instanceof Error ? err : new Error("events"));
              });
          }}
          eventClick={(info: EventClickArg) => {
            info.jsEvent.preventDefault();
            onOpenEntry(info.event.extendedProps.entry as EntryDto);
          }}
          dateClick={(info) => {
            if (!canCreateAny) return;
            if (info.view.type === "listWeek") return;
            onCreateAt(info.allDay ? defaultsForDay(info.date) : { start: info.date, end: addHours(info.date, 1) });
          }}
          select={(info) => {
            const a = api();
            a.unselect();
            if (!canCreateAny || !info.view.type.startsWith("timeGrid")) return;
            if (info.allDay) {
              onCreateAt({
                allDay: true,
                start: info.start,
                end: new Date(`${fcEndToApiEndDate(info.endStr.slice(0, 10))}T00:00:00`),
              });
              return;
            }
            onCreateAt({ start: info.start, end: info.end });
          }}
        />
      </div>
    </div>
  );
});
