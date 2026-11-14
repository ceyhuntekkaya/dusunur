CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE cal_calendar (
  id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  type            varchar(12)  NOT NULL CHECK (type IN ('INSTITUTION','BRANCH','PERSONAL','GROUP','RESOURCE')),
  name            varchar(100) NOT NULL,
  name_key        varchar(100) NOT NULL,
  description     varchar(500),
  owner_id        bigint       REFERENCES app_user(id),
  branch_id       bigint       REFERENCES branch(id),
  staff_group_id  bigint       REFERENCES staff_group(id),
  is_default      boolean      NOT NULL DEFAULT false,
  read_only       boolean      NOT NULL DEFAULT false,
  created_by      bigint       REFERENCES app_user(id),
  created_at      timestamptz  NOT NULL DEFAULT now(),
  updated_at      timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ck_cal_personal CHECK ((type = 'PERSONAL') = (owner_id IS NOT NULL)),
  CONSTRAINT ck_cal_group    CHECK ((type = 'GROUP') = (staff_group_id IS NOT NULL)),
  CONSTRAINT ck_cal_branch   CHECK (type <> 'BRANCH' OR branch_id IS NOT NULL),
  CONSTRAINT ck_cal_default  CHECK (NOT is_default OR type = 'PERSONAL')
);
CREATE UNIQUE INDEX ux_cal_institution ON cal_calendar ((true)) WHERE type = 'INSTITUTION';
CREATE UNIQUE INDEX ux_cal_branch      ON cal_calendar (branch_id) WHERE type = 'BRANCH';
CREATE UNIQUE INDEX ux_cal_group       ON cal_calendar (staff_group_id) WHERE type = 'GROUP';
CREATE UNIQUE INDEX ux_cal_default     ON cal_calendar (owner_id) WHERE is_default;
CREATE UNIQUE INDEX ux_cal_resource    ON cal_calendar (name_key) WHERE type = 'RESOURCE';
CREATE INDEX ix_cal_owner ON cal_calendar (owner_id) WHERE type = 'PERSONAL';

CREATE TABLE cal_share (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  calendar_id  bigint      NOT NULL REFERENCES cal_calendar(id) ON DELETE CASCADE,
  subject_type varchar(6)  NOT NULL CHECK (subject_type IN ('USER','GROUP')),
  subject_id   bigint      NOT NULL,
  level        smallint    NOT NULL CHECK (level IN (1,2)),     -- 1=VIEW 2=EDIT
  created_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_cal_share UNIQUE (calendar_id, subject_type, subject_id)
);
CREATE INDEX ix_cal_share_subject ON cal_share (subject_type, subject_id);

CREATE TABLE cal_entry (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  calendar_id  bigint        NOT NULL REFERENCES cal_calendar(id) ON DELETE CASCADE,
  is_resource  boolean       NOT NULL,
  title        varchar(150)  NOT NULL,
  all_day      boolean       NOT NULL,
  start_at     timestamptz   NOT NULL,
  end_at       timestamptz   NOT NULL,
  location     varchar(200),
  description  varchar(2000),
  file_id      uuid          REFERENCES stored_file(id),
  created_by   bigint        NOT NULL REFERENCES app_user(id),
  updated_by   bigint        NOT NULL REFERENCES app_user(id),
  created_at   timestamptz   NOT NULL DEFAULT now(),
  updated_at   timestamptz   NOT NULL DEFAULT now(),
  CONSTRAINT ck_cal_entry_range CHECK (end_at > start_at),
  CONSTRAINT ex_cal_resource_overlap EXCLUDE USING gist
    (calendar_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&) WHERE (is_resource)
);
CREATE INDEX ix_cal_entry_range ON cal_entry (calendar_id, start_at, end_at);
