package tr.okul.iletisim.common.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.security.UserContext;

@Service
public class FileStorageService {

	private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

	private static final Set<String> IMAGE = Set.of("jpg", "jpeg", "png", "webp", "gif");
	private static final Set<String> VIDEO = Set.of("mp4", "mov", "webm");
	private static final Set<String> OFFICE = Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp");
	private static final Set<String> TYPE_CHECK_SKIPPED = Set.of("txt", "csv", "rtf", "rar", "7z", "mp3");
	private static final Set<String> OFFICE_TYPES = Set.of("application/x-tika-ooxml", "application/zip",
			"application/x-tika-msoffice", "application/msword", "application/x-tika-ooxml-protected");

	private static final List<String> COMMON = List.of("jpg", "jpeg", "png", "webp", "gif", "pdf", "doc", "docx",
			"xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "txt", "csv", "zip", "mp4", "mov", "webm");
	private static final List<String> DMS = List.of("jpg", "jpeg", "png", "webp", "gif", "pdf", "doc", "docx", "xls",
			"xlsx", "ppt", "pptx", "odt", "ods", "odp", "txt", "csv", "zip", "mp4", "mov", "webm", "rtf", "rar", "7z",
			"mp3");
	private static final List<String> PRINT = List.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods",
			"odp", "jpg", "jpeg", "png");
	private static final List<String> CALENDAR = List.of("pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png");

	private static final Map<String, String> EXT_TYPES = Map.ofEntries(
			Map.entry("doc", "application/msword"),
			Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
			Map.entry("xls", "application/vnd.ms-excel"),
			Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
			Map.entry("ppt", "application/vnd.ms-powerpoint"),
			Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
			Map.entry("odt", "application/vnd.oasis.opendocument.text"),
			Map.entry("ods", "application/vnd.oasis.opendocument.spreadsheet"),
			Map.entry("odp", "application/vnd.oasis.opendocument.presentation"),
			Map.entry("txt", "text/plain"), Map.entry("csv", "text/csv"), Map.entry("rtf", "application/rtf"),
			Map.entry("zip", "application/zip"), Map.entry("rar", "application/vnd.rar"),
			Map.entry("7z", "application/x-7z-compressed"), Map.entry("mp3", "audio/mpeg"));

	private final StoredFileRepository repository;
	private final AppProperties app;
	private final Tika tika = new Tika();

	public FileStorageService(StoredFileRepository repository, AppProperties app) {
		this.repository = repository;
		this.app = app;
	}

