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
