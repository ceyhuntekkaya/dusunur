"use client";

import Link from "next/link";
import { LogOut, Settings, User } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { UserAvatar } from "@/components/common/UserAvatar";
import { useSignOut } from "@/features/core/api";
import type { MeResponse } from "@/features/core/types";
import { adminItem } from "@/components/layout/nav-items";

type Props = {
  me: MeResponse;
  variant: "card" | "avatar";
  side?: "top" | "bottom";
};

export function UserMenu({ me, variant, side = "bottom" }: Props) {
  const { signOut } = useSignOut();
  const admin = adminItem(me);

  return (
    <DropdownMenu>
      {variant === "card" ? (
        <DropdownMenuTrigger
          className="flex min-h-14 w-full items-center gap-3 rounded-lg p-2 text-left outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50"
          aria-label="Hesap menüsü"
        >
          <UserAvatar userId={me.id} name={me.fullName} />
          <span className="hidden min-w-0 flex-1 lg:block">
            <span className="block truncate text-sm font-medium">{me.fullName}</span>
            {me.membership?.title && (
              <span className="block truncate text-[13px] text-muted-foreground">
                {me.membership.title}
              </span>
            )}
          </span>
        </DropdownMenuTrigger>
      ) : (
        <DropdownMenuTrigger
          className="flex size-11 items-center justify-center rounded-full outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          aria-label="Hesap menüsü"
        >
          <UserAvatar userId={me.id} name={me.fullName} />
        </DropdownMenuTrigger>
      )}
      <DropdownMenuContent align={variant === "card" ? "start" : "end"} side={side} className="w-52">
        {admin && variant === "avatar" && (
          <DropdownMenuItem className="min-h-11 gap-2" render={<Link href={admin.href} />}>
            <Settings className="size-4" aria-hidden />
            Yönetim
          </DropdownMenuItem>
        )}
        <DropdownMenuItem className="min-h-11 gap-2" render={<Link href="/profil" />}>
          <User className="size-4" aria-hidden />
          Profilim
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem className="min-h-11 gap-2" onClick={signOut}>
          <LogOut className="size-4" aria-hidden />
          Çıkış Yap
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