	@Transactional
	public StoredFile store(UserContext me, MultipartFile file, FilePurpose purpose) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(CommonErrorCode.FILE_EMPTY);
		}
		String name = sanitizeName(file.getOriginalFilename());
		String ext = extension(name);
		long size = file.getSize();

		if (size > maxBytes(purpose, null)) {
			throw new BusinessException(CommonErrorCode.FILE_TOO_LARGE, maxMb(purpose, null));
		}
		List<String> allowed = allowedExtensions(purpose);
		if (!allowed.contains(ext)) {
			throw new BusinessException(CommonErrorCode.FILE_TYPE_NOT_ALLOWED, String.join(", ", allowed));
		}
		String kind = IMAGE.contains(ext) ? "image" : VIDEO.contains(ext) ? "video" : "doc";
		if ((purpose == FilePurpose.FEED || purpose == FilePurpose.CHAT) && size > maxBytes(purpose, kind)) {
			throw new BusinessException(CommonErrorCode.FILE_TOO_LARGE, maxMb(purpose, kind));
		}

		String detected;
		try (InputStream in = file.getInputStream()) {
			detected = tika.detect(in);
		}
		catch (IOException e) {
			log.error("Dosya içeriği okunamadı", e);
			throw new BusinessException(CommonErrorCode.FILE_STORAGE_ERROR);
		}
		if (!typeMatches(ext, detected)) {
			throw new BusinessException(CommonErrorCode.FILE_CONTENT_MISMATCH);
		}

		UUID id = UUID.randomUUID();
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		String relative = "%04d/%02d/%s".formatted(now.getYear(), now.getMonthValue(), id);
		Path target = root().resolve(relative);
		Path part = root().resolve(relative + ".part");
		try {
			Files.createDirectories(target.getParent());
			try (InputStream in = file.getInputStream()) {
				Files.copy(in, part, StandardCopyOption.REPLACE_EXISTING);
			}
			Files.move(part, target, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (IOException e) {
			log.error("Dosya diske yazılamadı: {}", target, e);
			deleteQuietly(part);
			throw new BusinessException(CommonErrorCode.FILE_STORAGE_ERROR);
		}

		StoredFile sf = new StoredFile();
		sf.setId(id);
		sf.setOriginalName(name);
		sf.setContentType(resolveContentType(ext, detected));
		sf.setSizeBytes(size);
		sf.setStoragePath(relative);
		sf.setPurpose(purpose);
		sf.setUploadedBy(me.userId());
		try {
			return repository.saveAndFlush(sf);
		}
		catch (RuntimeException e) {
			deleteQuietly(target);
			throw e;
		}
	}

	@Transactional
	public StoredFile attach(UUID fileId, UserContext me, FilePurpose purpose) {
		StoredFile f = fileId == null ? null : repository.findById(fileId).orElse(null);
		if (f == null || f.getDeletedAt() != null || f.isAttached() || f.getPurpose() != purpose
				|| !f.getUploadedBy().equals(me.userId())) {
			throw new BusinessException(CommonErrorCode.FILE_NOT_USABLE);
		}
		f.setAttached(true);
		return repository.save(f);
	}

	@Transactional(readOnly = true)
	public StoredFile get(UUID fileId) {
		return repository.findById(fileId).filter(f -> f.getDeletedAt() == null)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.FILE_NOT_FOUND));
	}

	public ResponseEntity<Resource> serve(StoredFile file, boolean inline) {
		Path path = root().resolve(file.getStoragePath());
		if (file.getDeletedAt() != null || !Files.isReadable(path)) {
			throw new BusinessException(CommonErrorCode.FILE_NOT_FOUND);
		}
		String type = file.getContentType();
		boolean previewable = type.startsWith("image/") || type.startsWith("video/") || type.equals("application/pdf");
		ContentDisposition disposition = (inline && previewable ? ContentDisposition.inline()
				: ContentDisposition.attachment()).filename(file.getOriginalName(), java.nio.charset.StandardCharsets.UTF_8)
				.build();
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(type))
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
				.header("X-Content-Type-Options", "nosniff")
				.cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS).cachePrivate())
				.body(new FileSystemResource(path));
	}

	@Transactional
	public void delete(StoredFile file) {
		deleteQuietly(root().resolve(file.getStoragePath()));
		file.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
		repository.save(file);
	}

	private Path root() {
		return Path.of(app.storage().root()).toAbsolutePath().normalize();
	}

	private static void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		}
		catch (IOException e) {
			log.warn("Dosya silinemedi: {}", path, e);
		}
	}

	private static String sanitizeName(String original) {
		String n = original == null ? "" : original;
		n = n.substring(Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\')) + 1);
		n = n.replaceAll("\\p{Cntrl}", "").trim();
		if (n.length() > 255) {
			n = n.substring(0, 255);
		}
		return n.isEmpty() ? "dosya" : n;
	}

	private static String extension(String name) {
		int dot = name.lastIndexOf('.');
		return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
	}

	private static List<String> allowedExtensions(FilePurpose purpose) {
		return switch (purpose) {
			case FEED, CHAT -> COMMON;
			case PRINT -> PRINT;
			case DMS -> DMS;
			case CALENDAR -> CALENDAR;
		};
	}

	private int maxMb(FilePurpose purpose, String kind) {
		AppProperties.Files f = app.files();
		return switch (purpose) {
			case FEED, CHAT -> kind == null ? f.videoMaxMb()
					: kind.equals("image") ? f.imageMaxMb() : kind.equals("video") ? f.videoMaxMb() : f.documentMaxMb();
			case PRINT -> f.documentMaxMb();
			case DMS -> f.dmsMaxMb();
			case CALENDAR -> f.calendarMaxMb();
		};
	}

	private long maxBytes(FilePurpose purpose, String kind) {
		return maxMb(purpose, kind) * 1024L * 1024L;
	}

	private static boolean typeMatches(String ext, String detected) {
		if (TYPE_CHECK_SKIPPED.contains(ext)) {
			return true;
		}
		if (IMAGE.contains(ext)) {
			return detected.startsWith("image/");
		}
		if (VIDEO.contains(ext)) {
			return detected.startsWith("video/");
		}
		if (ext.equals("pdf")) {
			return detected.equals("application/pdf");
		}
		if (ext.equals("zip")) {
			return detected.equals("application/zip") || detected.equals("application/x-tika-ooxml");
		}
		if (OFFICE.contains(ext)) {
			return OFFICE_TYPES.contains(detected) || detected.startsWith("application/vnd.");
		}
		return true;
	}

	private static String resolveContentType(String ext, String detected) {
		String type = detected.startsWith("image/") || detected.startsWith("video/") || detected.equals("application/pdf")
				? detected : EXT_TYPES.getOrDefault(ext, detected);
		return type.length() > 150 ? "application/octet-stream" : type;
	}

}
