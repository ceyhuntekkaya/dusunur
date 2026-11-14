CREATE TABLE feed_post (
  id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  season_id        bigint        NOT NULL REFERENCES season(id),
  author_id        bigint        NOT NULL REFERENCES app_user(id),
  content          varchar(5000),
  important        boolean       NOT NULL DEFAULT false,
  pinned_at        timestamptz,
  pinned_by        bigint        REFERENCES app_user(id),
  edited_at        timestamptz,
  last_reminder_at timestamptz,
  created_at       timestamptz   NOT NULL DEFAULT now(),
  updated_at       timestamptz   NOT NULL DEFAULT now(),
  deleted_at       timestamptz,
  deleted_by       bigint        REFERENCES app_user(id)
);
CREATE INDEX ix_feed_post_season  ON feed_post (season_id, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_feed_post_author  ON feed_post (author_id, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_feed_post_pinned  ON feed_post (season_id, pinned_at DESC) WHERE pinned_at IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE feed_post_target (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  post_id     bigint      NOT NULL REFERENCES feed_post(id) ON DELETE CASCADE,
  target_type varchar(10) NOT NULL CHECK (target_type IN ('USER','GROUP','BRANCH','ALL')),
  target_id   bigint,
  CONSTRAINT ck_feed_target_id CHECK ((target_type = 'ALL') = (target_id IS NULL))
);
CREATE UNIQUE INDEX ux_feed_target ON feed_post_target (post_id, target_type, COALESCE(target_id, 0));
CREATE INDEX ix_feed_target_lookup ON feed_post_target (target_type, target_id, post_id);

CREATE TABLE feed_post_attachment (
  post_id    bigint   NOT NULL REFERENCES feed_post(id) ON DELETE CASCADE,
  file_id    uuid     NOT NULL REFERENCES stored_file(id),
  sort_order smallint NOT NULL,
  PRIMARY KEY (post_id, file_id)
);

CREATE TABLE feed_comment (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  post_id     bigint        NOT NULL REFERENCES feed_post(id),
  author_id   bigint        NOT NULL REFERENCES app_user(id),
  content     varchar(1000) NOT NULL,
  created_at  timestamptz   NOT NULL DEFAULT now(),
  deleted_at  timestamptz,
  deleted_by  bigint        REFERENCES app_user(id)
);
CREATE INDEX ix_feed_comment_post ON feed_comment (post_id, id) WHERE deleted_at IS NULL;

CREATE TABLE feed_like (
  post_id    bigint      NOT NULL REFERENCES feed_post(id),
  user_id    bigint      NOT NULL REFERENCES app_user(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (post_id, user_id)
);

CREATE TABLE feed_read (
  post_id  bigint      NOT NULL REFERENCES feed_post(id),
  user_id  bigint      NOT NULL REFERENCES app_user(id),
  read_at  timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (post_id, user_id)
);
CREATE INDEX ix_feed_read_user ON feed_read (user_id, post_id);
