CREATE TABLE poll
(
    id         BIGSERIAL PRIMARY KEY,
    question   VARCHAR(255) NOT NULL,
    start_date TIMESTAMP    NOT NULL,
    end_date   TIMESTAMP    NOT NULL,
    status     VARCHAR(20)  NOT NULL
);

CREATE TABLE poll_option
(
    id          BIGSERIAL PRIMARY KEY,
    poll_id     BIGINT NOT NULL REFERENCES poll (id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    votes       INTEGER DEFAULT 0
);