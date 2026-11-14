package tr.okul.iletisim.core.season;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Long> {

	Optional<Season> findByStatus(SeasonStatus status);

	boolean existsByName(String name);

	List<Season> findAllByOrderByNameDesc();

	boolean existsByNameAndIdNot(String name, Long id);

	boolean existsByNameGreaterThanAndIdNot(String name, Long id);

}
