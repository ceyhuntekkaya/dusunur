package tr.okul.iletisim.core.user;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.web.PageResponse;
import tr.okul.iletisim.core.directory.DirectoryDtos.DirUser;
import tr.okul.iletisim.core.user.UserDtos.AdminUserRow;
import tr.okul.iletisim.core.user.UserDtos.CreateUserResponse;
import tr.okul.iletisim.core.user.UserDtos.DeactivateRequest;
import tr.okul.iletisim.core.user.UserDtos.DeactivationCheck;
import tr.okul.iletisim.core.user.UserDtos.ImportResponse;
import tr.okul.iletisim.core.user.UserDtos.UserInput;

@RestController
public class UserController {

	private final UserService service;
	private final UserQueries queries;
	private final UserImportService importService;

	public UserController(UserService service, UserQueries queries, UserImportService importService) {
		this.service = service;
		this.queries = queries;
		this.importService = importService;
	}

	@GetMapping("/api/admin/users")
	public PageResponse<AdminUserRow> list(@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "ACTIVE") String status, @RequestParam(required = false) Long branchId,
			@RequestParam(required = false) Boolean assigned, @RequestParam(required = false) Boolean passwordSet,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
		String st = status.toUpperCase(java.util.Locale.ROOT);
		if (!st.equals("ACTIVE") && !st.equals("PASSIVE")) {
			st = "ALL";
		}
		return queries.page(q, st, branchId, assigned, passwordSet, page, size);
	}

	@GetMapping("/api/admin/users/lookup")
	public List<DirUser> lookup(@RequestParam(defaultValue = "") String q,
			@RequestParam(required = false) Long seasonId) {
		return queries.lookup(q, seasonId);
	}

	@GetMapping("/api/admin/titles")
	public List<String> titles() {
		return queries.titles();
	}

	@GetMapping("/api/admin/users/import-template")
	public ResponseEntity<byte[]> importTemplate() {
		byte[] body = ("﻿" + UserImportService.TEMPLATE).getBytes(StandardCharsets.UTF_8);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("personel-sablon.csv").build().toString())
				.body(body);
	}

	@PostMapping("/api/admin/users/import")
	public ImportResponse importCsv(UserContext me, @RequestParam("file") MultipartFile file,
			@RequestParam("seasonId") long seasonId) {
		return importService.importCsv(me, file, seasonId);
	}

	@GetMapping("/api/admin/users/{id}")
	public AdminUserRow get(@PathVariable long id) {
		return queries.row(id);
	}

	@PostMapping("/api/admin/users")
	@ResponseStatus(HttpStatus.CREATED)
	public CreateUserResponse create(UserContext me, @Valid @RequestBody UserInput request) {
		return service.create(me, request);
	}

	@PutMapping("/api/admin/users/{id}")
	public AdminUserRow update(UserContext me, @PathVariable long id, @Valid @RequestBody UserInput request) {
		return service.update(me, id, request);
	}

	@PostMapping("/api/admin/users/{id}/send-setup-link")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void sendSetupLink(UserContext me, @PathVariable long id) {
		service.sendSetupLink(me, id);
	}

	@GetMapping("/api/admin/users/{id}/deactivation-check")
	public DeactivationCheck deactivationCheck(@PathVariable long id) {
		return service.deactivationCheck(id);
	}

	@PostMapping("/api/admin/users/{id}/deactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(UserContext me, @PathVariable long id, @RequestBody(required = false) DeactivateRequest request) {
		service.deactivate(me, id, request == null ? null : request.successorUserId());
	}

	@PostMapping("/api/admin/users/{id}/reactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void reactivate(UserContext me, @PathVariable long id) {
		service.reactivate(me, id);
	}

}
