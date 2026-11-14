import type { TargetRef, TargetView } from "@/features/core/types";

export type CalAccess = "VIEW" | "CREATE" | "EDIT_ALL" | "OWNER";
export type CalendarSection =
  | "INSTITUTION"
  | "BRANCH"
  | "MINE"
  | "SHARED"
  | "GROUP"
  | "OTHER_GROUP"
  | "RESOURCE";

export type CalendarDto = {
  id: number;
  type: "INSTITUTION" | "BRANCH" | "PERSONAL" | "GROUP" | "RESOURCE";
  name: string;
  description?: string;
  color: string;
  section: CalendarSection;
  myAccess: CalAccess;
  canCreate: boolean;
  canManage: boolean;
  readOnly: boolean;
  isDefault: boolean;
  branchName?: string;
  owner?: { id: number; fullName: string };
};

export type EntryDto = {
  id: number;
  calendarId: number;
  title: string;
  allDay: boolean;
  startAt: string;
  endAt: string;
  startDate?: string;
  endDate?: string;
  location?: string;
  description?: string;
  file?: { name: string; contentType: string; sizeBytes: number };
  createdBy: { id: number; fullName: string };
  createdAt: string;
  updatedAt: string;
  canEdit: boolean;
};

export type EntryInput = {
  calendarId: number;
  title: string;
  allDay: boolean;
  startDate?: string;
  endDate?: string;
  startAt?: string;
  endAt?: string;
  location?: string;
  description?: string;
  fileId?: string;
  removeFile?: boolean;
};

export type CalShareLevel = "VIEW" | "EDIT";
export type CalShareRow = { id: number; subject: TargetView; level: CalShareLevel };

export type CalendarInput = { name: string; description?: string; branchId?: number };
export type ShareInput = { subject: TargetRef; level: CalShareLevel };
