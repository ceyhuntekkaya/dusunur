import type { TargetView } from "@/features/core/types";

export type DmsLevel = "VIEW" | "EDIT" | "MANAGE";
export type DmsPerson = { id: number; fullName: string };
export type SpaceRow = {
  id: number;
  name: string;
  description?: string;
  owner: DmsPerson;
  myLevel: DmsLevel;
  fileCount: number;
  totalBytes: number;
  lastUpdate?: string;
};
export type GrantRow = { id: number; subject: TargetView; level: DmsLevel };
export type FolderRow = { id: number; name: string; updatedAt: string; createdBy: DmsPerson; itemCount: number };
export type FileRow = {
  id: number;
  name: string;
  sizeBytes: number;
  contentType: string;
  previewable: boolean;
  updatedAt: string;
  updatedBy: DmsPerson;
  uploadedBy: DmsPerson;
  createdAt: string;
};
export type FolderContent = {
  space: SpaceRow;
  folder?: { id: number; name: string; parentId?: number };
  breadcrumb: { id: number; name: string }[];
  folders: FolderRow[];
  files: FileRow[];
};
export type FolderNode = { id: number; parentId?: number; name: string; depth: number };
export type TrashRow = {
  type: "FOLDER" | "FILE";
  id: number;
  name: string;
  originalPath: string;
  deletedBy: DmsPerson;
  deletedAt: string;
  purgeAt: string;
  sizeBytes?: number;
};
export type SearchResult = {
  type: "FOLDER" | "FILE";
  id: number;
  name: string;
  spaceId: number;
  spaceName: string;
  folderId?: number;
  path: string;
  sizeBytes?: number;
  contentType?: string;
};
export type RestoreResult = { movedToRoot: boolean; renamedTo?: string };
export type OnConflict = "ASK" | "OVERWRITE" | "RENAME";
