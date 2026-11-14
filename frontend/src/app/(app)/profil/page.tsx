"use client";

import { LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { InstallInstructions } from "@/components/common/InstallInstructions";
import { PageHeader } from "@/components/common/PageHeader";
import { useSignOut } from "@/features/core/api";
import { ChangePasswordCard } from "@/features/core/components/ChangePasswordCard";
import { PushSettingsCard } from "@/features/core/components/PushSettingsCard";
import { useMe } from "@/lib/me";
import { isStandalone } from "@/lib/pwa";

function InfoRow({ label, value }: { label: string; value?: string }) {
  return (
    <div className="grid gap-0.5 sm:grid-cols-[180px_1fr] sm:gap-3">
      <dt className="text-[13px] text-muted-foreground">{label}</dt>
      <dd className="text-[15px]">{value || "—"}</dd>
    </div>
  );
}

export default function ProfilePage() {
  const me = useMe().data;
  const { signOut, pending } = useSignOut();
  if (!me) return null;

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <PageHeader title="Profilim" />

      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold">Bilgilerim</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <dl className="space-y-3">
            <InfoRow label="Ad soyad" value={me.fullName} />
            <InfoRow label="E-posta" value={me.email} />
            <InfoRow label="Telefon" value={me.phone} />
            <InfoRow label="Branş" value={me.discipline} />
            <InfoRow label="Şube" value={me.membership?.branchName} />
            <InfoRow label="Unvan" value={me.membership?.title} />
            <InfoRow label="Fotokopi onaylayıcısı" value={me.membership?.printApproverName} />
          </dl>
          <p className="text-[13px] text-muted-foreground">
            Bilgilerinizde hata varsa okul yönetimine bildirin.
          </p>
        </CardContent>
      </Card>

      <PushSettingsCard />

      {!isStandalone() && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base font-semibold">Uygulamayı yükle</CardTitle>
          </CardHeader>
          <CardContent>
            <InstallInstructions />
          </CardContent>
        </Card>
      )}

      <ChangePasswordCard />

      <Button
        variant="outline"
        className="h-11 w-full text-destructive md:h-10 md:w-auto"
        disabled={pending}
        onClick={signOut}
      >
        <LogOut aria-hidden />
        Çıkış Yap
      </Button>
    </div>
  );
}
