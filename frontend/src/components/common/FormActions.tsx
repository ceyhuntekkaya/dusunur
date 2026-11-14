"use client";

import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";

type Props = {
  submitLabel: string;
  submitting?: boolean;
  disabled?: boolean;
  destructive?: boolean;
  onCancel: () => void;
  cancelLabel?: string;
};

export function FormActions({
  submitLabel,
  submitting,
  disabled,
  destructive,
  onCancel,
  cancelLabel = "Vazgeç",
}: Props) {
  return (
    <div className="flex flex-col-reverse gap-2 pt-2 md:flex-row md:justify-end">
      <Button
        type="button"
        variant="outline"
        className="h-11 md:h-10"
        disabled={submitting}
        onClick={onCancel}
      >
        {cancelLabel}
      </Button>
      <Button
        type="submit"
        variant={destructive ? "destructive" : "default"}
        className="h-11 md:h-10"
        disabled={submitting || disabled}
      >
        {submitting && <Loader2 className="animate-spin" aria-hidden />}
        {submitLabel}
      </Button>
    </div>
  );
}
