"use client";

import { useRef, useState } from "react";
import { Plus } from "lucide-react";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { ErrorState } from "@/components/common/ErrorState";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useCalendars, useDeleteCalendar } from "@/features/calendar/api";
import { CalendarFormDialog } from "@/features/calendar/components/CalendarFormDialog";
import { CalendarList } from "@/features/calendar/components/CalendarList";
import { CalendarView, type CalendarViewHandle, type ViewKey } from "@/features/calendar/components/CalendarView";
import { EntryDetailDialog } from "@/features/calendar/components/EntryDetailDialog";
import { EntryFormDialog, type EntryDefaults } from "@/features/calendar/components/EntryFormDialog";
import { ShareDialog } from "@/features/calendar/components/ShareDialog";
import type { CalendarDto, EntryDto } from "@/features/calendar/types";
import { useCalendarVisibility } from "@/features/calendar/visibility";
import { useMe } from "@/lib/me";
import { useMediaQuery } from "@/lib/media";
import { useUrlParams } from "@/lib/url-state";
import { toast } from "sonner";

type Manage =
  | { kind: "create" }
  | { kind: "rename"; calendar: CalendarDto }
  | { kind: "resource"; calendar?: CalendarDto }
  | { kind: "share"; calendar: CalendarDto }
  | { kind: "delete"; calendar: CalendarDto };

