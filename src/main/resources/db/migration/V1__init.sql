-- 글
CREATE TABLE post (
    id            BIGSERIAL     PRIMARY KEY,
    slug          VARCHAR(200)  NOT NULL UNIQUE,
    title         VARCHAR(300)  NOT NULL,
    summary       VARCHAR(500),
    content_md    TEXT          NOT NULL,
    content_html  TEXT          NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    view_count    BIGINT        NOT NULL DEFAULT 0,
    published_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_post_status CHECK (status IN ('DRAFT', 'PUBLISHED'))
);

-- 공개 목록 조회용. status 필터 + published_at 역순이 기본 접근 경로.
CREATE INDEX idx_post_status_published_at ON post (status, published_at DESC);

-- 태그
CREATE TABLE tag (
    id    BIGSERIAL    PRIMARY KEY,
    name  VARCHAR(50)  NOT NULL UNIQUE,
    slug  VARCHAR(50)  NOT NULL UNIQUE
);

CREATE TABLE post_tag (
    post_id  BIGINT NOT NULL REFERENCES post(id) ON DELETE CASCADE,
    tag_id   BIGINT NOT NULL REFERENCES tag(id)  ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);

CREATE INDEX idx_post_tag_tag_id ON post_tag (tag_id);
