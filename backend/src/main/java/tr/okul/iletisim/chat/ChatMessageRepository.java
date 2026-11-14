package tr.okul.iletisim.chat;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	Optional<ChatMessage> findBySenderIdAndClientId(Long senderId, UUID clientId);

}
