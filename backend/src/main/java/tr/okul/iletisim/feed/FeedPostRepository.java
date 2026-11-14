package tr.okul.iletisim.feed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedPostRepository extends JpaRepository<FeedPost, Long> {

	@Query("SELECT count(p) FROM FeedPost p WHERE p.seasonId = :seasonId AND p.pinnedAt IS NOT NULL AND p.deletedAt IS NULL")
	int countPinned(@Param("seasonId") long seasonId);

}
