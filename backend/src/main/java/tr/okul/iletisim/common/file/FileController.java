package tr.okul.iletisim.common.file;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import tr.okul.iletisim.common.file.FileDtos.FileResponse;
import tr.okul.iletisim.common.security.UserContext;

@RestController
@RequestMapping("/api/files")
public class FileController {

	private final FileStorageService files;

	public FileController(FileStorageService files) {
		this.files = files;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public FileResponse upload(UserContext me, @RequestParam("file") MultipartFile file,
			@RequestParam("purpose") FilePurpose purpose) {
		return FileResponse.from(files.store(me, file, purpose));
	}

}
