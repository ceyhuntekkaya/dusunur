package tr.okul.iletisim.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMemberRepository extends JpaRepository<ChatMember, ChatMemberId> {

	List<ChatMember> findByConversationId(Long conversationId);

	List<ChatMember> findByUserId(Long userId);

}
