import { AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api";

type Props = {
  error?: unknown;
  onRetry?: () => void;
};

export function ErrorState({ error, onRetry }: Props) {
  const message =
    error instanceof ApiError
      ? error.message
      : "Bilgiler yüklenemedi. Lütfen tekrar deneyin.";
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-4 py-12 text-center" role="alert">
      <div className="flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
        <AlertCircle className="size-6" aria-hidden />
      </div>
      <p className="max-w-sm text-[15px]">{message}</p>
      {onRetry && (
        <Button variant="outline" className="h-11 md:h-10" onClick={onRetry}>
          Tekrar dene
        </Button>
      )}
    </div>
  );
}
