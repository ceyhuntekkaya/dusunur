package tr.okul.iletisim.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageAttachmentRepository extends JpaRepository<ChatMessageAttachment, ChatMessageAttachmentId> {

	List<ChatMessageAttachment> findByMessageId(Long messageId);

	void deleteByMessageId(Long messageId);

}
