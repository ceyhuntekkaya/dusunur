"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";

export function useUrlParams() {
  const sp = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  function get(key: string, fallback = ""): string {
    return sp.get(key) ?? fallback;
  }

  function set(patch: Record<string, string | undefined>) {
    const next = new URLSearchParams(sp.toString());
    for (const [k, v] of Object.entries(patch)) {
      if (v === undefined || v === "") next.delete(k);
      else next.set(k, v);
    }
    const s = next.toString();
    router.replace(s ? `${pathname}?${s}` : pathname, { scroll: false });
  }

  return { get, set };
}
