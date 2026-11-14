package tr.okul.iletisim.core.branch;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tr.okul.iletisim.common.util.TextNormalizer;

public final class BranchDtos {

	private BranchDtos() {
	}

	public record DirBranch(long id, String name) {
	}

	public record BranchRow(long id, String name, boolean active, int memberCount) {
	}

	public record BranchRequest(
			@NotNull(message = "Ad 2–100 karakter olmalıdır.") @Size(min = 2, max = 100, message = "Ad 2–100 karakter olmalıdır.") String name) {

		public BranchRequest {
			name = TextNormalizer.clean(name);
		}

	}

}
