"use client";

import Link from "next/link";
import { MoreHorizontal, Printer } from "lucide-react";
import { ArchiveBanner } from "@/components/common/ArchiveBanner";
import { EmptyState } from "@/components/common/EmptyState";
import { SegmentedControl } from "@/components/common/SegmentedControl";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useMe } from "@/lib/me";
import { useUrlParams } from "@/lib/url-state";
import { usePrintSeasons } from "../api";
import type { MineGroup, PrintRequestRow } from "../types";
import { PagedRequestList } from "./PagedRequestList";
import { PrintActionButtons, type ActionButtonKind } from "./PrintActionButtons";
import { PrintRequestCard } from "./PrintRequestCard";

type GroupValue = MineGroup | "all";

const GROUP_OPTIONS: { value: GroupValue; label: string }[] = [
  { value: "all", label: "Tümü" },
  { value: "open", label: "Süren" },
  { value: "ready", label: "Hazır" },
  { value: "delivered", label: "Teslim Edildi" },
  { value: "closed", label: "Reddedilen / İptal" },
];

function mineActions(r: PrintRequestRow): ActionButtonKind[] {
  const kinds: ActionButtonKind[] = [];
  if (!r.archived && (r.status === "PENDING_APPROVAL" || r.status === "APPROVED")) kinds.push("cancel");
  if (r.status === "REJECTED" || r.status === "CANCELLED" || r.status === "DELIVERED") kinds.push("copy");
  return kinds;
}

export function MyRequestsTab() {
  const me = useMe().data;
  const url = useUrlParams();
  const seasons = usePrintSeasons();

  const activeSeasonId = me?.season?.id;
  const yil = url.get("yil");
  const seasonId = yil && Number(yil) !== activeSeasonId ? Number(yil) : undefined;
  const archived = seasonId !== undefined;
  const rawGroup = url.get("grup");
  const group: GroupValue = GROUP_OPTIONS.some((g) => g.value === rawGroup) ? (rawGroup as GroupValue) : "all";
  const seasonName = seasons.data?.find((s) => s.id === seasonId)?.name ?? "";

  return (
    <div className="mx-auto max-w-[720px]">
      <div className="mb-4 flex items-center gap-1">
        <SegmentedControl
          variant="chips"
          ariaLabel="Talep grubu"
          className="min-w-0 flex-1"
          value={group}
          onChange={(v) => url.set({ grup: v === "all" ? undefined : v })}
          options={GROUP_OPTIONS}
        />
        <DropdownMenu>
          <DropdownMenuTrigger
            aria-label="Diğer seçenekler"
            className="flex size-11 shrink-0 items-center justify-center rounded-lg outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 md:size-10"
          >
            <MoreHorizontal className="size-5" aria-hidden />
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="min-w-48">
            <DropdownMenuSub>
              <DropdownMenuSubTrigger className="min-h-11">Eğitim-öğretim yılı</DropdownMenuSubTrigger>
              <DropdownMenuSubContent>
                <DropdownMenuRadioGroup
                  value={String(seasonId ?? activeSeasonId ?? "")}
                  onValueChange={(v) => url.set({ yil: Number(v) === activeSeasonId ? undefined : v })}
                >
                  {(seasons.data ?? []).map((s) => (
                    <DropdownMenuRadioItem key={s.id} value={String(s.id)} className="min-h-11">
                      {s.name}
                    </DropdownMenuRadioItem>
                  ))}
                </DropdownMenuRadioGroup>
              </DropdownMenuSubContent>
            </DropdownMenuSub>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {archived && (
        <div className="mb-1">
          <ArchiveBanner seasonName={seasonName} />
          <Button
            variant="link"
            className="-mt-2 mb-3 h-auto p-0"
            nativeButton={false}
            render={<Link href="/fotokopi?sekme=taleplerim" />}
          >
            Bu yıla dön
          </Button>
        </div>
      )}

      <PagedRequestList
        queryKey={["print", "mine", { seasonId, group }]}
        path="/api/print/requests/mine"
        params={{ seasonId: seasonId ? String(seasonId) : undefined, group: group === "all" ? undefined : group }}
        renderItem={(r) => {
          const kinds = mineActions(r);
          return (
            <PrintRequestCard
              request={r}
              context="mine"
              actions={
                kinds.length > 0 ? (
                  <PrintActionButtons requestId={r.id} ownerName={r.requester.fullName} kinds={kinds} compact />
                ) : undefined
              }
            />
          );
        }}
        empty={
          archived || group !== "all" ? (
            <EmptyState icon={Printer} message="Bu görünümde fotokopi talebi yok." />
          ) : (
            <EmptyState
              icon={Printer}
              message="Henüz fotokopi talebiniz yok."
              action={{ label: "Yeni Talep", href: "/fotokopi/yeni" }}
            />
          )
        }
      />
    </div>
  );
}
