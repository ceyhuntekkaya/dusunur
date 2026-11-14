package tr.okul.iletisim.core.user;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.audit.CoreAuditAction;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.util.Csv;
import tr.okul.iletisim.common.util.PhoneNormalizer;
import tr.okul.iletisim.common.util.TextNormalizer;
import tr.okul.iletisim.core.CoreErrorCode;
import tr.okul.iletisim.core.branch.Branch;
import tr.okul.iletisim.core.branch.BranchRepository;
import tr.okul.iletisim.core.season.MembershipService;
import tr.okul.iletisim.core.season.Season;
import tr.okul.iletisim.core.season.SeasonMembership;
import tr.okul.iletisim.core.season.SeasonMembershipRepository;
import tr.okul.iletisim.core.season.SeasonStatus;
import tr.okul.iletisim.core.user.UserDtos.ImportResponse;

@Service
public class UserImportService {

	public static final String TEMPLATE = "ad;soyad;eposta;telefon;brans;sube;unvan\r\n"
			+ "Ayşe;Yılmaz;ayse.yilmaz@okul.k12.tr;05321234567;Matematik;Merkez Kampüs;Öğretmen\r\n";

	private static final long MAX_BYTES = 2L * 1024 * 1024;
	private static final int MAX_ROWS = 1000;
	private static final int MAX_ERRORS = 100;
	private static final List<String> HEADER = List.of("ad", "soyad", "eposta", "telefon", "brans", "sube", "unvan");
	private static final Pattern NAME = Pattern.compile(UserDtos.NAME_REGEX);
	private static final Pattern EMAIL = Pattern.compile(UserDtos.EMAIL_REGEX);

	private record Row(int line, String firstName, String lastName, String email, String phone, String discipline,
			Long branchId, String title) {
	}

	private final MembershipService membershipService;
	private final AppUserRepository users;
	private final SeasonMembershipRepository memberships;
	private final BranchRepository branches;
	private final AuditService audit;
	private final TransactionTemplate tx;
	private final UserImportMailJob mailJob;

	public UserImportService(MembershipService membershipService, AppUserRepository users,
			SeasonMembershipRepository memberships, BranchRepository branches, AuditService audit,
			TransactionTemplate tx, UserImportMailJob mailJob) {
		this.membershipService = membershipService;
		this.users = users;
		this.memberships = memberships;
		this.branches = branches;
		this.audit = audit;
		this.tx = tx;
		this.mailJob = mailJob;
	}

