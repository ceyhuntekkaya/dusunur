package tr.okul.iletisim.dms;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.AppProperties;

@Component
public class DmsJobs {

	private static final Logger log = LoggerFactory.getLogger(DmsJobs.class);

	private final DmsQueries queries;
	private final DmsItemService items;
	private final AppProperties app;

	public DmsJobs(DmsQueries queries, DmsItemService items, AppProperties app) {
		this.queries = queries;
		this.items = items;
		this.app = app;
	}

	@Scheduled(cron = "0 30 4 * * *", zone = "Europe/Istanbul")
	public void purgeTrash() {
		OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(app.dms().trashRetentionDays());
		List<DmsQueries.TrashKey> expired = queries.expiredTrash(cutoff);
		int purged = 0;
		for (DmsQueries.TrashKey key : expired) {
			try {
				items.purgeExpired(key);
				purged++;
			}
			catch (RuntimeException e) {
				log.warn("Çöp kutusu öğesi silinemedi: {} {}", key.type(), key.id(), e);
			}
		}
		log.info("Doküman çöp kutusu temizliği: {} öğe silindi.", purged);
	}

}
