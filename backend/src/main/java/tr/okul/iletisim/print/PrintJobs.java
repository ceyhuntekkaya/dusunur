package tr.okul.iletisim.print;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.AppProperties;

@Component
public class PrintJobs {

	private static final Logger log = LoggerFactory.getLogger(PrintJobs.class);

	private final PrintQueries queries;
	private final PrintRequestService service;
	private final AppProperties app;

	public PrintJobs(PrintQueries queries, PrintRequestService service, AppProperties app) {
		this.queries = queries;
		this.service = service;
		this.app = app;
	}

	@Scheduled(cron = "0 0 4 * * *", zone = "Europe/Istanbul")
	public void purgeFiles() {
		OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(app.print().fileRetentionDays());
		List<Long> ids = queries.purgeCandidates(cutoff, 500);
		int purged = 0;
		for (Long id : ids) {
			try {
				service.purgeFile(id);
				purged++;
			}
			catch (RuntimeException e) {
				log.warn("Fotokopi talebi dosyası silinemedi: talep {}", id, e);
			}
		}
		log.info("Fotokopi dosya temizliği: {} dosya silindi.", purged);
	}

}
