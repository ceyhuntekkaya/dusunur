"use client";

import { useState } from "react";
import { toast } from "sonner";
import { CheckboxField } from "@/components/common/CheckboxField";
import { ErrorState } from "@/components/common/ErrorState";
import { FormActions } from "@/components/common/FormActions";
import { ResponsiveDialog } from "@/components/common/ResponsiveDialog";
import { Skeleton } from "@/components/ui/skeleton";
import { useActivateSeason, useActivationPreview } from "@/features/core/admin-api";
import type { SeasonRow } from "@/features/core/types";

type Props = { season: SeasonRow; open: boolean; onOpenChange: (open: boolean) => void };

function Body({ season, onClose }: { season: SeasonRow; onClose: () => void }) {
  const preview = useActivationPreview(season.id, true);
  const activate = useActivateSeason();
  const [confirmed, setConfirmed] = useState(false);
  const [pending, setPending] = useState(false);

  if (preview.isPending) return <Skeleton className="h-32 w-full" />;
  if (preview.isError) return <ErrorState error={preview.error} onRetry={() => void preview.refetch()} />;

  const { canActivate, blockers, items } = preview.data;

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    try {
      await activate.mutateAsync(season.id);
      toast.success(`${season.name} eğitim-öğretim yılı aktifleştirildi.`);
      onClose();
    } catch {
      // Hata global toast ile gösterilir.
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      {blockers.length > 0 && (
        <ul
          className="list-disc space-y-1 rounded-lg bg-destructive/10 py-3 pr-3 pl-7 text-sm text-destructive"
          role="alert"
        >
          {blockers.map((b) => (
            <li key={b}>{b}</li>
          ))}
        </ul>
      )}
      {items.length > 0 && (
        <ul className="list-disc space-y-1.5 pl-5 text-sm">
          {items.map((i) => (
            <li key={i}>{i}</li>
          ))}
        </ul>
      )}
      <CheckboxField
        id="activate-confirm"
        checked={confirmed}
        onCheckedChange={setConfirmed}
        label="Yukarıdakileri okudum, onaylıyorum."
        disabled={!canActivate}
      />
      <FormActions
        submitLabel="Yılı Aktifleştir"
        submitting={pending}
        disabled={!canActivate || !confirmed}
        onCancel={onClose}
      />
    </form>
  );
}

export function ActivateSeasonDialog({ season, open, onOpenChange }: Props) {
  return (
    <ResponsiveDialog open={open} onOpenChange={onOpenChange} title={`${season.name} yılını aktifleştir`}>
      <Body season={season} onClose={() => onOpenChange(false)} />
    </ResponsiveDialog>
  );
}
