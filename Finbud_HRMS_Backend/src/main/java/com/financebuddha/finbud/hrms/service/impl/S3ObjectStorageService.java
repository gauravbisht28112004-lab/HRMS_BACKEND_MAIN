package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.config.FinbudStorageProperties;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.service.ObjectStorageService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;

/**
 * S3 / MinIO implementation of {@link ObjectStorageService}.
 * <p>
 * Design notes:
 * <ul>
 *   <li>Both {@link S3Client} and {@link S3Presigner} are built once in
 *       {@link #init()} and cached. They are thread-safe per the AWS SDK v2
 *       docs, so a single instance is shared across all requests.</li>
 *   <li>When {@code finbud.storage.s3.endpoint} is blank, we treat this as
 *       "real AWS" — don't override endpoint, use
 *       {@link DefaultCredentialsProvider} so env vars / instance profiles
 *       work without explicit keys in properties.</li>
 *   <li>{@link #init()} logs warnings rather than throwing so the app can
 *       still boot without MinIO running locally. Upload endpoints will
 *       still fail loudly; that's better than refusing to start.</li>
 *   <li>{@link #rewritePublicUrl(String)} handles the common Docker-for-
 *       developers case where the backend container and the browser can't
 *       reach S3 via the same hostname.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3ObjectStorageService implements ObjectStorageService {

    private final FinbudStorageProperties props;

    private S3Client s3;
    private S3Presigner presigner;
    private boolean ready = false;

    @PostConstruct
    void init() {
        if (!props.isEnabled()) {
            log.info("Object storage DISABLED via finbud.storage.s3.enabled=false — avatar uploads will 503");
            return;
        }

        try {
            var credentials = buildCredentials();
            var region = Region.of(props.getRegion());
            var serviceConfig = S3Configuration.builder()
                    .pathStyleAccessEnabled(props.isPathStyleAccess())
                    .build();

            var clientBuilder = S3Client.builder()
                    .region(region)
                    .credentialsProvider(credentials)
                    .serviceConfiguration(serviceConfig);

            var presignerBuilder = S3Presigner.builder()
                    .region(region)
                    .credentialsProvider(credentials)
                    .serviceConfiguration(serviceConfig);

            if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
                URI endpointUri = URI.create(props.getEndpoint());
                clientBuilder.endpointOverride(endpointUri);
                presignerBuilder.endpointOverride(endpointUri);
            }

            this.s3 = clientBuilder.build();
            this.presigner = presignerBuilder.build();
            this.ready = true;

            log.info("S3 object storage initialised — endpoint='{}', region='{}', bucket='{}', pathStyle={}",
                    props.getEndpoint(), props.getRegion(), props.getBucket(), props.isPathStyleAccess());

            if (props.isAutoCreateBucket()) {
                ensureBucketExists();
            }
        } catch (Exception e) {
            log.warn("S3 object storage failed to initialise — uploads will fail until this is fixed: {}",
                    e.getMessage());
            this.ready = false;
        }
    }

    @PreDestroy
    void shutdown() {
        if (s3 != null) {
            try { s3.close(); } catch (Exception ignore) { /* noop */ }
        }
        if (presigner != null) {
            try { presigner.close(); } catch (Exception ignore) { /* noop */ }
        }
    }

    @Override
    public void upload(String key, InputStream input, String contentType, long contentLength) {
        requireReady();
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();
        try {
            s3.putObject(put, RequestBody.fromInputStream(input, contentLength));
            log.debug("Uploaded object key='{}' ({} bytes, {})", key, contentLength, contentType);
        } catch (NoSuchBucketException e) {
            // Someone deleted the bucket out from under us, or autoCreateBucket is
            // off and prod wasn't provisioned. Surface this clearly — it's an ops
            // problem, not a user problem.
            log.error("S3 bucket '{}' does not exist. Create it or enable finbud.storage.s3.auto-create-bucket",
                    props.getBucket());
            throw new BadRequestException(
                    "Photo storage bucket is missing. Please contact your administrator.");
        } catch (SdkClientException e) {
            // Network-level failure — MinIO/S3 unreachable, DNS failure, bad
            // endpoint, etc. This is by far the most common dev-env error
            // (forgot to `docker compose up minio`), so give a helpful message
            // instead of a generic 500.
            log.error("S3 putObject failed — storage unreachable at '{}': {}",
                    props.getEndpoint(), e.getMessage());
            throw new BadRequestException(
                    "Photo storage is not reachable. Please ensure the storage service "
                            + "(MinIO/S3) is running and reachable, then try again.");
        } catch (S3Exception e) {
            log.error("S3 putObject failed for key='{}': {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new BadRequestException("Failed to upload file to object storage: "
                    + e.awsErrorDetails().errorMessage());
        }
    }

    @Override
    public void delete(String key) {
        if (!ready || key == null || key.isBlank()) {
            return;
        }
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .build());
            log.debug("Deleted object key='{}'", key);
        } catch (S3Exception e) {
            // Never propagate — delete is best-effort during user flows.
            log.warn("S3 deleteObject failed for key='{}': {}", key, e.awsErrorDetails().errorMessage());
        }
    }

    @Override
    public Optional<String> presignedGetUrl(String key, Duration ttl) {
        if (!ready || key == null || key.isBlank()) {
            return Optional.empty();
        }
        try {
            GetObjectRequest get = GetObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .build();
            GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(get)
                    .build();
            String url = presigner.presignGetObject(presignReq).url().toString();
            return Optional.of(rewritePublicUrl(url));
        } catch (Exception e) {
            log.warn("Failed to presign GET for key='{}': {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean isEnabled() {
        return ready;
    }

    // ---------------------------------------------------------------------

    private void requireReady() {
        if (!ready) {
            throw new BadRequestException(
                    "Object storage is not available. Check finbud.storage.s3 configuration and ensure MinIO/S3 is reachable.");
        }
    }

    private software.amazon.awssdk.auth.credentials.AwsCredentialsProvider buildCredentials() {
        boolean hasStaticKeys = props.getAccessKey() != null && !props.getAccessKey().isBlank()
                && props.getSecretKey() != null && !props.getSecretKey().isBlank();
        if (hasStaticKeys) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()));
        }
        log.info("No static S3 credentials configured — falling back to DefaultCredentialsProvider "
                + "(env vars, instance profile, etc.)");
        return DefaultCredentialsProvider.create();
    }

    /**
     * Create the bucket if it doesn't exist. Harmless if it does. We swallow
     * all errors and log — a real prod bucket will be pre-created; this is
     * purely for dev convenience against MinIO.
     */
    private void ensureBucketExists() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(props.getBucket()).build());
            log.debug("Bucket '{}' already exists", props.getBucket());
        } catch (NoSuchBucketException e) {
            try {
                s3.createBucket(CreateBucketRequest.builder().bucket(props.getBucket()).build());
                log.info("Created S3 bucket '{}'", props.getBucket());
            } catch (Exception ce) {
                log.warn("Failed to auto-create bucket '{}': {}", props.getBucket(), ce.getMessage());
            }
        } catch (Exception e) {
            log.warn("headBucket check failed for '{}': {}", props.getBucket(), e.getMessage());
        }
    }

    /**
     * If {@code finbud.storage.s3.public-base-url} is set, replace the
     * scheme+host+port of the presigned URL with that base. The path +
     * query string (which carries the signature) are preserved exactly.
     */
    private String rewritePublicUrl(String originalUrl) {
        String publicBase = props.getPublicBaseUrl();
        if (publicBase == null || publicBase.isBlank()) {
            return originalUrl;
        }
        try {
            URI original = new URI(originalUrl);
            URI base = new URI(publicBase.endsWith("/")
                    ? publicBase.substring(0, publicBase.length() - 1)
                    : publicBase);
            URI rewritten = new URI(
                    base.getScheme(),
                    base.getUserInfo(),
                    base.getHost(),
                    base.getPort(),
                    original.getRawPath(),
                    original.getRawQuery(),
                    original.getRawFragment());
            return rewritten.toString();
        } catch (URISyntaxException e) {
            log.warn("Failed to rewrite presigned URL to publicBaseUrl='{}': {}", publicBase, e.getMessage());
            return originalUrl;
        }
    }
}
