package tr.okul.iletisim.calendar;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import tr.okul.iletisim.calendar.CalendarDtos.CalShareRow;
import tr.okul.iletisim.calendar.CalendarDtos.CalendarDto;
import tr.okul.iletisim.calendar.CalendarDtos.CalendarRequest;
import tr.okul.iletisim.calendar.CalendarDtos.EntryDto;
import tr.okul.iletisim.calendar.CalendarDtos.EntryInput;
import tr.okul.iletisim.calendar.CalendarDtos.ShareCreateRequest;
import tr.okul.iletisim.calendar.CalendarDtos.ShareResult;
import tr.okul.iletisim.calendar.CalendarDtos.ShareUpdateRequest;
import tr.okul.iletisim.common.file.FileStorageService;
import tr.okul.iletisim.common.file.StoredFile;
import tr.okul.iletisim.common.security.UserContext;

@RestController
public class CalendarController {

	private final CalendarService calendars;
	private final CalendarEntryService entries;
	private final FileStorageService storage;

	public CalendarController(CalendarService calendars, CalendarEntryService entries, FileStorageService storage) {
		this.calendars = calendars;
		this.entries = entries;
		this.storage = storage;
	}

	@GetMapping("/api/calendar/calendars")
	public List<CalendarDto> calendars(UserContext me) {
		return calendars.list(me);
	}

	@PostMapping("/api/calendar/calendars")
	@ResponseStatus(HttpStatus.CREATED)
	public CalendarDto createCalendar(UserContext me, @Valid @RequestBody CalendarRequest request) {
		return calendars.createPersonal(me, request);
	}

	@PostMapping("/api/calendar/resources")
	@ResponseStatus(HttpStatus.CREATED)
	public CalendarDto createResource(UserContext me, @Valid @RequestBody CalendarRequest request) {
		return calendars.createResource(me, request);
	}

	@PutMapping("/api/calendar/calendars/{id}")
	public CalendarDto updateCalendar(UserContext me, @PathVariable long id, @Valid @RequestBody CalendarRequest request) {
		return calendars.update(me, id, request);
	}

	@DeleteMapping("/api/calendar/calendars/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteCalendar(UserContext me, @PathVariable long id) {
		calendars.delete(me, id);
	}

	@GetMapping("/api/calendar/calendars/{id}/shares")
	public List<CalShareRow> shares(UserContext me, @PathVariable long id) {
		return calendars.shares(me, id);
	}

	@PostMapping("/api/calendar/calendars/{id}/shares")
	public ResponseEntity<CalShareRow> addShare(UserContext me, @PathVariable long id,
			@Valid @RequestBody ShareCreateRequest request) {
		ShareResult result = calendars.addShare(me, id, request);
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.row());
	}

	@PutMapping("/api/calendar/calendars/{id}/shares/{sid}")
	public CalShareRow updateShare(UserContext me, @PathVariable long id, @PathVariable long sid,
			@Valid @RequestBody ShareUpdateRequest request) {
		return calendars.updateShare(me, id, sid, request);
	}

	@DeleteMapping("/api/calendar/calendars/{id}/shares/{sid}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteShare(UserContext me, @PathVariable long id, @PathVariable long sid) {
		calendars.deleteShare(me, id, sid);
	}

	@GetMapping("/api/calendar/entries")
	public List<EntryDto> entries(UserContext me,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(required = false) List<Long> calendarIds) {
		return entries.list(me, from, to, calendarIds);
	}

	@GetMapping("/api/calendar/entries/{id}")
	public EntryDto entry(UserContext me, @PathVariable long id) {
		return entries.get(me, id);
	}

	@PostMapping("/api/calendar/entries")
	@ResponseStatus(HttpStatus.CREATED)
	public EntryDto createEntry(UserContext me, @Valid @RequestBody EntryInput request) {
		return entries.create(me, request);
	}

	@PutMapping("/api/calendar/entries/{id}")
	public EntryDto updateEntry(UserContext me, @PathVariable long id, @Valid @RequestBody EntryInput request) {
		return entries.update(me, id, request);
	}

	@DeleteMapping("/api/calendar/entries/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteEntry(UserContext me, @PathVariable long id) {
		entries.delete(me, id);
	}

	@GetMapping("/api/calendar/entries/{id}/file")
	public ResponseEntity<Resource> entryFile(UserContext me, @PathVariable long id,
			@RequestParam(defaultValue = "false") boolean download) {
		StoredFile file = entries.file(me, id);
		return storage.serve(file, !download);
	}

}
