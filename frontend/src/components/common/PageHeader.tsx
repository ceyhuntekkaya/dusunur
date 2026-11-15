"use client";

import Link from "next/link";
import type { LucideIcon } from "lucide-react";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";

export type PrimaryAction = {
  label: string;
  onClick?: () => void;
  href?: string;
  icon?: LucideIcon;
  disabled?: boolean;
  disabledReason?: string;
};

type Props = {
  title: string;
  description?: string;
  actions?: React.ReactNode;
  primaryAction?: PrimaryAction;
};

export function PageHeader({ title, description, actions, primaryAction }: Props) {
  const Icon = primaryAction?.icon ?? Plus;
  const linkProps = primaryAction?.href
    ? { render: <Link href={primaryAction.href} />, nativeButton: false as const }
    : {};

  return (
    <div className="mb-4 flex flex-wrap items-start justify-between gap-3 rounded-xl border bg-card px-4 py-3 md:px-5 md:py-4">
      <div className="min-w-0">
        <h1 className="text-xl font-semibold md:text-[22px] md:leading-7">{title}</h1>
        {description && <p className="mt-1 text-[13px] text-muted-foreground">{description}</p>}
      </div>
      <div className="flex items-center gap-2">
        {actions}
        {primaryAction && (
          <span title={primaryAction.disabled ? primaryAction.disabledReason : undefined} className="hidden md:inline-flex">
            <Button
              className="h-10"
              disabled={primaryAction.disabled}
              onClick={primaryAction.onClick}
              {...linkProps}
            >
              <Icon aria-hidden />
              {primaryAction.label}
            </Button>
          </span>
        )}
      </div>
      {primaryAction && (
        <Button
          aria-label={primaryAction.label}
          disabled={primaryAction.disabled}
          className="fixed right-4 bottom-[calc(64px+env(safe-area-inset-bottom)+16px)] z-30 size-14 rounded-full shadow-lg md:hidden"
          onClick={primaryAction.onClick}
          {...linkProps}
        >
          <Icon className="size-6" aria-hidden />
        </Button>
      )}
    </div>
  );
}
