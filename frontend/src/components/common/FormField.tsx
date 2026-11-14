import { AlertCircle } from "lucide-react";
import { Label } from "@/components/ui/label";

type Props = {
  label: string;
  htmlFor: string;
  required?: boolean;
  hint?: string;
  error?: string;
  children: React.ReactNode;
};

export function FormField({ label, htmlFor, required, hint, error, children }: Props) {
  return (
    <div className="space-y-1.5">
      <Label htmlFor={htmlFor} className="text-sm font-medium">
        {label}
        {required && <span className="text-destructive"> *</span>}
      </Label>
      {children}
      {hint && !error && <p className="text-[13px] text-muted-foreground">{hint}</p>}
      {error && (
        <p className="flex items-start gap-1.5 text-[13px] text-destructive" role="alert">
          <AlertCircle className="mt-0.5 size-3.5 shrink-0" aria-hidden />
          {error}
        </p>
      )}
    </div>
  );
}
