import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const TONES = {
  neutral: "bg-muted text-muted-foreground",
  info: "bg-info-bg text-primary",
  success: "bg-success-bg text-success",
  warning: "bg-warning-bg text-warning",
  danger: "bg-destructive/10 text-destructive",
} as const;

type Props = {
  tone?: keyof typeof TONES;
  children: React.ReactNode;
  className?: string;
};

export function StatusBadge({ tone = "neutral", children, className }: Props) {
  return <Badge className={cn("h-6 px-2.5", TONES[tone], className)}>{children}</Badge>;
}
