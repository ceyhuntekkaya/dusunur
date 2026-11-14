package tr.okul.iletisim.dms;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DmsSpaceRepository extends JpaRepository<DmsSpace, Long> {

	boolean existsByNameKey(String nameKey);

	boolean existsByNameKeyAndIdNot(String nameKey, Long id);

	List<DmsSpace> findByOwnerId(Long ownerId);

}
