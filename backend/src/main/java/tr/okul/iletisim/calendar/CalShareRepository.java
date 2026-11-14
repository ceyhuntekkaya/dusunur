package tr.okul.iletisim.calendar;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tr.okul.iletisim.core.directory.TargetType;

public interface CalShareRepository extends JpaRepository<CalShare, Long> {

	List<CalShare> findByCalendarId(Long calendarId);

	long countByCalendarId(Long calendarId);

	Optional<CalShare> findByCalendarIdAndSubjectTypeAndSubjectId(Long calendarId, TargetType subjectType, Long subjectId);

	Optional<CalShare> findByIdAndCalendarId(Long id, Long calendarId);

}
