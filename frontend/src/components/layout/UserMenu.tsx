"use client";

import Link from "next/link";
import { ChevronDown, LogOut, Settings, User } from "lucide-react";
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
  variant: "nav" | "avatar";
};

export function UserMenu({ me, variant }: Props) {
  const { signOut } = useSignOut();
  const admin = adminItem(me);

  return (
    <DropdownMenu>
      {variant === "nav" ? (
        <DropdownMenuTrigger
          className="relative flex h-full w-[72px] flex-col items-center justify-center gap-0.5 border-b-2 border-transparent text-[12px] text-muted-foreground outline-none hover:text-foreground focus-visible:bg-muted aria-expanded:border-foreground aria-expanded:text-foreground"
          aria-label="Hesap menüsü"
        >
          <UserAvatar userId={me.id} name={me.fullName} size="sm" />
          <span className="inline-flex items-center leading-4">
            Ben
            <ChevronDown className="size-3.5" aria-hidden />
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
      <DropdownMenuContent align={variant === "nav" ? "end" : "start"} className="w-64">
        <div className="flex items-center gap-3 px-2 py-2">
          <UserAvatar userId={me.id} name={me.fullName} size="lg" />
          <div className="min-w-0">
            <p className="truncate text-[15px] font-semibold">{me.fullName}</p>
            {me.membership?.title && (
              <p className="truncate text-[13px] text-muted-foreground">{me.membership.title}</p>
            )}
          </div>
        </div>
        <DropdownMenuSeparator />
        <DropdownMenuItem className="min-h-11 gap-2" render={<Link href="/profil" />}>
          <User className="size-4" aria-hidden />
          Profilim
        </DropdownMenuItem>
        {admin && variant === "avatar" && (
          <DropdownMenuItem className="min-h-11 gap-2" render={<Link href={admin.href} />}>
            <Settings className="size-4" aria-hidden />
            Yönetim
          </DropdownMenuItem>
        )}
        <DropdownMenuSeparator />
        <DropdownMenuItem className="min-h-11 gap-2" onClick={signOut}>
          <LogOut className="size-4" aria-hidden />
          Çıkış Yap
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