const VIEWS: ViewKey[] = ["ay", "hafta", "gun", "liste"];
const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export default function CalendarPage() {
  const desktop = useMediaQuery("(min-width: 1024px)");
  const url = useUrlParams();
  const me = useMe();
  const calendars = useCalendars();
  const del = useDeleteCalendar();
  const view = useRef<CalendarViewHandle>(null);
  const vis = useCalendarVisibility(calendars.data, me.data?.membership?.branchName);

  const [detail, setDetail] = useState<EntryDto | null>(null);
  const [form, setForm] = useState<{ entry?: EntryDto; defaults?: EntryDefaults } | null>(null);
  const [manage, setManage] = useState<Manage | null>(null);
  const [drawer, setDrawer] = useState(false);

  if (calendars.isPending || me.isPending) {
    return (
      <div className="grid gap-4 lg:grid-cols-[260px_1fr]">
        <Skeleton className="hidden h-96 lg:block" />
        <Skeleton className="h-[560px]" />
      </div>
    );
  }
  if (calendars.isError) return <ErrorState error={calendars.error} onRetry={() => void calendars.refetch()} />;

  const list = calendars.data;
  const isManager = me.data?.permissions.canEditInstitutionCalendar ?? false;
  const canCreateAny = list.some((c) => c.canCreate);
  const shown = list.filter(vis.isVisible);
  const soleCalendarId = shown.length === 1 && shown[0].canCreate ? shown[0].id : undefined;
  const requested = url.get("gorunum") as ViewKey;
  const picked: ViewKey = VIEWS.includes(requested) ? requested : desktop ? "ay" : "liste";
  const currentView: ViewKey = !desktop && picked === "hafta" ? "liste" : picked;
  const tarih = url.get("tarih");
  const refetch = () => view.current?.refetch();

  function onViewChange(v: ViewKey, date: string) {
    if (v === url.get("gorunum") && date === url.get("tarih")) return;
    url.set({ gorunum: v, tarih: date });
  }

  function showOnly(c: CalendarDto) {
    vis.showOnly(c.id);
    url.set({ gorunum: desktop ? "hafta" : "gun" });
    setDrawer(false);
  }

  const listPanel = (
    <CalendarList
      calendars={list}
      isVisible={vis.isVisible}
      onToggle={vis.toggle}
      isManager={isManager}
      onShare={(c) => setManage({ kind: "share", calendar: c })}
      onRename={(c) => setManage({ kind: "rename", calendar: c })}
      onEditResource={(c) => setManage({ kind: "resource", calendar: c })}
      onDelete={(c) => setManage({ kind: "delete", calendar: c })}
      onShowOnly={showOnly}
      onCreateCalendar={() => setManage({ kind: "create" })}
      onCreateResource={() => setManage({ kind: "resource" })}
    />
  );

  const newEntry = () => {
    const start = new Date();
    start.setMinutes(0, 0, 0);
    start.setHours(start.getHours() + 1);
    setForm({ defaults: { calendarId: soleCalendarId, start, end: new Date(start.getTime() + 3600_000) } });
  };

  const entryCalendar = (e: EntryDto) => list.find((c) => c.id === e.calendarId);
  const removing = manage?.kind === "delete" ? manage.calendar : null;

  return (
    <div className="grid gap-4 lg:grid-cols-[260px_1fr]">
      <h1 className="sr-only">Takvim</h1>
      {desktop && (
        <aside className="space-y-4">
          <Button className="h-10 w-full" disabled={!canCreateAny} onClick={newEntry}>
            <Plus aria-hidden />
            Yeni Kayıt
          </Button>
          {listPanel}
        </aside>
      )}
      {vis.loaded ? (
        <CalendarView
          ref={view}
          desktop={desktop}
          view={currentView}
          initialDate={DATE_RE.test(tarih) ? tarih : undefined}
          calendars={list}
          visibleIds={vis.visibleIds}
          canCreateAny={canCreateAny}
          onViewChange={onViewChange}
          onOpenEntry={setDetail}
          onCreateAt={(defaults) => setForm({ defaults: { calendarId: soleCalendarId, ...defaults } })}
          onOpenCalendars={() => setDrawer(true)}
        />
      ) : (
        <Skeleton className="h-[560px]" />
      )}

      {!desktop && canCreateAny && (
        <Button
          aria-label="Yeni Kayıt"
          className="fixed right-4 bottom-[calc(64px+env(safe-area-inset-bottom)+16px)] z-30 size-14 rounded-full shadow-lg"
          onClick={newEntry}
        >
          <Plus className="size-6" aria-hidden />
        </Button>
      )}

      {!desktop && (
        <ResponsiveDialog open={drawer} onOpenChange={setDrawer} title="Takvimler" variant="sheet">
          {listPanel}
        </ResponsiveDialog>
      )}

      {detail && (
        <EntryDetailDialog
          entry={detail}
          calendar={entryCalendar(detail)}
          onClose={() => setDetail(null)}
          onEdit={() => {
            setForm({ entry: detail });
            setDetail(null);
          }}
          onDeleted={refetch}
        />
      )}
      {form && (
        <EntryFormDialog
          calendars={list}
          entry={form.entry}
          defaults={form.defaults}
          onClose={() => setForm(null)}
          onSaved={refetch}
        />
      )}

      {(manage?.kind === "create" || manage?.kind === "rename") && (
        <CalendarFormDialog
          open
          onOpenChange={(o) => !o && setManage(null)}
          kind="personal"
          calendar={manage.kind === "rename" ? manage.calendar : undefined}
        />
      )}
      {manage?.kind === "resource" && (
        <CalendarFormDialog open onOpenChange={(o) => !o && setManage(null)} kind="resource" calendar={manage.calendar} />
      )}
      {manage?.kind === "share" && (
        <ShareDialog calendar={manage.calendar} open onOpenChange={(o) => !o && setManage(null)} />
      )}
      {removing && (
        <ConfirmDialog
          open
          onOpenChange={(o) => !o && setManage(null)}
          title={removing.type === "RESOURCE" ? "Salonu sil" : "Takvimi sil"}
          description={
            removing.type === "RESOURCE"
              ? `'${removing.name}' silinecek. Geçmiş rezervasyonlar da silinir.`
              : `'${removing.name}' takvimi ve içindeki tüm kayıtlar silinecek. Bu işlem geri alınamaz.`
          }
          confirmLabel="Sil"
          destructive
          onConfirm={async () => {
            await del.mutateAsync(removing.id);
            toast.success(removing.type === "RESOURCE" ? "Salon silindi." : "Takvim silindi.");
            refetch();
          }}
        />
      )}
    </div>
  );
}
