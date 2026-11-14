package tr.okul.iletisim.core.group;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffGroupMemberRepository extends JpaRepository<StaffGroupMember, StaffGroupMemberId> {

	List<StaffGroupMember> findByGroupId(Long groupId);

	List<StaffGroupMember> findByUserId(Long userId);

	void deleteByUserId(Long userId);

}
