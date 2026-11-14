package tr.okul.iletisim.core.group;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class StaffGroupMemberId implements Serializable {

	private Long groupId;
	private Long userId;

	public StaffGroupMemberId(Long groupId, Long userId) {
		this.groupId = groupId;
		this.userId = userId;
	}

}
