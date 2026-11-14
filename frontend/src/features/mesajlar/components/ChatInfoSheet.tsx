"use client";

import { useState } from "react";
import { Pencil, UserPlus, X } from "lucide-react";
import { Controller } from "react-hook-form";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { UserAvatar } from "@/components/common/UserAvatar";
import { UserPicker } from "@/components/common/UserPicker";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Switch } from "@/components/ui/switch";
import { useApiForm } from "@/lib/form";
import { useMe } from "@/lib/me";
import { useAddMembers, useMuteConversation, useRemoveMember, useRenameConversation } from "../api";
import { addMembersSchema, renameSchema } from "../schemas";
import type { ConversationDetail } from "../types";
import { ConversationAvatar } from "./ConversationAvatar";

function RenameForm({ detail, onDone }: { detail: ConversationDetail; onDone: () => void }) {
  const rename = useRenameConversation(detail.id);
  const form = useApiForm(renameSchema, { defaultValues: { title: detail.title } });
  const onSubmit = form.submit(async (v) => {
    await rename.mutateAsync(v.title);
    toast.success("Sohbet adı güncellendi.");
    onDone();
  });
  return (
    <form onSubmit={onSubmit} className="w-full space-y-2" noValidate>
      <FormField label="Sohbet adı" htmlFor="rn-title" error={form.formState.errors.title?.message}>
        <Input
          id="rn-title"
          className="h-11 md:h-10"
          maxLength={100}
          autoFocus
          aria-invalid={!!form.formState.errors.title}
          {...form.register("title")}
        />
      </FormField>
      <FormActions submitLabel="Kaydet" submitting={form.formState.isSubmitting} onCancel={onDone} />
    </form>
  );
}

function AddMembersForm({ detail, onClose }: { detail: ConversationDetail; onClose: () => void }) {
  const add = useAddMembers(detail.id);
  const form = useApiForm(addMembersSchema, { defaultValues: { userIds: [] } });
  const onSubmit = form.submit(async (v) => {
    const before = detail.members.length;
    const res = await add.mutateAsync(v.userIds.map((u) => u.id));
    toast.success(`${res.members.length - before} kişi eklendi.`);
    onClose();
  });
  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <FormField label="Kişiler" htmlFor="am-users" required error={form.formState.errors.userIds?.message}>
        <Controller
          control={form.control}
          name="userIds"
          render={({ field }) => (
            <UserPicker
              id="am-users"
              value={field.value}
              onChange={field.onChange}
              excludeIds={detail.members.map((m) => m.id)}
              invalid={!!form.formState.errors.userIds}
            />
          )}
        />
      </FormField>
      <FormActions submitLabel="Ekle" submitting={form.formState.isSubmitting} onCancel={onClose} />
    </form>
  );
}

type Props = {
  detail: ConversationDetail;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onLeave: () => void;
};

export function ChatInfoSheet({ detail, open, onOpenChange, onLeave }: Props) {
  const meId = useMe().data?.id;
  const [editing, setEditing] = useState(false);
  const [adding, setAdding] = useState(false);
  const [removing, setRemoving] = useState<ConversationDetail["members"][number] | null>(null);
  const mute = useMuteConversation(detail.id);
  const remove = useRemoveMember(detail.id);
  const other = detail.type === "DIRECT" ? detail.members.find((m) => m.id === detail.other?.id) : undefined;

  return (
    <>
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full gap-0 overflow-y-auto p-0 data-[side=right]:w-full sm:max-w-[360px] data-[side=right]:sm:max-w-[360px]">
        <SheetHeader className="border-b p-4">
          <SheetTitle className="text-base font-semibold">Sohbet Bilgisi</SheetTitle>
        </SheetHeader>

        <div className="flex flex-col items-center gap-3 p-6 text-center">
          <ConversationAvatar conversation={detail} size="xl" />
          {editing ? (
            <RenameForm detail={detail} onDone={() => setEditing(false)} />
          ) : (
            <div className="flex max-w-full items-center gap-1">
              <h2 className="truncate text-lg font-semibold">{detail.title}</h2>
              {detail.canManage && (
                <Button
                  variant="ghost"
                  size="icon"
                  className="size-11 md:size-9"
                  aria-label="Sohbet adını düzenle"
                  onClick={() => setEditing(true)}
                >
                  <Pencil className="size-4" aria-hidden />
                </Button>
              )}
            </div>
          )}
          {other && (
            <p className="text-[13px] text-muted-foreground">
              {[other.title, other.branchName].filter(Boolean).join(" · ")}
            </p>
          )}
        </div>

        {detail.type !== "DIRECT" && (
          <div className="border-t p-4">
            <div className="mb-2 flex items-center justify-between gap-2">
              <h3 className="text-sm font-semibold">{detail.memberCount} üye</h3>
              {detail.canManage && (
                <Button variant="outline" className="h-11 md:h-10" onClick={() => setAdding(true)}>
                  <UserPlus aria-hidden />
                  Kişi Ekle
                </Button>
              )}
            </div>
            <ul>
              {detail.members.map((m) => (
                <li key={m.id} className="flex min-h-14 items-center gap-3 py-1">
                  <UserAvatar userId={m.id} name={m.fullName} size="lg" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[15px]">{m.fullName}</p>
                    {m.title && <p className="truncate text-[13px] text-muted-foreground">{m.title}</p>}
                  </div>
                  {m.role === "OWNER" && <Badge variant="secondary">Sahip</Badge>}
                  {detail.canManage && m.id !== meId && (
                    <Button
                      variant="ghost"
                      size="icon"
                      className="size-11 md:size-9"
                      aria-label={`${m.fullName} kişisini sohbetten çıkar`}
                      onClick={() => setRemoving(m)}
                    >
                      <X className="size-4" aria-hidden />
                    </Button>
                  )}
                </li>
              ))}
            </ul>
            {detail.type === "GROUP" && (
              <p className="mt-3 text-[13px] text-muted-foreground">
                Bu sohbetin üyeleri &apos;{detail.title}&apos; grubunun üyeleridir. Üyelik okul yönetimi tarafından
                düzenlenir.
              </p>
            )}
          </div>
        )}

        <div className="space-y-3 border-t p-4">
          <div className="flex min-h-11 items-center justify-between gap-3">
            <Label htmlFor="ci-mute" className="text-[15px]">
              Sessize al
            </Label>
            <Switch
              id="ci-mute"
              checked={detail.muted}
              disabled={mute.isPending}
              onCheckedChange={(v) => mute.mutate(v)}
            />
          </div>
          {detail.canLeave && (
            <Button
              variant="outline"
              className="h-11 w-full text-destructive hover:text-destructive md:h-10"
              onClick={onLeave}
            >
              Sohbetten Ayrıl
            </Button>
          )}
        </div>
      </SheetContent>
    </Sheet>

      <ResponsiveDialog open={adding} onOpenChange={setAdding} title="Kişi Ekle">
        <AddMembersForm detail={detail} onClose={() => setAdding(false)} />
      </ResponsiveDialog>
      <ConfirmDialog
        open={removing !== null}
        onOpenChange={(o) => !o && setRemoving(null)}
        title="Kişi çıkarılsın mı?"
        description={`${removing?.fullName ?? ""} sohbetten çıkarılsın mı?`}
        confirmLabel="Çıkar"
        destructive
        onConfirm={async () => {
          if (!removing) return;
          await remove.mutateAsync(removing.id);
          toast.success(`${removing.fullName} sohbetten çıkarıldı.`);
        }}
      />
    </>
  );
}
