package tr.okul.iletisim.dms;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DmsFileRepository extends JpaRepository<DmsFile, Long> {

	boolean existsBySpaceId(Long spaceId);

}
