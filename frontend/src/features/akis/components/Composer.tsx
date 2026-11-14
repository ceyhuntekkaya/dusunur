"use client";

import { useEffect, useRef, useState } from "react";
import { Controller } from "react-hook-form";
import { Building2, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { FileUploader } from "@/components/common/FileUploader";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { TargetPicker } from "@/components/common/TargetPicker";
import type { TargetType } from "@/features/core/types";
import { useApiForm } from "@/lib/form";
import { useMe } from "@/lib/me";
import { useCreatePost } from "../api";
import { composerSchema } from "../schemas";

const ADMIN_TYPES: TargetType[] = ["ALL", "BRANCH", "GROUP", "USER"];
const STAFF_TYPES: TargetType[] = ["GROUP", "USER"];
const LINE = 24;

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onPosted?: () => void;
};

function ComposerForm({
  onClose,
  onCancel,
  onPosted,
  onDirty,
  onImportant,
}: {
  onClose: () => void;
  onCancel: () => void;
  onPosted?: () => void;
  onDirty: (d: boolean) => void;
  onImportant: (v: boolean) => void;
}) {
  const me = useMe().data;
  const create = useCreatePost();
  const [uploading, setUploading] = useState(false);
  const form = useApiForm(composerSchema, {
    defaultValues: { targets: [], content: "", files: [], important: false },
  });
  const { control, register, formState, watch, setValue } = form;
  const textRef = useRef<HTMLTextAreaElement | null>(null);
  const { ref: contentRef, ...contentProps } = register("content");

  const canPostToAll = !!me?.permissions.canPostToAll;
  const canImportant = !!me?.permissions.canPostImportant;
  const content = watch("content");
  const targets = watch("targets");
  const files = watch("files");
  const important = watch("important");

  useEffect(() => {
    onDirty(content.trim() !== "" || targets.length > 0 || files.length > 0);
  }, [content, targets, files, onDirty]);

  useEffect(() => {
    onImportant(canImportant && important);
    return () => onImportant(false);
  }, [canImportant, important, onImportant]);

  useEffect(() => {
    const el = textRef.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = `${Math.min(Math.max(el.scrollHeight, 4 * LINE + 16), 12 * LINE + 16)}px`;
  }, [content]);

  const onSubmit = form.submit(async (v) => {
    const res = await create.mutateAsync({
      targets: v.targets.map((t) => ({ type: t.type, id: t.id })),
      content: v.content || undefined,
      fileIds: v.files.map((f) => f.id),
      important: canImportant && v.important,
    });
    toast.success(
      v.important
        ? `Önemli duyuru yayınlandı. ${res.recipientCount} kişiye bildirim gönderildi.`
        : "Paylaşımınız yayınlandı.",
    );
    onDirty(false);
    onClose();
    onPosted?.();
  });

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <FormField
        label="Kime"
        htmlFor="c-targets"
        required
        hint={canPostToAll ? undefined : "Üyesi olduğunuz grupları veya kişileri seçebilirsiniz."}
        error={formState.errors.targets?.message ?? formState.errors.targets?.root?.message}
      >
        <div className="space-y-2">
          {canPostToAll && !targets.some((t) => t.type === "ALL") && (
            <Button
              type="button"
              variant="outline"
              className="h-10 rounded-full"
              onClick={() => setValue("targets", [{ type: "ALL", name: "Tüm Kurum" }], { shouldDirty: true, shouldValidate: true })}
            >
              <Building2 aria-hidden />
              Tüm Kurum
            </Button>
          )}
          <Controller
            control={control}
            name="targets"
            render={({ field }) => (
              <TargetPicker
                id="c-targets"
                value={field.value}
                onChange={field.onChange}
                allowed={canPostToAll ? ADMIN_TYPES : STAFF_TYPES}
                groupScope={canPostToAll ? "all" : "mine"}
              />
            )}
          />
        </div>
      </FormField>

      <FormField label="Metin" htmlFor="c-content" error={formState.errors.content?.message}>
        <Textarea
          id="c-content"
          placeholder="Ne paylaşmak istersiniz?"
          className="min-h-24 resize-none"
          aria-invalid={!!formState.errors.content}
          ref={(el) => {
            contentRef(el);
            textRef.current = el;
          }}
          {...contentProps}
        />
        {content.length > 4500 && (
          <p className="text-right text-xs text-muted-foreground">{content.length}/5000</p>
        )}
      </FormField>

      <FormField label="Ekler" htmlFor="c-files" error={formState.errors.files?.message}>
        <FileUploader
          purpose="FEED"
          multiple
          compressImages
          maxFiles={10}
          onUploadingChange={setUploading}
          onChange={(list) => setValue("files", list, { shouldDirty: true, shouldValidate: true })}
        />
      </FormField>

      {canImportant && (
        <div className="flex items-start gap-3 rounded-lg border p-3">
          <Switch
            id="c-important"
            className="mt-1"
            checked={important}
            onCheckedChange={(v) => setValue("important", v, { shouldDirty: true })}
          />
          <div className="space-y-0.5">
            <Label htmlFor="c-important" className="text-[15px] font-medium">
              Önemli duyuru olarak yayınla
            </Label>
            <p className="text-[13px] text-muted-foreground">
              Tüm alıcılar bildirim alır ve &lsquo;Okudum&rsquo; onayı vermeleri istenir. Kimlerin okuduğunu görebilirsiniz.
            </p>
          </div>
        </div>
      )}

      <div className="flex flex-col-reverse gap-2 pt-2 md:flex-row md:justify-end">
        <Button type="button" variant="outline" className="h-11 md:h-10" disabled={formState.isSubmitting} onClick={onCancel}>
          Vazgeç
        </Button>
        <Button type="submit" className="h-11 md:h-10" disabled={formState.isSubmitting || uploading}>
          {(formState.isSubmitting || uploading) && <Loader2 className="animate-spin" aria-hidden />}
          {uploading ? "Dosyalar yükleniyor…" : "Paylaş"}
        </Button>
      </div>
    </form>
  );
}

export function Composer({ open, onOpenChange, onPosted }: Props) {
  const [dirty, setDirty] = useState(false);
  const [confirmClose, setConfirmClose] = useState(false);
  const [important, setImportant] = useState(false);

  function requestClose(next: boolean) {
    if (next) return onOpenChange(true);
    if (dirty) setConfirmClose(true);
    else onOpenChange(false);
  }

  return (
    <>
      <ResponsiveDialog
        open={open}
        onOpenChange={requestClose}
        title="Yeni Paylaşım"
        className={important ? "ring-2 ring-important sm:max-w-[600px]" : "sm:max-w-[600px]"}
      >
        {open && (
          <ComposerForm
            onClose={() => onOpenChange(false)}
            onCancel={() => requestClose(false)}
            onImportant={setImportant}
            onPosted={onPosted}
            onDirty={setDirty}
          />
        )}
      </ResponsiveDialog>
      <ConfirmDialog
        open={confirmClose}
        onOpenChange={setConfirmClose}
        title="Paylaşım kapatılsın mı?"
        description="Paylaşımınız kaydedilmedi. Çıkmak istediğinize emin misiniz?"
        confirmLabel="Çık"
        destructive
        onConfirm={() => {
          setDirty(false);
          onOpenChange(false);
        }}
      />
    </>
  );
}
