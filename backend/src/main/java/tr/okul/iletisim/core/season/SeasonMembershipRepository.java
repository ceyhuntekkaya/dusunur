package tr.okul.iletisim.core.season;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonMembershipRepository extends JpaRepository<SeasonMembership, Long> {

	Optional<SeasonMembership> findBySeasonIdAndUserId(Long seasonId, Long userId);

	List<SeasonMembership> findBySeasonId(Long seasonId);

	long countBySeasonId(Long seasonId);

	long countBySeasonIdAndPrintApproverId(Long seasonId, Long printApproverId);

}
