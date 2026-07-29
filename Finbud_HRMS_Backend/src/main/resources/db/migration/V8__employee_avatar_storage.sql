-- =========================================================================
-- V8: Profile-picture storage backed by S3 / MinIO
-- =========================================================================
-- The existing employees.profile_picture_url column stores a raw URL string
-- (either a pasted HTTP URL or, historically, a data URL from the admin
-- form's local preview). That column stays — it remains the value returned
-- in EmployeeResponse.profilePictureUrl, for backward compatibility.
--
-- What changes:
--   * New column avatar_key      — the S3 object key (e.g. avatars/ND33004/<uuid>.jpg)
--   * New column avatar_content_type — stored MIME type, used for HEAD responses
--     and for serving back through a gateway if we ever need streaming
--
-- Runtime behaviour (EmployeeServiceImpl):
--   if (avatar_key is set) profilePictureUrl = presignedGetUrl(avatar_key)
--   else                   profilePictureUrl = employees.profile_picture_url
-- ---------------------------------------------------------------------------

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS avatar_key          VARCHAR(512),
    ADD COLUMN IF NOT EXISTS avatar_content_type VARCHAR(100);

COMMENT ON COLUMN employees.avatar_key IS
    'S3/MinIO object key for the employee''s profile picture. '
    'Null = no uploaded avatar; fall back to profile_picture_url (legacy pasted URL).';

COMMENT ON COLUMN employees.avatar_content_type IS
    'MIME type of the stored avatar (image/jpeg, image/png, image/webp). '
    'Null when avatar_key is null.';

-- We don't index these columns — avatar_key is looked up only when serving a
-- specific employee row (by id or employee_id) which already has indexes.
