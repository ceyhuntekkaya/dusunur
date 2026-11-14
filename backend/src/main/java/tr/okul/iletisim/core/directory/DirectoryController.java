package tr.okul.iletisim.core.directory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.branch.BranchDtos.DirBranch;
import tr.okul.iletisim.core.directory.DirectoryDtos.DirGroup;
import tr.okul.iletisim.core.directory.DirectoryDtos.DirUser;
import tr.okul.iletisim.core.directory.DirectoryDtos.SearchResponse;

@RestController
public class DirectoryController {

	private final DirectoryQueries queries;

	public DirectoryController(DirectoryQueries queries) {
		this.queries = queries;
	}

	@GetMapping("/api/directory/search")
	public SearchResponse search(UserContext me, @RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "user,group,branch") String kinds,
			@RequestParam(defaultValue = "mine") String groupScope, @RequestParam(defaultValue = "20") int limit) {
		long seasonId = me.requireMember();
		Set<String> wanted = Arrays.stream(kinds.split(",")).map(s -> s.trim().toLowerCase(Locale.ROOT))
				.collect(Collectors.toSet());
		int max = Math.max(1, Math.min(limit, 50));
		List<DirUser> users = wanted.contains("user") ? queries.users(seasonId, q, max) : List.of();
		List<DirGroup> groups = wanted.contains("group")
				? queries.groups(me.userId(), !"all".equalsIgnoreCase(groupScope), q, max) : List.of();
		List<DirBranch> branches = wanted.contains("branch") ? queries.branches(q, max) : List.of();
		return new SearchResponse(users, groups, branches);
	}

}
