package tr.okul.iletisim.dms;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DmsFolderRepository extends JpaRepository<DmsFolder, Long> {

	boolean existsBySpaceId(Long spaceId);

}
