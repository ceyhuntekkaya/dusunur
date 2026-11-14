package tr.okul.iletisim.core.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

	Optional<AuthToken> findByTokenHash(String tokenHash);

	List<AuthToken> findByUserIdAndUsedAtIsNull(Long userId);

	long countByUserIdAndPurposeAndCreatedAtAfter(Long userId, TokenPurpose purpose, OffsetDateTime after);

}
