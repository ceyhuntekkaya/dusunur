export type PrintStatus =
  | "PENDING_APPROVAL"
  | "APPROVED"
  | "REJECTED"
  | "IN_PROGRESS"
  | "READY"
  | "DELIVERED"
  | "CANCELLED";
export type PrintColor = "BW" | "COLOR";
export type PrintSides = "SINGLE" | "DOUBLE";
export type PaperSize = "A4" | "A3";

export type PersonRef = { id: number; fullName: string; branchName?: string };

export type PrintRequestRow = {
  id: number;
  seasonId: number;
  archived: boolean;
  title: string;
  secret: boolean;
  status: PrintStatus;
  requester: PersonRef;
  branchName: string;
  pageCount: number;
  copies: number;
  totalPages: number;
  color: PrintColor;
  sides: PrintSides;
  paperSize: PaperSize;
  originalOnPaper: boolean;
  dueAt: string;
  createdAt: string;
  approver?: PersonRef;
};

export type PrintHistoryItem = {
  fromStatus?: PrintStatus;
  toStatus: PrintStatus;
  actor?: PersonRef;
  note?: string;
  createdAt: string;
};

export type PrintRequestDetail = PrintRequestRow & {
  note?: string;
  rejectReason?: string;
  cancelReason?: string;
  originalApprover?: PersonRef;
  decidedBy?: PersonRef;
  decidedAt?: string;
  operator?: PersonRef;
  startedAt?: string;
  readyAt?: string;
  deliveredAt?: string;
  cancelledAt?: string;
  file?: { name: string; contentType: string; sizeBytes: number; purged: boolean };
  history: PrintHistoryItem[];
  actions: {
    canCancel: boolean;
    canCopy: boolean;
    canApprove: boolean;
    canReject: boolean;
    canStart: boolean;
    canMarkReady: boolean;
    canDeliver: boolean;
    canDownload: boolean;
  };
};

export type CreatePrintRequest = {
  title: string;
  fileId?: string;
  originalOnPaper: boolean;
  pageCount: number;
  copies: number;
  color: PrintColor;
  sides: PrintSides;
  paperSize: PaperSize;
  secret: boolean;
  dueAt: string;
  note?: string;
  copiedFromId?: number;
};

export type PrintContext = {
  isApprover: boolean;
  isOperator: boolean;
  isManager: boolean;
  branchName: string;
  myApprover?: PersonRef;
  myApproverOnLeave?: { approver: PersonRef; delegate: PersonRef; endDate: string };
  minLeadMinutes: number;
  maxDaysAhead: number;
};

export type DelegationDto = { id: number; delegate: PersonRef; startDate: string; endDate: string; active: boolean };
export type CreateDelegationRequest = { delegateId: number; startDate: string; endDate: string };

export type QueueBoard = { approved: PrintRequestRow[]; inProgress: PrintRequestRow[]; ready: PrintRequestRow[] };

export type ReportRow = {
  userId: number;
  fullName: string;
  branchName: string;
  requestCount: number;
  bwPages: number;
  colorPages: number;
  totalPages: number;
};
export type MonthlyReport = {
  month: string;
  rows: ReportRow[];
  totals: Omit<ReportRow, "userId" | "fullName" | "branchName">;
};

export type PrintSeason = { id: number; name: string; active: boolean };
export type PrintChangedEvent = { requestId: number; status: PrintStatus };

export type PrintAction = "approve" | "reject" | "cancel" | "start" | "ready" | "deliver";
export type MineGroup = "open" | "ready" | "delivered" | "closed";
