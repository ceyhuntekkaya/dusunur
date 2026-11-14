package tr.okul.iletisim.calendar;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CalEntryRepository extends JpaRepository<CalEntry, Long> {

	List<CalEntry> findByCalendarId(Long calendarId);

}
