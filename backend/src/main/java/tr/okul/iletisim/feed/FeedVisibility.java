package tr.okul.iletisim.feed;

public final class FeedVisibility {

	public static final String PREDICATE = """
			(p.author_id = :uid OR EXISTS (
			   SELECT 1 FROM feed_post_target t
			   WHERE t.post_id = p.id AND (
			        (t.target_type = 'USER'   AND t.target_id = :uid)
			     OR (t.target_type = 'GROUP'  AND t.target_id IN (SELECT gm.group_id FROM staff_group_member gm WHERE gm.user_id = :uid))
			     OR (t.target_type = 'BRANCH' AND t.target_id = (SELECT sm.branch_id FROM season_membership sm
			                                                     WHERE sm.season_id = p.season_id AND sm.user_id = :uid))
			     OR (t.target_type = 'ALL'    AND EXISTS (SELECT 1 FROM season_membership sm2
			                                              WHERE sm2.season_id = p.season_id AND sm2.user_id = :uid)))))
			""";

	private FeedVisibility() {
	}

}
