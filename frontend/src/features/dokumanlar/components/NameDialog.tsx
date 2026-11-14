"use client";

import { useMemo } from "react";
import { toast } from "sonner";
import { z } from "zod";
import { FormActions } from "@/components/common/FormActions";
import { FormField } from "@/components/common/FormField";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { Input } from "@/components/ui/input";
import { useApiForm } from "@/lib/form";
import { useCreateFolder, useRenameItem } from "../api";
import { dmsNameSchema } from "../schemas";
import { splitExtension } from "../utils";

export type NameTarget =
  | { mode: "create"; spaceId: number; parentId?: number }
  | { mode: "rename"; kind: "folder" | "file"; id: number; name: string };

function Form({ target, onClose }: { target: NameTarget; onClose: () => void }) {
  const create = useCreateFolder(target.mode === "create" ? target.spaceId : 0);
  const renameFolder = useRenameItem("folder");
  const renameFile = useRenameItem("file");

  const ext = target.mode === "rename" && target.kind === "file" ? splitExtension(target.name).ext : "";
  const initial =
    target.mode === "rename" ? (target.kind === "file" ? splitExtension(target.name).base : target.name) : "";
  const max = (target.mode === "rename" && target.kind === "file" ? 200 : 100) - ext.length;
  const schema = useMemo(() => z.object({ name: dmsNameSchema(max) }), [max]);
  const form = useApiForm(schema, { defaultValues: { name: initial } });
  const { register, formState } = form;

  const onSubmit = form.submit(async (v) => {
    if (target.mode === "create") {
      await create.mutateAsync({ parentId: target.parentId, name: v.name });
      toast.success("Klasör oluşturuldu.");
    } else {
      const m = target.kind === "file" ? renameFile : renameFolder;
      await m.mutateAsync({ id: target.id, name: v.name + ext });
      toast.success("Yeniden adlandırıldı.");
    }
    onClose();
  });

  const label = target.mode === "create" || target.kind === "folder" ? "Klasör adı" : "Dosya adı";
  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <FormField label={label} htmlFor="dms-name" required error={formState.errors.name?.message}>
        <div className="flex items-center gap-2">
          <Input
            id="dms-name"
            className="h-11 md:h-10"
            autoFocus
            aria-invalid={!!formState.errors.name}
            {...register("name")}
          />
          {ext && <span className="shrink-0 text-[15px] text-muted-foreground">{ext}</span>}
        </div>
      </FormField>
      <FormActions
        submitLabel={target.mode === "create" ? "Oluştur" : "Kaydet"}
        submitting={formState.isSubmitting}
        onCancel={onClose}
      />
    </form>
  );
}

export function NameDialog({ target, onClose }: { target: NameTarget | null; onClose: () => void }) {
  return (
    <ResponsiveDialog
      open={target !== null}
      onOpenChange={(o) => !o && onClose()}
      title={target?.mode === "rename" ? "Yeniden Adlandır" : "Yeni Klasör"}
    >
      {target && <Form target={target} onClose={onClose} />}
    </ResponsiveDialog>
  );
}
