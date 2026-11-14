CREATE TABLE branch (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name        varchar(100) NOT NULL,
  name_key    varchar(100) NOT NULL,
  active      boolean      NOT NULL DEFAULT true,
  created_at  timestamptz  NOT NULL DEFAULT now(),
  updated_at  timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ux_branch_name_key UNIQUE (name_key)
);

CREATE TABLE app_user (
  id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  email              varchar(254) NOT NULL,
  password_hash      varchar(100),
  first_name         varchar(50)  NOT NULL,
  last_name          varchar(50)  NOT NULL,
  search_key         varchar(400) NOT NULL,           -- fold(first_name + ' ' + last_name + ' ' + email)
  phone              varchar(13),
  discipline         varchar(100),                    -- branş
  status             varchar(10)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','PASSIVE')),
  system_admin       boolean      NOT NULL DEFAULT false,
  kvkk_version       integer      NOT NULL DEFAULT 0,
  kvkk_accepted_at   timestamptz,
  failed_login_count integer      NOT NULL DEFAULT 0,
  locked_until       timestamptz,
  last_login_at      timestamptz,
  created_at         timestamptz  NOT NULL DEFAULT now(),
  updated_at         timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ux_user_email UNIQUE (email)
);

CREATE TABLE season (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name         varchar(9)  NOT NULL,
  start_date   date        NOT NULL,
  end_date     date        NOT NULL,
  status       varchar(10) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','CLOSED')),
  activated_at timestamptz,
  closed_at    timestamptz,
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_season_name UNIQUE (name),
  CONSTRAINT ck_season_dates CHECK (end_date > start_date)
);
CREATE UNIQUE INDEX ux_season_one_active ON season ((true)) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX ux_season_one_draft  ON season ((true)) WHERE status = 'DRAFT';

CREATE TABLE season_membership (
  id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  season_id            bigint  NOT NULL REFERENCES season(id) ON DELETE CASCADE,
  user_id              bigint  NOT NULL REFERENCES app_user(id),
  branch_id            bigint  NOT NULL REFERENCES branch(id),
  title                varchar(100),
  is_manager           boolean NOT NULL DEFAULT false,
  is_print_operator    boolean NOT NULL DEFAULT false,
  can_create_dms_space boolean NOT NULL DEFAULT false,
  print_approver_id    bigint  REFERENCES app_user(id),
  created_at           timestamptz NOT NULL DEFAULT now(),
  updated_at           timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_membership_season_user UNIQUE (season_id, user_id),
  CONSTRAINT ck_membership_not_self CHECK (print_approver_id IS NULL OR print_approver_id <> user_id)
);
CREATE INDEX ix_membership_user     ON season_membership (user_id);
CREATE INDEX ix_membership_branch   ON season_membership (season_id, branch_id);
CREATE INDEX ix_membership_approver ON season_membership (season_id, print_approver_id);

CREATE TABLE staff_group (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name        varchar(100) NOT NULL,
  name_key    varchar(100) NOT NULL,
  description varchar(500),
  archived    boolean      NOT NULL DEFAULT false,
  created_by  bigint       NOT NULL REFERENCES app_user(id),
  created_at  timestamptz  NOT NULL DEFAULT now(),
  updated_at  timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ux_group_name_key UNIQUE (name_key)
);

CREATE TABLE staff_group_member (
  group_id  bigint      NOT NULL REFERENCES staff_group(id),
  user_id   bigint      NOT NULL REFERENCES app_user(id),
  added_at  timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (group_id, user_id)
);
CREATE INDEX ix_group_member_user ON staff_group_member (user_id);

CREATE TABLE auth_token (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     bigint      NOT NULL REFERENCES app_user(id),
  token_hash  char(64)    NOT NULL,                  -- sha256 hex
  purpose     varchar(10) NOT NULL CHECK (purpose IN ('SETUP','RESET')),
  expires_at  timestamptz NOT NULL,
  used_at     timestamptz,
  created_at  timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_auth_token_hash UNIQUE (token_hash)
);
CREATE INDEX ix_auth_token_user ON auth_token (user_id, created_at);

CREATE TABLE stored_file (
  id             uuid PRIMARY KEY,
  original_name  varchar(255) NOT NULL,
  content_type   varchar(150) NOT NULL,
  size_bytes     bigint       NOT NULL,
  storage_path   varchar(300) NOT NULL,
  purpose        varchar(10)  NOT NULL CHECK (purpose IN ('FEED','CHAT','PRINT','DMS','CALENDAR')),
  uploaded_by    bigint       NOT NULL REFERENCES app_user(id),
  attached       boolean      NOT NULL DEFAULT false,
  created_at     timestamptz  NOT NULL DEFAULT now(),
  deleted_at     timestamptz
);
CREATE INDEX ix_file_orphan ON stored_file (created_at) WHERE attached = false AND deleted_at IS NULL;

CREATE TABLE notification (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     bigint       NOT NULL REFERENCES app_user(id),
  type        varchar(50)  NOT NULL,
  title       varchar(200) NOT NULL,
  body        varchar(500),
  link        varchar(300),
  created_at  timestamptz  NOT NULL DEFAULT now(),
  read_at     timestamptz
);
CREATE INDEX ix_notification_user   ON notification (user_id, id DESC);
CREATE INDEX ix_notification_unread ON notification (user_id) WHERE read_at IS NULL;

CREATE TABLE push_subscription (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     bigint        NOT NULL REFERENCES app_user(id),
  endpoint    varchar(1000) NOT NULL,
  p256dh      varchar(200)  NOT NULL,
  auth        varchar(100)  NOT NULL,
  user_agent  varchar(300),
  created_at  timestamptz   NOT NULL DEFAULT now(),
  CONSTRAINT ux_push_endpoint UNIQUE (endpoint)
);
CREATE INDEX ix_push_user ON push_subscription (user_id);

CREATE TABLE audit_log (
  id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  created_at    timestamptz  NOT NULL DEFAULT now(),
  actor_user_id bigint       REFERENCES app_user(id),
  ip            varchar(45),
  action        varchar(60)  NOT NULL,
  action_label  varchar(120) NOT NULL,
  entity_type   varchar(40),
  entity_id     varchar(40),
  detail        jsonb
);
CREATE INDEX ix_audit_created ON audit_log (created_at DESC);
CREATE INDEX ix_audit_actor   ON audit_log (actor_user_id, created_at DESC);

CREATE FUNCTION audit_log_immutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'audit_log is append-only'; END $$;
CREATE TRIGGER trg_audit_log_immutable BEFORE UPDATE OR DELETE ON audit_log
  FOR EACH ROW EXECUTE FUNCTION audit_log_immutable();

CREATE TABLE SPRING_SESSION (
	PRIMARY_ID CHAR(36) NOT NULL,
	SESSION_ID CHAR(36) NOT NULL,
	CREATION_TIME BIGINT NOT NULL,
	LAST_ACCESS_TIME BIGINT NOT NULL,
	MAX_INACTIVE_INTERVAL INT NOT NULL,
	EXPIRY_TIME BIGINT NOT NULL,
	PRINCIPAL_NAME VARCHAR(100),
	CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
	SESSION_PRIMARY_ID CHAR(36) NOT NULL,
	ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
	ATTRIBUTE_BYTES BYTEA NOT NULL,
	CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
	CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);
