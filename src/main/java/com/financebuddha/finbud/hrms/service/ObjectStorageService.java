package com.financebuddha.finbud.hrms.service;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;

/**
 * Abstraction over an object store (S3, MinIO, or any S3-compatible backend).
 * <p>
 * Kept deliberately small — just the four operations the HRMS portal
 * currently needs for profile pictures. If we later need listing, copy, or
 * multipart uploads, add them here rather than letting S3 SDK types leak
 * into the rest of the codebase.
 */
public interface ObjectStorageService {

    /**
     * Upload an object to the configured bucket.
     *
     * @param key           the S3 object key (e.g.
     *                      {@code avatars/ND33004/uuid.jpg})
     * @param input         raw bytes to upload (caller owns closing)
     * @param contentType   MIME type; stored as the object's
     *                      {@code Content-Type} header so browsers serve it
     *                      correctly
     * @param contentLength exact byte count — required by the S3 SDK so it
     *                      can avoid buffering the whole stream
     * @throws com.financebuddha.finbud.hrms.exception.BadRequestException
     *         if storage is disabled or rejects the upload
     */
    void upload(String key, InputStream input, String contentType, long contentLength);

    /**
     * Delete an object. No-op if the key doesn't exist — we want idempotent
     * cleanup during user actions like "remove my picture".
     */
    void delete(String key);

    /**
     * Build a time-limited presigned GET URL.
     * <p>
     * Returns {@link Optional#empty()} if {@code key} is null/blank or
     * storage is disabled. The returned URL respects
     * {@code finbud.storage.s3.public-base-url} so the browser hits the
     * right hostname in dockerised setups.
     */
    Optional<String> presignedGetUrl(String key, Duration ttl);

    /** True if storage is enabled and the client is initialised. */
    boolean isEnabled();
}
