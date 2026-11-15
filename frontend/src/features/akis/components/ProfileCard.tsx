"use client";

import Link from "next/link";
import { UserAvatar } from "@/components/common/UserAvatar";
import type { MeResponse } from "@/features/core/types";

export function ProfileCard({ me }: { me: MeResponse }) {
  return (
    <section className="overflow-hidden rounded-xl border bg-card" aria-label="Profil özeti">
      <div className="h-14 bg-gradient-to-r from-[#0A66C2] to-[#70B5F9]" aria-hidden />
      <div className="-mt-8 px-4 pb-4 text-center">
        <Link href="/profil" aria-label="Profilim" className="inline-block rounded-full outline-none focus-visible:ring-3 focus-visible:ring-ring/50">
          <UserAvatar userId={me.id} name={me.fullName} size="lg" className="size-16 border-2 border-card" />
        </Link>
        <Link href="/profil" className="mt-2 block truncate text-base font-semibold hover:underline">
          {me.fullName}
        </Link>
        {me.membership?.title && (
          <p className="mt-0.5 line-clamp-2 text-[13px] text-muted-foreground">{me.membership.title}</p>
        )}
      </div>
      {(me.membership?.branchName || me.season) && (
        <dl className="space-y-1 border-t px-4 py-3 text-[13px]">
          {me.membership?.branchName && (
            <div className="flex justify-between gap-2">
              <dt className="text-muted-foreground">Şube</dt>
              <dd className="truncate font-semibold text-primary">{me.membership.branchName}</dd>
            </div>
          )}
          {me.season && (
            <div className="flex justify-between gap-2">
              <dt className="text-muted-foreground">Yıl</dt>
              <dd className="truncate font-semibold text-primary">{me.season.name}</dd>
            </div>
          )}
        </dl>
      )}
    </section>
  );
}
