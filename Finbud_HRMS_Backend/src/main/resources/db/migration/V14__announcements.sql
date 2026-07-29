-- ---------------------------------------------------------------------------
-- V14: Announcements — Admin/HR-published notices visible on every dashboard.
--
-- Q2 of the Apr-25 feature triage. Soft-delete via is_active=false rather
-- than hard delete so HR can restore an accidentally-archived announcement
-- and the audit trail stays intact.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS announcements (
    id                       BIGSERIAL    PRIMARY KEY,
    title                    VARCHAR(200) NOT NULL,
    message                  TEXT         NOT NULL,
    priority                 VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    is_active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by_employee_id   BIGINT       NOT NULL REFERENCES employees(id),
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    version                  BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_announcement_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW'))
);

-- Hot path: "fetch active announcements, newest first" — composite index
-- covers both the filter and the ORDER BY in a single index scan.
CREATE INDEX IF NOT EXISTS idx_announcements_active_created
    ON announcements (is_active, created_at DESC);
