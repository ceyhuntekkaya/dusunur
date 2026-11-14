package tr.okul.iletisim.core.group;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "staff_group_member")
@IdClass(StaffGroupMemberId.class)
@Getter
@Setter
@NoArgsConstructor
public class StaffGroupMember {

	@Id
	@Column(name = "group_id")
	private Long groupId;

	@Id
	@Column(name = "user_id")
	private Long userId;

	@CreationTimestamp
	@Column(name = "added_at", updatable = false)
	private OffsetDateTime addedAt;

	public StaffGroupMember(Long groupId, Long userId) {
		this.groupId = groupId;
		this.userId = userId;
	}

}
