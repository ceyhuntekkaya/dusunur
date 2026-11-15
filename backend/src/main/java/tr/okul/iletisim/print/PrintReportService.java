package tr.okul.iletisim.print;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.util.Csv;
import tr.okul.iletisim.print.PrintDtos.MonthlyReport;
import tr.okul.iletisim.print.PrintDtos.ReportRow;

@Service
public class PrintReportService {

	private static final Pattern MONTH = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

	private final PrintQueries queries;

	public PrintReportService(PrintQueries queries) {
		this.queries = queries;
	}

	@Transactional(readOnly = true)
	public MonthlyReport monthly(UserContext me, String month, Long branchId) {
		me.requireMember();
		if (!me.isManager()) {
			throw new BusinessException(PrintErrorCode.PRINT_REPORT_FORBIDDEN);
		}
		if (month == null || !MONTH.matcher(month).matches()) {
			throw new BusinessException(PrintErrorCode.PRINT_REPORT_MONTH_INVALID);
		}
		LocalDate first = LocalDate.parse(month + "-01");
		return queries.report(month, first, branchId);
	}

	@Transactional(readOnly = true)
	public byte[] monthlyCsv(UserContext me, String month, Long branchId) {
		MonthlyReport report = monthly(me, month, branchId);
		StringBuilder sb = new StringBuilder();
		sb.append("Ad Soyad;Şube;Talep Sayısı;Siyah-Beyaz Sayfa;Renkli Sayfa;Toplam Sayfa\r\n");
		for (ReportRow r : report.rows()) {
			sb.append(Csv.escape(safe(r.fullName()), ';')).append(';').append(Csv.escape(safe(r.branchName()), ';')).append(';')
					.append(r.requestCount()).append(';').append(r.bwPages()).append(';').append(r.colorPages())
					.append(';').append(r.totalPages()).append("\r\n");
		}
		var t = report.totals();
		sb.append("TOPLAM;;").append(t.requestCount()).append(';').append(t.bwPages()).append(';')
				.append(t.colorPages()).append(';').append(t.totalPages()).append("\r\n");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.writeBytes(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
		out.writeBytes(sb.toString().getBytes(StandardCharsets.UTF_8));
		return out.toByteArray();
	}

	private static String safe(String v) {
		if (v != null && !v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
			return "'" + v;
		}
		return v;
	}

}