	public ImportResponse importCsv(UserContext me, MultipartFile file, long seasonId) {
		String name = file == null || file.getOriginalFilename() == null ? ""
				: file.getOriginalFilename().toLowerCase(Locale.ROOT);
		if (file == null || file.isEmpty() || !name.endsWith(".csv") || file.getSize() > MAX_BYTES) {
			throw new BusinessException(CoreErrorCode.IMPORT_FILE_INVALID);
		}
		Season season = membershipService.findSeason(seasonId);
		if (season.getStatus() == SeasonStatus.CLOSED) {
			throw new BusinessException(CoreErrorCode.SEASON_CLOSED_READ_ONLY);
		}
		byte[] bytes;
		try {
			bytes = file.getBytes();
		}
		catch (java.io.IOException e) {
			throw new BusinessException(CoreErrorCode.IMPORT_FILE_INVALID);
		}
		String content = decode(bytes);
		if (content.startsWith("﻿")) {
			content = content.substring(1);
		}
		List<List<String>> table = Csv.parse(content, Csv.detectDelimiter(content));
		if (table.isEmpty() || !headerMatches(table.get(0))) {
			throw new BusinessException(CoreErrorCode.IMPORT_HEADER_INVALID);
		}
		List<Integer> dataLines = new ArrayList<>();
		for (int i = 1; i < table.size(); i++) {
			if (table.get(i).stream().anyMatch(c -> !c.isBlank())) {
				dataLines.add(i);
			}
		}
		if (dataLines.isEmpty()) {
			throw new BusinessException(CoreErrorCode.IMPORT_EMPTY);
		}
		if (dataLines.size() > MAX_ROWS) {
			throw new BusinessException(CoreErrorCode.IMPORT_TOO_MANY_ROWS);
		}

		Map<String, Branch> byKey = new java.util.HashMap<>();
		branches.findAll().stream().filter(Branch::isActive).forEach(b -> byKey.put(b.getNameKey(), b));
		List<String> errors = new ArrayList<>();
		List<Row> rows = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (int i : dataLines) {
			List<String> c = table.get(i);
			int line = i + 1;
			List<String> rowErrors = new ArrayList<>();
			String first = TextNormalizer.clean(cell(c, 0));
			String last = TextNormalizer.clean(cell(c, 1));
			String email = TextNormalizer.lowerEmail(cell(c, 2));
			String phone = cell(c, 3).trim();
			String discipline = TextNormalizer.clean(cell(c, 4));
			String branch = TextNormalizer.clean(cell(c, 5));
			String title = TextNormalizer.clean(cell(c, 6));
			if (first.length() < 2 || first.length() > 50 || !NAME.matcher(first).matches()) {
				rowErrors.add(UserDtos.FIRST_NAME_MESSAGE);
			}
			if (last.length() < 2 || last.length() > 50 || !NAME.matcher(last).matches()) {
				rowErrors.add(UserDtos.LAST_NAME_MESSAGE);
			}
			if (email.length() > 254 || !EMAIL.matcher(email).matches()) {
				rowErrors.add(UserDtos.EMAIL_MESSAGE);
			}
			else if (!seen.add(email)) {
				rowErrors.add("Bu e-posta dosyada birden fazla kez geçiyor.");
			}
			else if (users.existsByEmail(email)) {
				rowErrors.add("Bu e-posta ile kayıtlı personel var.");
			}
			if (!PhoneNormalizer.isValid(phone)) {
				rowErrors.add("Telefon numarası 05XX XXX XX XX biçiminde olmalıdır.");
			}
			if (discipline.length() > 100) {
				rowErrors.add(UserDtos.DISCIPLINE_MESSAGE);
			}
			if (title.length() > 100) {
				rowErrors.add(UserDtos.TITLE_MESSAGE);
			}
			Branch b = byKey.get(TextNormalizer.fold(branch));
			if (b == null) {
				rowErrors.add("Şube bulunamadı: " + branch);
			}
			if (rowErrors.isEmpty()) {
				rows.add(new Row(line, first, last, email, PhoneNormalizer.normalize(phone),
						discipline.isEmpty() ? null : discipline, b.getId(), title.isEmpty() ? null : title));
			}
			else {
				rowErrors.forEach(m -> errors.add(line + ". satır: " + m));
			}
		}
		if (!errors.isEmpty()) {
			List<String> details = new ArrayList<>(errors.subList(0, Math.min(MAX_ERRORS, errors.size())));
			if (errors.size() > MAX_ERRORS) {
				details.add("… ve " + (errors.size() - MAX_ERRORS) + " hata daha.");
			}
			throw BusinessException.details(CoreErrorCode.IMPORT_INVALID, details, new Object[0]);
		}

		List<Long> created = tx.execute(status -> persist(me, season, rows));
		mailJob.run(me.userId(), created);
		return new ImportResponse(created.size());
	}

	private List<Long> persist(UserContext me, Season season, List<Row> rows) {
		List<Long> ids = new ArrayList<>();
		for (Row r : rows) {
			AppUser u = new AppUser();
			u.setFirstName(r.firstName());
			u.setLastName(r.lastName());
			u.setEmail(r.email());
			u.setPhone(r.phone());
			u.setDiscipline(r.discipline());
			u = users.saveAndFlush(u);
			SeasonMembership m = new SeasonMembership();
			m.setSeasonId(season.getId());
			m.setUserId(u.getId());
			m.setBranchId(r.branchId());
			m.setTitle(r.title());
			memberships.save(m);
			ids.add(u.getId());
		}
		memberships.flush();
		audit.log(me.userId(), CoreAuditAction.USER_IMPORTED, "USER", null,
				Map.of("count", ids.size(), "seasonId", season.getId()));
		return ids;
	}

	private static boolean headerMatches(List<String> header) {
		List<String> cells = new ArrayList<>(header.stream().map(h -> TextNormalizer.fold(h)).toList());
		while (!cells.isEmpty() && cells.get(cells.size() - 1).isEmpty()) {
			cells.remove(cells.size() - 1);
		}
		return cells.equals(HEADER);
	}

	private static String cell(List<String> row, int i) {
		return i < row.size() && row.get(i) != null ? row.get(i) : "";
	}

	private static String decode(byte[] bytes) {
		try {
			return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
		}
		catch (CharacterCodingException e) {
			return new String(bytes, Charset.forName("windows-1254"));
		}
	}

}
