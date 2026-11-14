"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { Controller } from "react-hook-form";
import { toast } from "sonner";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { SegmentedControl } from "@/components/common/SegmentedControl";
import { UserPicker } from "@/components/common/UserPicker";
import { Input } from "@/components/ui/input";
import { useApiForm } from "@/lib/form";
import { useMe } from "@/lib/me";
import { useCreateChatGroup, useOpenDirect } from "../api";
import { newGroupSchema } from "../schemas";

type Tab = "direct" | "group";

function DirectTab({ onDone }: { onDone: (id: number) => void }) {
  const meId = useMe().data?.id;
  const open = useOpenDirect();
  return (
    <div className="space-y-3">
      <FormField label="Kişi" htmlFor="nc-user">
        <UserPicker
          id="nc-user"
          multiple={false}
          value={[]}
          excludeIds={meId ? [meId] : []}
          placeholder="Kişi seçin"
          onChange={(users) => {
            if (users[0] && !open.isPending) {
              open.mutate(users[0].id, { onSuccess: (c) => onDone(c.id) });
            }
          }}
        />
      </FormField>
      {open.isPending && (
        <p className="flex items-center gap-2 text-[13px] text-muted-foreground">
          <Loader2 className="size-4 animate-spin" aria-hidden />
          Sohbet açılıyor…
        </p>
      )}
    </div>
  );
}

function GroupTab({ onDone, onClose }: { onDone: (id: number) => void; onClose: () => void }) {
  const meId = useMe().data?.id;
  const create = useCreateChatGroup();
  const form = useApiForm(newGroupSchema, { defaultValues: { title: "", memberIds: [] } });
  const { register, formState, control } = form;

  const onSubmit = form.submit(async (v) => {
    const c = await create.mutateAsync({ title: v.title, memberIds: v.memberIds.map((u) => u.id) });
    toast.success("Grup sohbeti oluşturuldu.");
    onDone(c.id);
  });

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <FormField label="Sohbet adı" htmlFor="ng-title" required error={formState.errors.title?.message}>
        <Input
          id="ng-title"
          className="h-11 md:h-10"
          maxLength={100}
          aria-invalid={!!formState.errors.title}
          {...register("title")}
        />
      </FormField>
      <FormField
        label="Kişiler"
        htmlFor="ng-members"
        required
        error={formState.errors.memberIds?.message}
      >
        <Controller
          control={control}
          name="memberIds"
          render={({ field }) => (
            <UserPicker
              id="ng-members"
              value={field.value}
              onChange={field.onChange}
              excludeIds={meId ? [meId] : []}
              invalid={!!formState.errors.memberIds}
            />
          )}
        />
      </FormField>
      <p className="text-[13px] text-muted-foreground">
        Grup sohbetlerini (zümre, kurul vb.) yönetim otomatik oluşturur; burada kendi küçük gruplarınızı
        açabilirsiniz.
      </p>
      <FormActions submitLabel="Sohbeti Oluştur" submitting={formState.isSubmitting} onCancel={onClose} />
    </form>
  );
}

function Content({ onClose }: { onClose: () => void }) {
  const router = useRouter();
  const [tab, setTab] = useState<Tab>("direct");

  function done(id: number) {
    onClose();
    router.push(`/mesajlar/${id}`);
  }

  return (
    <div className="space-y-4">
      <SegmentedControl<Tab>
        kind="tabs"
        ariaLabel="Sohbet türü"
        value={tab}
        onChange={setTab}
        options={[
          { value: "direct", label: "Kişi" },
          { value: "group", label: "Grup sohbeti" },
        ]}
      />
      {tab === "direct" ? <DirectTab onDone={done} /> : <GroupTab onDone={done} onClose={onClose} />}
    </div>
  );
}

export function NewChatDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (o: boolean) => void }) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title="Yeni Sohbet">
      <Content onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
