package tr.okul.iletisim;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tr.okul.iletisim.core.directory.AudienceResolver;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetRef;
import tr.okul.iletisim.core.directory.TargetType;

class AudienceResolverTest extends IntegrationTest {

	@Autowired
	AudienceResolver resolver;

	@Test
	void tc5_yalnizcaAktifVeYilaAtanmisKisilerDoner() {
		long creator = data.admin();
		long branchA = data.branch("Merkez");
		long branchB = data.branch("Kampus");
		long season = data.season("2026-2027", "ACTIVE");
		long direct = data.user("Dogrudan");
		long inBranch = data.user("SubeUyesi");
		long groupMember = data.user("GrupUyesi");
		long passiveGroupMember = data.user("PasifUye", "pasifuye@okul.test", "PASSIVE", false);
		long unassignedGroupMember = data.user("Atanmamis");
		long outsider = data.user("Disarida");
		data.member(season, direct, branchB, null);
		data.member(season, inBranch, branchA, null);
		data.member(season, groupMember, branchB, null);
		data.member(season, passiveGroupMember, branchB, null);
		data.member(season, outsider, branchB, null);
		long group = data.group("Zumre", creator, groupMember, passiveGroupMember, unassignedGroupMember);

		Set<Long> result = resolver.resolveUserIds(season, List.of(new TargetRef(TargetType.USER, direct),
				new TargetRef(TargetType.GROUP, group), new TargetRef(TargetType.BRANCH, branchA)));

		assertThat(result).containsExactlyInAnyOrder(direct, inBranch, groupMember);
	}

}
