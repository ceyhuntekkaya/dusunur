"use client";

import { useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowLeft, Archive, UserPlus, Users, X } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { FormField } from "@/components/common/FormField";
import { PageSkeleton } from "@/components/common/Skeletons";
import { StatusBadge } from "@/components/common/StatusBadge";
import { UserAvatar } from "@/components/common/UserAvatar";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  useGroup,
  useRemoveGroupMember,
  useSetGroupArchived,
  useUpdateGroup,
} from "@/features/core/admin-api";
import { GroupAddMembersDialog } from "@/features/core/components/GroupAddMembersDialog";
import { groupFormSchema } from "@/features/core/schemas";
import type { GroupDetail, GroupMember } from "@/features/core/types";
import { useApiForm } from "@/lib/form";

function GroupInfoForm({ group }: { group: GroupDetail }) {
  const update = useUpdateGroup(group.id);
  const form = useApiForm(groupFormSchema, {
    defaultValues: { name: group.name, description: group.description ?? "" },
  });
  const { register, formState } = form;

  const onSubmit = form.submit(async (v) => {
    await update.mutateAsync({ name: v.name, description: v.description || undefined });
    toast.success("Grup bilgileri güncellendi.");
    form.reset(v);
  });

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <FormField label="Ad" htmlFor="g-name" required error={formState.errors.name?.message}>
        <Input id="g-name" className="h-11 md:h-10" aria-invalid={!!formState.errors.name} {...register("name")} />
      </FormField>
      <FormField label="Açıklama" htmlFor="g-desc" error={formState.errors.description?.message}>
        <Textarea
          id="g-desc"
          rows={3}
          aria-invalid={!!formState.errors.description}
          {...register("description")}
        />
      </FormField>
      <Button
        type="submit"
        className="h-11 w-full md:h-10 md:w-auto"
        disabled={formState.isSubmitting || !formState.isDirty}
      >
        Kaydet
      </Button>
    </form>
  );
}

export default function GroupDetailPage() {
  const id = Number(useParams<{ id: string }>().id);
  const group = useGroup(id);
  const removeMember = useRemoveGroupMember(id);
  const setArchived = useSetGroupArchived(id);

  const [adding, setAdding] = useState(false);
  const [removing, setRemoving] = useState<GroupMember | null>(null);
  const [archiveOpen, setArchiveOpen] = useState(false);

  if (group.isPending) return <PageSkeleton />;
  if (group.isError) return <ErrorState error={group.error} onRetry={() => void group.refetch()} />;

  const g = group.data;

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <Link
        href="/yonetim/gruplar"
        className="inline-flex min-h-11 items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" aria-hidden />
        Gruplar
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <h2 className="text-xl font-semibold md:text-[22px] md:leading-7">{g.name}</h2>
          {g.archived && <StatusBadge>Arşivlenmiş</StatusBadge>}
        </div>
        {g.archived ? (
          <Button
            variant="outline"
            className="h-11 md:h-10"
            disabled={setArchived.isPending}
            onClick={() =>
              setArchived.mutate(false, { onSuccess: () => toast.success("Grup arşivden çıkarıldı.") })
            }
          >
            Arşivden Çıkar
          </Button>
        ) : (
          <Button variant="outline" className="h-11 md:h-10" onClick={() => setArchiveOpen(true)}>
            <Archive aria-hidden />
            Arşivle
          </Button>
        )}
      </div>

      {g.archived ? (
        <Card>
          <CardContent className="space-y-1">
            <p className="text-[15px]">{g.description || "Açıklama yok."}</p>
            <p className="text-[13px] text-muted-foreground">
              Bu grup arşivlenmiş; değişiklik yapılamaz. Önce arşivden çıkarın.
            </p>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardHeader>
            <CardTitle className="text-base font-semibold">Grup bilgileri</CardTitle>
          </CardHeader>
          <CardContent>
            <GroupInfoForm key={`${g.name}|${g.description ?? ""}`} group={g} />
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader className="flex-row items-center justify-between gap-3">
          <CardTitle className="text-base font-semibold">Üyeler ({g.members.length})</CardTitle>
          {!g.archived && (
            <Button className="h-11 md:h-10" onClick={() => setAdding(true)}>
              <UserPlus aria-hidden />
              Üye Ekle
            </Button>
          )}
        </CardHeader>
        <CardContent>
          {g.members.length === 0 ? (
            <EmptyState icon={Users} message="Bu grupta henüz üye yok." />
          ) : (
            <ul className="divide-y">
              {g.members.map((m) => (
                <li key={m.userId} className="flex min-h-14 items-center gap-3 py-2">
                  <UserAvatar userId={m.userId} name={m.fullName} />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="truncate text-[15px] font-medium">{m.fullName}</span>
                      {m.status === "PASSIVE" && <StatusBadge>Pasif</StatusBadge>}
                    </div>
                    <div className="truncate text-[13px] text-muted-foreground">
                      {[m.title, m.branchName].filter(Boolean).join(" · ")}
                    </div>
                  </div>
                  {!g.archived && (
                    <Button
                      variant="ghost"
                      className="size-11 md:size-10"
                      aria-label={`${m.fullName} üyesini gruptan çıkar`}
                      onClick={() => setRemoving(m)}
                    >
                      <X aria-hidden />
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <GroupAddMembersDialog
        groupId={id}
        existingIds={g.members.map((m) => m.userId)}
        open={adding}
        onOpenChange={setAdding}
      />
      <ConfirmDialog
        open={!!removing}
        onOpenChange={(o) => !o && setRemoving(null)}
        title="Üye gruptan çıkarılsın mı?"
        description={`${removing?.fullName ?? ""} gruptan çıkarılacak ve grup sohbetinden ayrılacak.`}
        confirmLabel="Çıkar"
        destructive
        onConfirm={async () => {
          if (!removing) return;
          await removeMember.mutateAsync(removing.userId);
          toast.success("Üye gruptan çıkarıldı.");
        }}
      />
      <ConfirmDialog
        open={archiveOpen}
        onOpenChange={setArchiveOpen}
        title="Grup arşivlensin mi?"
        description="Grup arşivlenecek: yeni paylaşım ve mesajlarda seçilemeyecek, grup sohbeti salt-okunur olacak. Geçmiş içerik korunur."
        confirmLabel="Arşivle"
        destructive
        onConfirm={async () => {
          await setArchived.mutateAsync(true);
          toast.success("Grup arşivlendi.");
        }}
      />
    </div>
  );
}
