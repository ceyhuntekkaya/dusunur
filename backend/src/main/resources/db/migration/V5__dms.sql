CREATE TABLE dms_space (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name        varchar(100) NOT NULL,
  name_key    varchar(100) NOT NULL,
  description varchar(500),
  owner_id    bigint       NOT NULL REFERENCES app_user(id),
  created_at  timestamptz  NOT NULL DEFAULT now(),
  updated_at  timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ux_dms_space_name UNIQUE (name_key)
);

CREATE TABLE dms_space_grant (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  space_id     bigint      NOT NULL REFERENCES dms_space(id) ON DELETE CASCADE,
  subject_type varchar(10) NOT NULL CHECK (subject_type IN ('USER','GROUP','BRANCH','ALL')),
  subject_id   bigint,
  level        smallint    NOT NULL CHECK (level IN (1,2,3)),   -- 1=VIEW 2=EDIT 3=MANAGE
  created_by   bigint      NOT NULL REFERENCES app_user(id),
  created_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_dms_grant_id CHECK ((subject_type = 'ALL') = (subject_id IS NULL))
);
CREATE UNIQUE INDEX ux_dms_grant ON dms_space_grant (space_id, subject_type, COALESCE(subject_id, 0));
CREATE INDEX ix_dms_grant_subject ON dms_space_grant (subject_type, subject_id);

CREATE TABLE dms_folder (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  space_id     bigint       NOT NULL REFERENCES dms_space(id),
  parent_id    bigint       REFERENCES dms_folder(id),
  name         varchar(100) NOT NULL,
  name_key     varchar(100) NOT NULL,
  search_key   varchar(200) NOT NULL,
  depth        smallint     NOT NULL,                          -- ana dizindeki klasör = 1
  created_by   bigint       NOT NULL REFERENCES app_user(id),
  created_at   timestamptz  NOT NULL DEFAULT now(),
  updated_at   timestamptz  NOT NULL DEFAULT now(),
  deleted_at   timestamptz,
  deleted_by   bigint       REFERENCES app_user(id),
  trash_root   boolean      NOT NULL DEFAULT false,
  trashed_with bigint
);
CREATE UNIQUE INDEX ux_dms_folder_name ON dms_folder (space_id, COALESCE(parent_id, 0), name_key) WHERE deleted_at IS NULL;
CREATE INDEX ix_dms_folder_parent ON dms_folder (space_id, parent_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_dms_folder_trash  ON dms_folder (space_id, deleted_at) WHERE trash_root;

CREATE TABLE dms_file (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  space_id     bigint       NOT NULL REFERENCES dms_space(id),
  folder_id    bigint       REFERENCES dms_folder(id),
  name         varchar(200) NOT NULL,
  name_key     varchar(200) NOT NULL,
  search_key   varchar(300) NOT NULL,
  file_id      uuid         NOT NULL REFERENCES stored_file(id),
  size_bytes   bigint       NOT NULL,
  content_type varchar(150) NOT NULL,
  uploaded_by  bigint       NOT NULL REFERENCES app_user(id),
  updated_by   bigint       NOT NULL REFERENCES app_user(id),
  created_at   timestamptz  NOT NULL DEFAULT now(),
  updated_at   timestamptz  NOT NULL DEFAULT now(),
  deleted_at   timestamptz,
  deleted_by   bigint       REFERENCES app_user(id),
  trash_root   boolean      NOT NULL DEFAULT false,
  trashed_with bigint
);
CREATE UNIQUE INDEX ux_dms_file_name ON dms_file (space_id, COALESCE(folder_id, 0), name_key) WHERE deleted_at IS NULL;
CREATE INDEX ix_dms_file_folder ON dms_file (space_id, folder_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_dms_file_trash  ON dms_file (space_id, deleted_at) WHERE trash_root;
