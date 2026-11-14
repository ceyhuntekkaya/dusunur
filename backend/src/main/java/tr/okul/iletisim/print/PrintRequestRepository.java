package tr.okul.iletisim.print;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrintRequestRepository extends JpaRepository<PrintRequest, Long> {

	List<PrintRequest> findBySeasonIdAndStatusIn(Long seasonId, Collection<PrintStatus> statuses);

	List<PrintRequest> findByApproverIdAndStatus(Long approverId, PrintStatus status);

}
