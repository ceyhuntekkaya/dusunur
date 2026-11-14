package tr.okul.iletisim.dms;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tr.okul.iletisim.core.directory.TargetType;

public interface DmsSpaceGrantRepository extends JpaRepository<DmsSpaceGrant, Long> {

	Optional<DmsSpaceGrant> findByIdAndSpaceId(Long id, Long spaceId);

	Optional<DmsSpaceGrant> findBySpaceIdAndSubjectTypeAndSubjectId(Long spaceId, TargetType subjectType, Long subjectId);

	Optional<DmsSpaceGrant> findBySpaceIdAndSubjectTypeAndSubjectIdIsNull(Long spaceId, TargetType subjectType);

	long countBySpaceId(Long spaceId);

}
