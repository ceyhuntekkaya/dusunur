CREATE TABLE print_request (
  id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  season_id            bigint       NOT NULL REFERENCES season(id),
  requester_id         bigint       NOT NULL REFERENCES app_user(id),
  branch_id            bigint       NOT NULL REFERENCES branch(id),
  title                varchar(200) NOT NULL,
  file_id              uuid         REFERENCES stored_file(id),
  original_on_paper    boolean      NOT NULL DEFAULT false,
  page_count           integer      NOT NULL CHECK (page_count BETWEEN 1 AND 2000),
  copies               integer      NOT NULL CHECK (copies BETWEEN 1 AND 1000),
  color                varchar(5)   NOT NULL CHECK (color IN ('BW','COLOR')),
  sides                varchar(6)   NOT NULL CHECK (sides IN ('SINGLE','DOUBLE')),
  paper_size           varchar(2)   NOT NULL CHECK (paper_size IN ('A4','A3')),
  secret               boolean      NOT NULL DEFAULT false,
  due_at               timestamptz  NOT NULL,
  note                 varchar(500),
  total_pages          integer      NOT NULL CHECK (total_pages BETWEEN 1 AND 20000),
  status               varchar(20)  NOT NULL CHECK (status IN ('PENDING_APPROVAL','APPROVED','REJECTED','IN_PROGRESS','READY','DELIVERED','CANCELLED')),
  approver_id          bigint       REFERENCES app_user(id),
  original_approver_id bigint       REFERENCES app_user(id),
  decided_by           bigint       REFERENCES app_user(id),
  decided_at           timestamptz,
  reject_reason        varchar(500),
  operator_id          bigint       REFERENCES app_user(id),
  started_at           timestamptz,
  ready_at             timestamptz,
  delivered_at         timestamptz,
  cancelled_by         bigint       REFERENCES app_user(id),
  cancelled_at         timestamptz,
  cancel_reason        varchar(200),
  file_purged_at       timestamptz,
  copied_from_id       bigint       REFERENCES print_request(id),
  version              integer      NOT NULL DEFAULT 0,
  created_at           timestamptz  NOT NULL DEFAULT now(),
  updated_at           timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ck_print_file_or_paper CHECK (file_id IS NOT NULL OR original_on_paper)
);
CREATE INDEX ix_print_requester ON print_request (requester_id, id DESC);
CREATE INDEX ix_print_approver  ON print_request (approver_id, status);
CREATE INDEX ix_print_decided   ON print_request (decided_by, decided_at DESC);
CREATE INDEX ix_print_queue     ON print_request (season_id, branch_id, status, due_at);
CREATE INDEX ix_print_purge     ON print_request (status) WHERE file_id IS NOT NULL AND file_purged_at IS NULL;
CREATE INDEX ix_print_delivered ON print_request (delivered_at) WHERE status = 'DELIVERED';

CREATE TABLE print_request_history (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  request_id  bigint      NOT NULL REFERENCES print_request(id),
  from_status varchar(20),
  to_status   varchar(20) NOT NULL,
  actor_id    bigint      REFERENCES app_user(id),
  note        varchar(500),
  created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_print_history_req ON print_request_history (request_id, id);

CREATE TABLE print_delegation (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  approver_id  bigint      NOT NULL REFERENCES app_user(id),
  delegate_id  bigint      NOT NULL REFERENCES app_user(id),
  start_date   date        NOT NULL,
  end_date     date        NOT NULL,
  created_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_delegation_dates CHECK (end_date >= start_date),
  CONSTRAINT ck_delegation_self  CHECK (approver_id <> delegate_id)
);
CREATE INDEX ix_delegation_approver ON print_delegation (approver_id, end_date);
