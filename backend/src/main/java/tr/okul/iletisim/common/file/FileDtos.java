package tr.okul.iletisim.common.file;

import java.util.UUID;

public final class FileDtos {

	private FileDtos() {
	}

	public record FileResponse(UUID id, String originalName, String contentType, long sizeBytes) {

		public static FileResponse from(StoredFile f) {
			return new FileResponse(f.getId(), f.getOriginalName(), f.getContentType(), f.getSizeBytes());
		}

	}

}
