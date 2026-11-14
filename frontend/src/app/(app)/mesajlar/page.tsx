"use client";

import { MessageCircle } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";

export default function Page() {
  return (
    <div className="m-auto">
      <EmptyState icon={MessageCircle} message="Bir sohbet seçin veya yeni bir sohbet başlatın." />
    </div>
  );
}
