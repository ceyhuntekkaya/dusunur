package tr.okul.iletisim.core.branch;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {

	Optional<Branch> findByNameKey(String nameKey);

	boolean existsByNameKeyAndIdNot(String nameKey, Long id);

	long countByActiveTrue();

}
