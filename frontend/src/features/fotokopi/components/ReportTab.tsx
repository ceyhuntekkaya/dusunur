"use client";

import { useState } from "react";
import { format } from "date-fns";
import { Download } from "lucide-react";
import { DataTable, type Column } from "@/components/common/DataTable";
import { SimpleSelect } from "@/components/common/SimpleSelect";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useActiveBranches } from "@/features/core/admin-api";
import { cn } from "@/lib/utils";
import { useMonthlyReport } from "../api";
import type { ReportRow } from "../types";

const TOTAL_ID = -1;
const num = (n: number) => n.toLocaleString("tr-TR");

export function ReportTab() {
  const [month, setMonth] = useState(() => format(new Date(), "yyyy-MM"));
  const [branch, setBranch] = useState("all");
  const branches = useActiveBranches();
  const branchId = branch === "all" ? undefined : branch;
  const valid = /^\d{4}-(0[1-9]|1[0-2])$/.test(month);
  const report = useMonthlyReport(month, branchId, valid);

  const rows: ReportRow[] | undefined = !valid
    ? []
    : report.data
    ? report.data.rows.length === 0
      ? []
      : [
          ...report.data.rows,
          { userId: TOTAL_ID, fullName: "TOPLAM", branchName: "", ...report.data.totals },
        ]
    : undefined;

  const bold = (r: ReportRow) => (r.userId === TOTAL_ID ? "font-semibold" : undefined);
  const numCol = (key: keyof ReportRow, header: string): Column<ReportRow> => ({
    key,
    header,
    className: "text-right",
    cell: (r) => <span className={cn(bold(r))}>{num(r[key] as number)}</span>,
  });
  const columns: Column<ReportRow>[] = [
    { key: "fullName", header: "Ad Soyad", cell: (r) => <span className={cn(bold(r))}>{r.fullName}</span> },
    { key: "branchName", header: "Şube", cell: (r) => r.branchName },
    numCol("requestCount", "Talep"),
    numCol("bwPages", "Siyah-Beyaz"),
    numCol("colorPages", "Renkli"),
    numCol("totalPages", "Toplam"),
  ];

  const csvParams = new URLSearchParams({ month });
  if (branchId) csvParams.set("branchId", branchId);

  return (
    <div>
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end">
        <div className="space-y-1.5">
          <Label htmlFor="report-month" className="text-sm font-medium">
            Ay
          </Label>
          <Input
            id="report-month"
            type="month"
            className="h-11 sm:w-48 md:h-10"
            value={month}
            onChange={(e) => setMonth(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="report-branch" className="text-sm font-medium">
            Şube
          </Label>
          <SimpleSelect
            id="report-branch"
            className="sm:w-48"
            value={branch}
            onChange={setBranch}
            options={[
              { value: "all", label: "Tümü" },
              ...(branches.data ?? []).map((b) => ({ value: String(b.id), label: b.name })),
            ]}
          />
        </div>
        <Button
          variant="outline"
          className="h-11 md:h-10"
          disabled={!valid}
          nativeButton={false}
          render={<a href={`/api/print/reports/monthly.csv?${csvParams.toString()}`} download />}
        >
          <Download aria-hidden />
          CSV İndir
        </Button>
      </div>

      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(r) => r.userId}
        loading={valid && report.isPending}
        error={report.error}
        onRetry={() => void report.refetch()}
        emptyMessage={valid ? "Bu ay teslim edilmiş talep yok." : "Geçerli bir ay seçin."}
      />
      <p className="mt-3 text-[13px] text-muted-foreground">
        Rapor, seçilen ayda teslim edilen talepleri içerir. Baskı = sayfa × kopya.
      </p>
    </div>
  );
}
