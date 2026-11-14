"use client";

import { formatDateText, formatFullDate } from "@/lib/format";

type Props = {
  value: string;
  relative?: boolean;
  className?: string;
};

export function DateText({ value, relative, className }: Props) {
  return (
    <time dateTime={value} title={formatFullDate(value)} className={className} suppressHydrationWarning>
      {formatDateText(value, relative)}
    </time>
  );
}
