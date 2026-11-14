package tr.okul.iletisim.core.directory;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import tr.okul.iletisim.core.branch.BranchDtos.DirBranch;

public final class DirectoryDtos {

	private DirectoryDtos() {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DirUser(long id, String fullName, String title, String branchName, String discipline) {
	}

	public record DirGroup(long id, String name, int memberCount) {
	}

	public record SearchResponse(List<DirUser> users, List<DirGroup> groups, List<DirBranch> branches) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record TargetRef(TargetType type, Long id) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record TargetView(TargetType type, Long id, String name) {
	}

}
