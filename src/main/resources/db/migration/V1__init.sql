-- V1 baseline
-- 모든 테이블은 BaseEntity 공통 컬럼(created_at, updated_at NOT NULL / deleted_at)을 가진다.
-- FK는 엔티티가 ConstraintMode.NO_CONSTRAINT라 물리 제약을 만들지 않는다.
-- PK는 GenerationType.IDENTITY → bigserial.

-- ===== auth: refresh_tokens =====
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    deleted_at  TIMESTAMPTZ
);
CREATE UNIQUE INDEX uk_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- ===== user: users =====
CREATE TABLE users (
    id                          BIGSERIAL PRIMARY KEY,
    apple_sub_hash              VARCHAR(64)  NOT NULL,
    email                       VARCHAR(255),
    nickname                    VARCHAR(50),
    status                      VARCHAR(20)  NOT NULL,
    withdrawn_at                TIMESTAMPTZ,
    auth_type                   VARCHAR(20)  NOT NULL,
    birth_year                  SMALLINT,
    gender                      VARCHAR(10),
    occupation                  VARCHAR(30),
    onboarding_completed        BOOLEAN      NOT NULL,
    notification_enabled        BOOLEAN      NOT NULL,
    sleep_reminder_time         TIME         NOT NULL,
    pvt_reminder_time           TIME         NOT NULL,
    custom_notification_options JSONB        NOT NULL,
    notification_timezone       VARCHAR(40)  NOT NULL,
    created_at                  TIMESTAMPTZ  NOT NULL,
    updated_at                  TIMESTAMPTZ  NOT NULL,
    deleted_at                  TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_users_apple_sub_hash ON users (apple_sub_hash);
CREATE INDEX idx_users_status ON users (status);

-- ===== user: user_devices =====
CREATE TABLE user_devices (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    fcm_token       VARCHAR(255) NOT NULL,
    platform        VARCHAR(10)  NOT NULL,
    last_active_at  TIMESTAMPTZ  NOT NULL,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    deleted_at      TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_user_devices_user_fcm ON user_devices (user_id, fcm_token);
CREATE INDEX idx_user_devices_user_id ON user_devices (user_id);
CREATE INDEX idx_user_devices_fcm_token ON user_devices (fcm_token);

-- ===== user: consent_logs =====
CREATE TABLE consent_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL,
    consent_type    VARCHAR(30) NOT NULL,
    policy_version  VARCHAR(10) NOT NULL,
    agreed          BOOLEAN     NOT NULL,
    agreed_at       TIMESTAMPTZ NOT NULL,
    client_ip       INET,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_consent_logs_user_id ON consent_logs (user_id);
CREATE INDEX idx_consent_logs_audit_trail ON consent_logs (user_id, consent_type, agreed_at DESC);

-- ===== user: notification_logs (PK 컬럼명 log_id) =====
CREATE TABLE notification_logs (
    log_id            BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    user_device_id    BIGINT,
    notification_type VARCHAR(255) NOT NULL,
    title             VARCHAR(255) NOT NULL,
    body              TEXT         NOT NULL,
    payload           JSONB        NOT NULL,
    fcm_message_id    VARCHAR(255),
    status            VARCHAR(255) NOT NULL,
    error_code        VARCHAR(255),
    error_message     TEXT,
    scheduled_at      TIMESTAMPTZ  NOT NULL,
    sent_at           TIMESTAMPTZ,
    delivered_at      TIMESTAMPTZ,
    opened_at         TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    deleted_at        TIMESTAMPTZ
);
CREATE INDEX idx_notification_logs_user_id ON notification_logs (user_id);
CREATE INDEX idx_notification_logs_user_device_id ON notification_logs (user_device_id);

-- ===== sleep: sleep_records =====
CREATE TABLE sleep_records (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT      NOT NULL,
    sleep_date              DATE        NOT NULL,
    total_minutes           INTEGER     NOT NULL,
    deep_minutes            INTEGER,
    rem_minutes             INTEGER,
    core_minutes            INTEGER,
    awake_minutes           INTEGER,
    in_bed_minutes          INTEGER,
    unspecified_minutes     INTEGER,
    sample_count            INTEGER,
    night_hrv_ms            DOUBLE PRECISION,
    weekly_hrv_baseline_ms  DOUBLE PRECISION,
    data_completeness       VARCHAR(20),
    raw_payload             JSONB,
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    deleted_at              TIMESTAMPTZ
);
CREATE INDEX idx_sleep_records_user_id ON sleep_records (user_id);
CREATE INDEX idx_sleep_records_user_sleep_date ON sleep_records (user_id, sleep_date);

-- ===== pvt: pvt_sessions =====
CREATE TABLE pvt_sessions (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT           NOT NULL,
    measurement_id     UUID             NOT NULL,
    started_at         TIMESTAMPTZ      NOT NULL,
    ended_at           TIMESTAMPTZ      NOT NULL,
    total_duration_ms  INTEGER          NOT NULL,
    total_count        INTEGER          NOT NULL,
    raw_rt_ms          INTEGER[]        NOT NULL,
    avg_rt_ms          DOUBLE PRECISION NOT NULL,
    median_rt_ms       DOUBLE PRECISION,
    lapses_mild        INTEGER          NOT NULL,
    lapses_timeout     INTEGER          NOT NULL,
    false_starts       INTEGER          NOT NULL,
    is_valid           BOOLEAN          NOT NULL,
    invalid_reason     VARCHAR(50),
    trials             JSONB            NOT NULL,
    created_at         TIMESTAMPTZ      NOT NULL,
    updated_at         TIMESTAMPTZ      NOT NULL,
    deleted_at         TIMESTAMPTZ
);
CREATE INDEX idx_pvt_sessions_user_id ON pvt_sessions (user_id);
CREATE INDEX idx_pvt_sessions_measurement_id ON pvt_sessions (measurement_id);

-- ===== roi: brain_roi_scores =====
CREATE TABLE brain_roi_scores (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT       NOT NULL,
    session_id           BIGINT       NOT NULL,
    sleep_id             BIGINT       NOT NULL,
    calculation_scenario VARCHAR(255) NOT NULL,
    final_score          INTEGER      NOT NULL,
    sleep_score          INTEGER      NOT NULL,
    pvt_score            INTEGER      NOT NULL,
    quadrant             VARCHAR(255) NOT NULL,
    formula_version      VARCHAR(255) NOT NULL,
    breakdown            JSONB        NOT NULL,
    measured_at          TIMESTAMPTZ  NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    deleted_at           TIMESTAMPTZ
);
CREATE INDEX idx_brain_roi_scores_user_id ON brain_roi_scores (user_id);
CREATE INDEX idx_brain_roi_scores_session_id ON brain_roi_scores (session_id);
CREATE INDEX idx_brain_roi_scores_sleep_id ON brain_roi_scores (sleep_id);

-- ===== roi: recommendations =====
CREATE TABLE recommendations (
    id              BIGSERIAL PRIMARY KEY,
    roi_score_id    BIGINT       NOT NULL,
    quadrant_key    VARCHAR(255) NOT NULL,
    title           VARCHAR(100) NOT NULL,
    message         TEXT         NOT NULL,
    suggested_tasks JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_recommendations_roi_score_id ON recommendations (roi_score_id);
