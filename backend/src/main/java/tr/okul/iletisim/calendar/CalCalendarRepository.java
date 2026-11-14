package tr.okul.iletisim.calendar;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CalCalendarRepository extends JpaRepository<CalCalendar, Long> {

	long countByTypeAndOwnerId(CalendarType type, Long ownerId);

	boolean existsByTypeAndNameKey(CalendarType type, String nameKey);

	boolean existsByTypeAndNameKeyAndIdNot(CalendarType type, String nameKey, Long id);

	java.util.Optional<CalCalendar> findByStaffGroupId(Long staffGroupId);

	List<CalCalendar> findByOwnerIdAndType(Long ownerId, CalendarType type);

}
