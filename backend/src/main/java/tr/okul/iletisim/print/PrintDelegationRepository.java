package tr.okul.iletisim.print;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrintDelegationRepository extends JpaRepository<PrintDelegation, Long> {

	List<PrintDelegation> findByApproverIdAndEndDateGreaterThanEqualOrderByStartDateAsc(Long approverId, LocalDate date);

	Optional<PrintDelegation> findFirstByApproverIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long approverId,
			LocalDate start, LocalDate end);

	boolean existsByApproverIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long approverId, LocalDate end,
			LocalDate start);

}
