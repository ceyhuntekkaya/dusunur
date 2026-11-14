CREATE TABLE chat_conversation (
  id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  type             varchar(6)   NOT NULL CHECK (type IN ('DIRECT','GROUP','FREE')),
  title            varchar(100),
  direct_key       varchar(41),
  staff_group_id   bigint       REFERENCES staff_group(id),
  read_only        boolean      NOT NULL DEFAULT false,
  last_message_id  bigint,
  last_message_at  timestamptz,
  created_by       bigint       REFERENCES app_user(id),
  created_at       timestamptz  NOT NULL DEFAULT now(),
  updated_at       timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ux_chat_direct_key UNIQUE (direct_key),
  CONSTRAINT ux_chat_group      UNIQUE (staff_group_id),
  CONSTRAINT ck_chat_direct CHECK ((type = 'DIRECT') = (direct_key IS NOT NULL)),
  CONSTRAINT ck_chat_group  CHECK ((type = 'GROUP')  = (staff_group_id IS NOT NULL)),
  CONSTRAINT ck_chat_title  CHECK (type = 'DIRECT' OR title IS NOT NULL)
);

CREATE TABLE chat_member (
  conversation_id      bigint      NOT NULL REFERENCES chat_conversation(id),
  user_id              bigint      NOT NULL REFERENCES app_user(id),
  role                 varchar(6)  NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('OWNER','MEMBER')),
  muted                boolean     NOT NULL DEFAULT false,
  last_read_message_id bigint      NOT NULL DEFAULT 0,
  joined_at            timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (conversation_id, user_id)
);
CREATE INDEX ix_chat_member_user ON chat_member (user_id);

CREATE TABLE chat_message (
  id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  conversation_id  bigint        NOT NULL REFERENCES chat_conversation(id),
  season_id        bigint        NOT NULL REFERENCES season(id),
  sender_id        bigint        NOT NULL REFERENCES app_user(id),
  content          varchar(4000),
  reply_to_id      bigint        REFERENCES chat_message(id),
  client_id        uuid          NOT NULL,
  created_at       timestamptz   NOT NULL DEFAULT now(),
  deleted_at       timestamptz,
  CONSTRAINT ux_chat_message_client UNIQUE (sender_id, client_id)
);
CREATE INDEX ix_chat_message_conv ON chat_message (conversation_id, id DESC);

CREATE TABLE chat_message_attachment (
  message_id  bigint   NOT NULL REFERENCES chat_message(id),
  file_id     uuid     NOT NULL REFERENCES stored_file(id),
  sort_order  smallint NOT NULL,
  PRIMARY KEY (message_id, file_id)
);
