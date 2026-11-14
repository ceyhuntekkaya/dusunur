package tr.okul.iletisim.core.group;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffGroupRepository extends JpaRepository<StaffGroup, Long> {

	boolean existsByNameKey(String nameKey);

	boolean existsByNameKeyAndIdNot(String nameKey, Long id);

}
