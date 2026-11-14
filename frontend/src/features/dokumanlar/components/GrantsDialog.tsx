"use client";

import { useState } from "react";
import { Building2, Globe, Loader2, Users, X } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { ErrorState } from "@/components/common/ErrorState";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { SimpleSelect } from "@/components/common/SimpleSelect";
import { TargetPicker } from "@/components/common/TargetPicker";
import { UserAvatar } from "@/components/common/UserAvatar";
import { Button } from "@/components/ui/button";
import type { TargetView } from "@/features/core/types";
import { useMe } from "@/lib/me";
import { useAddGrant, useGrants, useRemoveGrant, useUpdateGrant } from "../api";
import type { DmsLevel, GrantRow, SpaceRow } from "../types";
import { LEVEL_LABEL, LEVEL_OPTIONS, LEVEL_RANK } from "../utils";

const SELF_WARNING = "Kendi yetkinizi değiştiriyorsunuz; bu alanı yönetemeyebilirsiniz.";

type Pending =
  | { kind: "remove"; grant: GrantRow }
  | { kind: "level"; grant: GrantRow; level: DmsLevel };

function SubjectIcon({ grant }: { grant: GrantRow }) {
  const s = grant.subject;
  if (s.type === "USER" && s.id !== undefined) return <UserAvatar userId={s.id} name={s.name} size="sm" />;
  const Icon = s.type === "GROUP" ? Users : s.type === "BRANCH" ? Building2 : Globe;
  return (
    <span className="flex size-8 items-center justify-center rounded-full bg-muted text-muted-foreground">
      <Icon className="size-4" aria-hidden />
    </span>
  );
}

function Body({ space }: { space: SpaceRow }) {
  const me = useMe();
  const grants = useGrants(space.id, true);
  const add = useAddGrant(space.id);
  const update = useUpdateGrant(space.id);
  const remove = useRemoveGrant(space.id);
  const [targets, setTargets] = useState<TargetView[]>([]);
  const [level, setLevel] = useState<DmsLevel>("VIEW");
  const [pending, setPending] = useState<Pending | null>(null);

  const isSelf = (g: GrantRow) => g.subject.type === "USER" && g.subject.id === me.data?.id;

  async function addAll() {
    for (const t of targets) await add.mutateAsync({ subject: { type: t.type, id: t.id }, level });
    toast.success("Yetki eklendi.");
    setTargets([]);
  }

  function changeLevel(g: GrantRow, next: DmsLevel) {
    if (next === g.level) return;
    if (isSelf(g) && LEVEL_RANK[next] < LEVEL_RANK[g.level]) setPending({ kind: "level", grant: g, level: next });
    else void applyLevel(g, next).catch(() => {});
  }

  async function applyLevel(g: GrantRow, next: DmsLevel) {
    await update.mutateAsync({ id: g.id, level: next });
    toast.success("Yetki güncellendi.");
  }

  return (
    <div className="space-y-4">
      <div className="space-y-1 rounded-lg bg-muted p-3 text-[13px] text-muted-foreground">
        <p>
          <span className="font-medium text-foreground">Görüntüleme:</span> Dosyaları görür ve indirir.
        </p>
        <p>
          <span className="font-medium text-foreground">Düzenleme:</span> Ayrıca yükler, klasör açar, adlandırır,
          taşır ve siler.
        </p>
        <p>
          <span className="font-medium text-foreground">Yönetim:</span> Ayrıca yetki verir ve alan ayarlarını
          değiştirir.
        </p>
      </div>

      <div className="space-y-2">
        <TargetPicker value={targets} onChange={setTargets} allowed={["USER", "GROUP", "BRANCH", "ALL"]} groupScope="all" />
        <div className="flex gap-2">
          <SimpleSelect
            className="flex-1"
            value={level}
            onChange={(v) => setLevel(v as DmsLevel)}
            options={LEVEL_OPTIONS}
          />
          <Button
            className="h-11 md:h-10"
            disabled={targets.length === 0 || add.isPending}
            onClick={() => void addAll().catch(() => {})}
          >
            {add.isPending && <Loader2 className="animate-spin" aria-hidden />}
            Ekle
          </Button>
        </div>
      </div>

      {grants.isPending ? (
        <div className="flex justify-center py-6">
          <Loader2 className="size-5 animate-spin text-muted-foreground" aria-label="Yükleniyor" />
        </div>
      ) : grants.isError ? (
        <ErrorState error={grants.error} onRetry={() => void grants.refetch()} />
      ) : (
        <>
          <ul className="divide-y rounded-xl border">
            <li className="flex min-h-14 items-center gap-3 px-3">
              <UserAvatar userId={space.owner.id} name={space.owner.fullName} size="sm" />
              <span className="min-w-0 flex-1 truncate text-[15px]">{space.owner.fullName} · Sahip</span>
              <span className="text-sm text-muted-foreground">{LEVEL_LABEL.MANAGE}</span>
            </li>
            {grants.data.map((g) => (
              <li key={g.id} className="flex min-h-14 items-center gap-2 px-3 py-1.5">
                <SubjectIcon grant={g} />
                <span className="min-w-0 flex-1 truncate text-[15px]">{g.subject.name}</span>
                <SimpleSelect
                  className="w-36"
                  value={g.level}
                  onChange={(v) => changeLevel(g, v as DmsLevel)}
                  options={LEVEL_OPTIONS}
                />
                <Button
                  variant="ghost"
                  size="icon"
                  className="size-11 md:size-9"
                  aria-label={`${g.subject.name} yetkisini kaldır`}
                  onClick={() => setPending({ kind: "remove", grant: g })}
                >
                  <X className="size-4" aria-hidden />
                </Button>
              </li>
            ))}
          </ul>
          {grants.data.length === 0 && (
            <p className="text-center text-[13px] text-muted-foreground">Bu alana şu an yalnızca siz erişebilirsiniz.</p>
          )}
        </>
      )}

      <ConfirmDialog
        open={pending?.kind === "remove"}
        onOpenChange={(o) => !o && setPending(null)}
        title="Yetkiyi kaldır"
        description={`'${pending?.grant.subject.name ?? ""}' için verilen yetki kaldırılacak.${
          pending && isSelf(pending.grant) ? ` ${SELF_WARNING}` : ""
        }`}
        confirmLabel="Kaldır"
        destructive
        onConfirm={async () => {
          if (pending?.kind !== "remove") return;
          await remove.mutateAsync(pending.grant.id);
          toast.success("Yetki kaldırıldı.");
        }}
      />
      <ConfirmDialog
        open={pending?.kind === "level"}
        onOpenChange={(o) => !o && setPending(null)}
        title="Yetkiyi düşür"
        description={SELF_WARNING}
        confirmLabel="Değiştir"
        destructive
        onConfirm={async () => {
          if (pending?.kind !== "level") return;
          await applyLevel(pending.grant, pending.level);
        }}
      />
    </div>
  );
}

type Props = { space: SpaceRow; open: boolean; onOpenChange: (o: boolean) => void };

export function GrantsDialog({ space, open, onOpenChange }: Props) {
  return (
    <ResponsiveDialog
      open={open}
      onOpenChange={onOpenChange}
      title={`Yetkiler — ${space.name}`}
      className="sm:max-w-[560px]"
    >
      <Body space={space} />
    </ResponsiveDialog>
  );
}
