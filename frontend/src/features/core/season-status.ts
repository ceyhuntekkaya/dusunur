import type { SeasonStatus } from "@/features/core/types";

export const SEASON_STATUS: Record<SeasonStatus, { label: string; tone: "warning" | "success" | "neutral" }> = {
  DRAFT: { label: "Hazırlık", tone: "warning" },
  ACTIVE: { label: "Aktif", tone: "success" },
  CLOSED: { label: "Kapandı", tone: "neutral" },
};
