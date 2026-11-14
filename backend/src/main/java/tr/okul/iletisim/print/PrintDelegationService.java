package tr.okul.iletisim.print;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.print.PrintDtos.CreateDelegationRequest;
import tr.okul.iletisim.print.PrintDtos.DelegationDto;
import tr.okul.iletisim.print.PrintDtos.PersonRef;

@Service
public class PrintDelegationService {

	private final PrintDelegationRepository delegations;
	private final PrintQueries queries;
	private final AuditService audit;
	private final JdbcClient jdbc;
	private final AppProperties app;

	public PrintDelegationService(PrintDelegationRepository delegations, PrintQueries queries, AuditService audit,
			JdbcClient jdbc, AppProperties app) {
		this.delegations = delegations;
		this.queries = queries;
		this.audit = audit;
		this.jdbc = jdbc;
		this.app = app;
	}

	@Transactional(readOnly = true)
	public List<DelegationDto> list(UserContext me) {
		long season = me.requireMember();
		LocalDate today = today();
		List<PrintDelegation> rows = delegations
				.findByApproverIdAndEndDateGreaterThanEqualOrderByStartDateAsc(me.userId(), today);
		Map<Long, PersonRef> persons = queries.persons(rows.stream().map(PrintDelegation::getDelegateId).toList(),
				season);
		return rows.stream().map(d -> dto(d, persons.get(d.getDelegateId()), today)).toList();
	}

	@Transactional
	public DelegationDto create(UserContext me, CreateDelegationRequest req) {
		long season = me.requireMember();
		LocalDate today = today();
		long delegateId = req.delegateId();
		if (delegateId == me.userId() || !queries.isActiveSeasonMember(season, delegateId)) {
			throw BusinessException.field(PrintErrorCode.PRINT_DELEGATE_INVALID, "delegateId");
		}
		LocalDate start = req.startDate();
		LocalDate end = req.endDate();
		if (start.isBefore(today) || end.isBefore(start) || ChronoUnit.DAYS.between(start, end) + 1 > app.print().maxDelegationDays()) {
			throw BusinessException.field(PrintErrorCode.PRINT_DELEGATION_DATES_INVALID, "endDate");
		}
		if (delegations.existsByApproverIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(me.userId(), end,
				start)) {
			throw BusinessException.field(PrintErrorCode.PRINT_DELEGATION_OVERLAP, "startDate");
		}
		PrintDelegation d = new PrintDelegation();
		d.setApproverId(me.userId());
		d.setDelegateId(delegateId);
		d.setStartDate(start);
		d.setEndDate(end);
		d = delegations.saveAndFlush(d);
		audit.log(me.userId(), PrintAuditAction.PRINT_DELEGATION_CREATED, "PRINT_DELEGATION", d.getId(),
				Map.of("delegateId", delegateId, "startDate", start.toString(), "endDate", end.toString()));
		return dto(d, queries.person(delegateId, season), today);
	}

	@Transactional
	public void delete(UserContext me, long id) {
		me.requireMember();
		PrintDelegation d = delegations.findById(id).filter(x -> x.getApproverId() == me.userId())
				.orElseThrow(() -> new BusinessException(PrintErrorCode.PRINT_DELEGATION_NOT_FOUND));
		delegations.delete(d);
		audit.log(me.userId(), PrintAuditAction.PRINT_DELEGATION_DELETED, "PRINT_DELEGATION", id,
				Map.of("delegateId", d.getDelegateId(), "startDate", d.getStartDate().toString(), "endDate",
						d.getEndDate().toString()));
	}

	@Transactional
	public void deleteCurrentFor(long userId) {
		jdbc.sql("""
				DELETE FROM print_delegation
				WHERE (approver_id = :uid OR delegate_id = :uid) AND end_date >= :today
				""").param("uid", userId).param("today", today()).update();
	}

	private static LocalDate today() {
		return LocalDate.now(PrintQueries.ISTANBUL);
	}

	private static DelegationDto dto(PrintDelegation d, PersonRef delegate, LocalDate today) {
		return new DelegationDto(d.getId(), delegate, d.getStartDate(), d.getEndDate(),
				!d.getStartDate().isAfter(today) && !d.getEndDate().isBefore(today));
	}

}
