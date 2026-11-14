export type MeResponse = {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  discipline?: string;
  systemAdmin: boolean;
  kvkkRequired: boolean;
  season?: { id: number; name: string };
  membership?: {
    branchId: number;
    branchName: string;
    title?: string;
    manager: boolean;
    printOperator: boolean;
    canCreateDmsSpace: boolean;
    printApproverId?: number;
    printApproverName?: string;
  };
  permissions: {
    member: boolean;
    canPostToAll: boolean;
    canPostImportant: boolean;
    canManageGroups: boolean;
    canEditInstitutionCalendar: boolean;
    canCreateDmsSpace: boolean;
    printOperator: boolean;
    canAccessAdmin: boolean;
  };
};

export type LoginRequest = { email: string; password: string };

export type KvkkResponse = { version: number; text: string };

export type PasswordTokenInfo = {
  purpose: "SETUP" | "RESET";
  firstName: string;
  email: string;
};

export type BadgeMap = Record<string, number>;

export type NotificationDto = {
  id: number;
  type: string;
  title: string;
  body?: string;
  link?: string;
  createdAt: string;
  read: boolean;
};

export type DirUser = {
  id: number;
  fullName: string;
  title?: string;
  branchName?: string;
  discipline?: string;
};
export type DirGroup = { id: number; name: string; memberCount: number };
export type DirBranch = { id: number; name: string };
export type DirectorySearchResponse = { users: DirUser[]; groups: DirGroup[]; branches: DirBranch[] };

export type TargetType = "USER" | "GROUP" | "BRANCH" | "ALL";
export type TargetRef = { type: TargetType; id?: number };
export type TargetView = TargetRef & { name: string };

export type FilePurpose = "FEED" | "CHAT" | "PRINT" | "DMS" | "CALENDAR";
export type UploadedFileDto = { id: string; originalName: string; contentType: string; sizeBytes: number };

export type WsEnvelope = { type: string; payload: unknown };

export type PushSubscriptionRequest = { endpoint: string; keys: { p256dh: string; auth: string } };

export type ChangePasswordRequest = { currentPassword: string; newPassword: string };

export type MembershipInfo = {
  id: number;
  branchId: number;
  branchName: string;
  title?: string;
  manager: boolean;
  printOperator: boolean;
  canCreateDmsSpace: boolean;
  printApproverId?: number;
  printApproverName?: string;
};

export type AdminUserRow = {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  discipline?: string;
  status: "ACTIVE" | "PASSIVE";
  systemAdmin: boolean;
  passwordSet: boolean;
  lastLoginAt?: string;
  membership?: MembershipInfo;
};

export type MembershipInput = {
  branchId: number;
  title?: string;
  manager: boolean;
  printOperator: boolean;
  canCreateDmsSpace: boolean;
  printApproverId?: number;
};

export type UserInput = {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  discipline?: string;
  systemAdmin: boolean;
  membership?: MembershipInput;
};

export type ImpactItem = { label: string; count: number };
export type DeactivationCheck = { items: ImpactItem[]; successorRequired: boolean };

export type BranchRow = { id: number; name: string; active: boolean; memberCount: number };

export type SeasonStatus = "DRAFT" | "ACTIVE" | "CLOSED";
export type SeasonRow = {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: SeasonStatus;
  memberCount: number;
  activatedAt?: string;
  closedAt?: string;
};
export type SeasonInput = { name: string; startDate: string; endDate: string };

export type MembershipRow = MembershipInfo & {
  userId: number;
  fullName: string;
  email: string;
  userStatus: "ACTIVE" | "PASSIVE";
};

export type MembershipFilter = "manager" | "printOperator" | "dmsCreator" | "noApprover";

export type ActivationPreview = { canActivate: boolean; blockers: string[]; items: string[] };

export type BulkUpdateRequest = {
  membershipIds: number[];
  branchId?: number;
  printApproverId?: number;
  clearApprover?: boolean;
};

export type CopyMembershipsRequest = { sourceSeasonId: number; userIds: number[] };
export type CopyMembershipsResult = { created: number; skippedExisting: number; approverCleared: number };

export type GroupRow = {
  id: number;
  name: string;
  description?: string;
  memberCount: number;
  archived: boolean;
  createdAt: string;
};
export type GroupMember = {
  userId: number;
  fullName: string;
  title?: string;
  branchName?: string;
  status: "ACTIVE" | "PASSIVE";
};
export type GroupDetail = GroupRow & { members: GroupMember[] };

export type AuditRow = {
  id: number;
  createdAt: string;
  actorId?: number;
  actorName?: string;
  ip?: string;
  action: string;
  actionLabel: string;
  entityType?: string;
  entityId?: string;
  detail?: unknown;
};
export type AuditActionOption = { code: string; label: string };
