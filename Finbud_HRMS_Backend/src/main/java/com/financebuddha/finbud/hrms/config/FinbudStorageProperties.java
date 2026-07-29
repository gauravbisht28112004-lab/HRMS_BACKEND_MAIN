package com.financebuddha.finbud.hrms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Typed binding for {@code finbud.storage.*} properties.
 * <p>
 * Kept deliberately flat (no nested {@code Storage}/{@code S3} classes) so
 * the IntelliJ property editor and the Spring Boot config UI both show every
 * key in a single pane. If we ever add a second storage backend (e.g.
 * Azure Blob), nest at that point.
 *
 * @see com.financebuddha.finbud.hrms.service.ObjectStorageService
 * @see com.financebuddha.finbud.hrms.service.impl.S3ObjectStorageService
 */
@Data
@Component
@ConfigurationProperties(prefix = "finbud.storage.s3")
public class FinbudStorageProperties {

    /** Kill-switch: false → avatar endpoints return 503. */
    private boolean enabled = true;

    /** Blank = real AWS S3. For MinIO: e.g. http://localhost:9000. */
    private String endpoint = "";

    /** AWS region. Ignored-but-required for MinIO; set to anything. */
    private String region = "us-east-1";

    /** Target bucket. Created on startup if {@link #autoCreateBucket} is true. */
    private String bucket = "finbud-hrms-avatars";

    /** Static access key (MinIO) — prefer env vars / instance profile for real AWS. */
    private String accessKey = "";

    private String secretKey = "";

    /** MUST be true for MinIO. false works for real AWS unless the bucket name has dots. */
    private boolean pathStyleAccess = true;

    /**
     * Optional CDN / reverse-proxy base URL. When set, presigned URLs are
     * rewritten to this host so the browser doesn't try to hit the internal
     * S3 endpoint. Example: backend talks to {@code http://minio:9000},
     * browser hits {@code http://localhost:9000}.
     */
    private String publicBaseUrl = "";

    /** Seconds. Applied to every presigned GET URL returned in responses. */
    private long presignTtlSeconds = 3600L;

    /** Hard cap (bytes) on avatar uploads. 5 MB default. */
    private long maxUploadBytes = 5L * 1024 * 1024;

    /** Comma-separated MIME allow-list. Anything else → 400. */
    private String allowedContentTypes = "image/jpeg,image/png,image/webp";

    /** Auto-create the bucket on startup if it doesn't exist. */
    private boolean autoCreateBucket = true;

    /**
     * Parses {@link #allowedContentTypes} into a lowercase list of allowed
     * MIME types. Called on every upload; cheap enough we don't memoise.
     */
    public List<String> allowedContentTypeList() {
        return Arrays.stream(allowedContentTypes.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
