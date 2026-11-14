package tr.okul.iletisim.print;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrintRequestHistoryRepository extends JpaRepository<PrintRequestHistory, Long> {

	List<PrintRequestHistory> findByRequestIdOrderByIdAsc(Long requestId);

}
