package tr.okul.iletisim.core.job;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.branch.Branch;
import tr.okul.iletisim.core.branch.BranchRepository;
import tr.okul.iletisim.core.group.GroupDtos.GroupCreateRequest;
import tr.okul.iletisim.core.group.GroupService;
import tr.okul.iletisim.core.season.Season;
import tr.okul.iletisim.core.season.SeasonMembership;
import tr.okul.iletisim.core.season.SeasonMembershipRepository;
import tr.okul.iletisim.core.season.SeasonRepository;
import tr.okul.iletisim.core.season.SeasonStatus;
import tr.okul.iletisim.core.user.AppUser;
import tr.okul.iletisim.core.user.AppUserRepository;
import tr.okul.iletisim.core.user.UserStatus;

@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DevDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

	private static final String MERKEZ = "Merkez Kampüs";
	private static final String KUZEY = "Kuzey Kampüs";

	private record Seed(String email, String first, String last, String branch, String title, String discipline,
			boolean admin, boolean manager, boolean dms, boolean printOperator, String approver, boolean passive) {
	}

	private final AppUserRepository users;
	private final BranchRepository branches;
	private final SeasonRepository seasons;
	private final SeasonMembershipRepository memberships;
	private final GroupService groups;
	private final PasswordEncoder encoder;
	private final AppProperties app;
	private final TransactionTemplate tx;

	public DevDataSeeder(AppUserRepository users, BranchRepository branches, SeasonRepository seasons,
			SeasonMembershipRepository memberships, GroupService groups, PasswordEncoder encoder, AppProperties app,
			TransactionTemplate tx) {
		this.users = users;
		this.branches = branches;
		this.seasons = seasons;
		this.memberships = memberships;
		this.groups = groups;
		this.encoder = encoder;
		this.app = app;
		this.tx = tx;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (users.count() > 0) {
			return;
		}
		tx.executeWithoutResult(status -> seed());
		log.info("Geliştirme verisi yüklendi (şifre: Test1234).");
	}

	private void seed() {
		Map<String, Branch> branchMap = new HashMap<>();
		for (String name : List.of(MERKEZ, KUZEY)) {
			Branch b = new Branch();
			b.setName(name);
			branchMap.put(name, branches.save(b));
		}
		Season season = new Season();
		season.setName("2026-2027");
		season.setStartDate(LocalDate.of(2026, 9, 1));
		season.setEndDate(LocalDate.of(2027, 8, 31));
		season.setStatus(SeasonStatus.ACTIVE);
		season.setActivatedAt(OffsetDateTime.now());
		season = seasons.save(season);

		List<Seed> seeds = List.of(
				new Seed("admin@okul.test", "Sistem", "Yöneticisi", MERKEZ, "BT Sorumlusu", null, true, false, false, false, null, false),
				new Seed("mudur@okul.test", "Ayşe", "Yılmaz", MERKEZ, "Müdür", null, false, true, false, false, null, false),
				new Seed("my.kuzey@okul.test", "Mehmet", "Demir", KUZEY, "Müdür Yardımcısı", null, false, true, false, false, null, false),
				new Seed("zumre.mat@okul.test", "Elif", "Kaya", MERKEZ, "Matematik Zümre Başkanı", "Matematik", false, false, true, false, "mudur@okul.test", false),
				new Seed("ogretmen1@okul.test", "Ali", "Çelik", MERKEZ, "Öğretmen", "Matematik", false, false, false, false, "zumre.mat@okul.test", false),
				new Seed("ogretmen2@okul.test", "Zeynep", "Şahin", KUZEY, "Öğretmen", "Türkçe", false, false, false, false, "my.kuzey@okul.test", false),
				new Seed("ogretmen3@okul.test", "Burak", "Öztürk", MERKEZ, "Öğretmen", "Fen Bilimleri", false, false, false, false, null, false),
				new Seed("fotokopi.merkez@okul.test", "Hasan", "Arslan", MERKEZ, "Teksir Görevlisi", null, false, false, false, true, null, false),
				new Seed("fotokopi.kuzey@okul.test", "Fatma", "Koç", KUZEY, "Teksir Görevlisi", null, false, false, false, true, null, false),
				new Seed("pasif@okul.test", "Pasif", "Personel", null, null, null, false, false, false, false, null, true),
				new Seed("atanmamis@okul.test", "Atanmamış", "Personel", null, null, null, false, false, false, false, null, false));

		String hash = encoder.encode("Test1234");
		Map<String, AppUser> byEmail = new HashMap<>();
		for (Seed s : seeds) {
			AppUser u = new AppUser();
			u.setEmail(s.email());
			u.setFirstName(s.first());
			u.setLastName(s.last());
			u.setDiscipline(s.discipline());
			u.setSystemAdmin(s.admin());
			u.setStatus(s.passive() ? UserStatus.PASSIVE : UserStatus.ACTIVE);
			u.setPasswordHash(hash);
			u.setKvkkVersion(app.kvkk().version());
			u.setKvkkAcceptedAt(OffsetDateTime.now());
			byEmail.put(s.email(), users.save(u));
		}
		for (Seed s : seeds) {
			if (s.branch() == null) {
				continue;
			}
			SeasonMembership m = new SeasonMembership();
			m.setSeasonId(season.getId());
			m.setUserId(byEmail.get(s.email()).getId());
			m.setBranchId(branchMap.get(s.branch()).getId());
			m.setTitle(s.title());
			m.setManager(s.manager());
			m.setCanCreateDmsSpace(s.dms());
			m.setPrintOperator(s.printOperator());
			if (s.approver() != null) {
				m.setPrintApproverId(byEmail.get(s.approver()).getId());
			}
			memberships.save(m);
		}
		memberships.flush();

		AppUser admin = byEmail.get("admin@okul.test");
		UserContext ctx = new UserContext(admin.getId(), admin.getFirstName(), admin.getLastName(), true,
				season.getId(), season.getName(), null, Set.of());
		groups.create(ctx, new GroupCreateRequest("Matematik Zümresi", null,
				List.of(byEmail.get("zumre.mat@okul.test").getId(), byEmail.get("ogretmen1@okul.test").getId())));
		groups.create(ctx, new GroupCreateRequest("Fen Zümresi", null, List.of(byEmail.get("ogretmen3@okul.test").getId())));
		groups.create(ctx, new GroupCreateRequest("Yönetim Kurulu", null,
				List.of(byEmail.get("mudur@okul.test").getId(), byEmail.get("my.kuzey@okul.test").getId())));
	}

}
