package tr.okul.iletisim.chat;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

	Optional<ChatConversation> findByStaffGroupId(Long staffGroupId);

}
