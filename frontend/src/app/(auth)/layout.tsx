import { Suspense } from "react";

const institutionName = process.env.NEXT_PUBLIC_INSTITUTION_NAME ?? "Okul";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <main className="flex min-h-dvh items-center justify-center px-4 py-8">
      <div className="w-full max-w-[400px]">
        <div className="mb-6 flex flex-col items-center gap-2 text-center">
          <img src="/logo.svg" alt="" className="size-12" />
          <div className="text-lg font-semibold">{institutionName}</div>
          <div className="text-[13px] text-muted-foreground">İç İletişim Sistemi</div>
        </div>
        <div className="rounded-xl border bg-card p-5 shadow-sm md:p-6">
          <Suspense>{children}</Suspense>
        </div>
      </div>
    </main>
  );
}
