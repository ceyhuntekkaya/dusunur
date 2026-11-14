import Link from "next/link";
import type { LucideIcon } from "lucide-react";
import { Button } from "@/components/ui/button";

type Props = {
  icon: LucideIcon;
  message: string;
  action?: { label: string; onClick?: () => void; href?: string };
};

export function EmptyState({ icon: Icon, message, action }: Props) {
  const linkProps = action?.href
    ? { render: <Link href={action.href} />, nativeButton: false as const }
    : {};
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-4 py-12 text-center">
      <div className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
        <Icon className="size-6" aria-hidden />
      </div>
      <p className="max-w-sm text-[15px] text-muted-foreground">{message}</p>
      {action && (
        <Button className="h-11 md:h-10" onClick={action.onClick} {...linkProps}>
          {action.label}
        </Button>
      )}
    </div>
  );
}
