"use client";

import { SearchX } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";

export default function NotFound() {
  return (
    <div className="flex min-h-dvh items-center justify-center">
      <EmptyState icon={SearchX} message="Aradığınız sayfa bulunamadı." action={{ label: "Akış'a dön", href: "/akis" }} />
    </div>
  );
}
