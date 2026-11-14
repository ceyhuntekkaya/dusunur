package tr.okul.iletisim.core.job;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.common.file.FileStorageService;
import tr.okul.iletisim.common.file.StoredFile;
import tr.okul.iletisim.common.file.StoredFileRepository;

@Component
public class MaintenanceJobs {

	private static final Logger log = LoggerFactory.getLogger(MaintenanceJobs.class);

	private final StoredFileRepository files;
	private final FileStorageService storage;
	private final JdbcClient jdbc;
	private final AppProperties app;

	public MaintenanceJobs(StoredFileRepository files, FileStorageService storage, JdbcClient jdbc, AppProperties app) {
		this.files = files;
		this.storage = storage;
		this.jdbc = jdbc;
		this.app = app;
	}

	@Scheduled(cron = "0 0 3 * * *", zone = "Europe/Istanbul")
	public void deleteOrphanFiles() {
		try {
			List<StoredFile> orphans = files.findByAttachedFalseAndDeletedAtIsNullAndCreatedAtBefore(
					OffsetDateTime.now().minusHours(app.cleanup().orphanFileHours()));
			int deleted = 0;
			for (StoredFile f : orphans) {
				try {
					storage.delete(f);
					deleted++;
				}
				catch (RuntimeException e) {
					log.error("Bağlanmamış dosya silinemedi: {}", f.getId(), e);
				}
			}
			log.info("Bağlanmamış dosya temizliği: {} dosya silindi.", deleted);
		}
		catch (RuntimeException e) {
			log.error("Bağlanmamış dosya temizliği başarısız oldu.", e);
		}
	}

	@Scheduled(cron = "0 10 3 * * *", zone = "Europe/Istanbul")
	public void deleteOldNotifications() {
		try {
			int n = jdbc.sql("DELETE FROM notification WHERE created_at < now() - make_interval(days => :days)")
					.param("days", app.cleanup().notificationRetentionDays()).update();
			log.info("Bildirim temizliği: {} kayıt silindi.", n);
		}
		catch (RuntimeException e) {
			log.error("Bildirim temizliği başarısız oldu.", e);
		}
	}

	@Scheduled(cron = "0 20 3 * * *", zone = "Europe/Istanbul")
	public void deleteExpiredTokens() {
		try {
			int n = jdbc.sql("DELETE FROM auth_token WHERE expires_at < now() - interval '7 days'").update();
			log.info("Şifre bağlantısı temizliği: {} kayıt silindi.", n);
		}
		catch (RuntimeException e) {
			log.error("Şifre bağlantısı temizliği başarısız oldu.", e);
		}
	}

}
