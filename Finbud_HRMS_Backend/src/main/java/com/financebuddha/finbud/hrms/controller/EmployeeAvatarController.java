package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.config.FinbudStorageProperties;
import com.financebuddha.finbud.hrms.dto.employee.EmployeeResponse;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.EmployeeMapper;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.service.ObjectStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * Profile-picture upload / removal endpoints.
 * <p>
 * Authorization rule (enforced by {@code @PreAuthorize} SpEL): an employee
 * may mutate their <em>own</em> avatar, and ROLE_ADMIN / ROLE_HR may mutate
 * anyone's. We intentionally key on {@code employeeCode} (the external
 * alphanumeric ID like "ND33004") rather than the DB PK — URLs in the
 * frontend already use the employee code everywhere, and it keeps the
 * SpEL check symmetrical with what's stored on the JWT principal.
 *
 * <p>On upload we:
 * <ol>
 *   <li>Validate the file (non-empty, size &le; configured max, MIME in
 *       allow-list).</li>
 *   <li>Generate a new S3 key: {@code avatars/{employeeCode}/{uuid}.{ext}}.
 *       Using a UUID ensures each replacement is a fresh object — we
 *       never serve a stale cached image from a CDN.</li>
 *   <li>Put the bytes to object storage.</li>
 *   <li>Delete the <em>previous</em> object (best effort) to avoid
 *       orphaned files.</li>
 *   <li>Persist {@code avatarKey} + {@code avatarContentType} on the
 *       Employee row and return the enriched response. MapStruct's
 *       {@code @AfterMapping} hook turns {@code avatarKey} into a
 *       presigned URL on the {@code profilePictureUrl} response field.</li>
 * </ol>
 */
@Slf4j
@Tag(name = "Employee Avatar", description = "Upload / remove an employee's profile picture")
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeAvatarController {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final ObjectStorageService objectStorage;
    private final FinbudStorageProperties storageProps;

    @Operation(summary = "Upload or replace an employee's profile picture")
    @PostMapping(value = "/{employeeCode}/avatar")
    @PreAuthorize("hasAnyRole('ADMIN','HR') or (authentication.principal.employeeId == #employeeCode)")
    @Transactional
    public ResponseEntity<EmployeeResponse> uploadAvatar(
            @PathVariable String employeeCode,
            @RequestParam("file") MultipartFile file) {

        if (!objectStorage.isEnabled()) {
            throw new BadRequestException(
                    "Object storage is not enabled or configured. Please contact your administrator.");
        }

        validateFile(file);

        Employee employee = employeeRepository.findByEmployeeId(employeeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "employeeId", employeeCode));

        String contentType = file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType().toLowerCase(Locale.ROOT);
        String ext = extensionFor(contentType, file.getOriginalFilename());
        String newKey = "avatars/" + employeeCode + "/" + UUID.randomUUID() + "." + ext;

        try (var in = file.getInputStream()) {
            objectStorage.upload(newKey, in, contentType, file.getSize());
        } catch (IOException e) {
            log.error("Failed to read multipart file for employee {}: {}", employeeCode, e.getMessage());
            throw new BadRequestException("Failed to read uploaded file");
        }

        // Clean up the previous object — best effort, never blocks the response.
        String previousKey = employee.getAvatarKey();
        employee.setAvatarKey(newKey);
        employee.setAvatarContentType(contentType);
        employeeRepository.save(employee);

        if (previousKey != null && !previousKey.isBlank() && !previousKey.equals(newKey)) {
            objectStorage.delete(previousKey);
        }

        log.info("Avatar uploaded for employee {} ({} bytes, {})",
                employeeCode, file.getSize(), contentType);
        return ResponseEntity.ok(employeeMapper.toResponse(employee));
    }

    @Operation(summary = "Remove an employee's profile picture")
    @DeleteMapping("/{employeeCode}/avatar")
    @PreAuthorize("hasAnyRole('ADMIN','HR') or (authentication.principal.employeeId == #employeeCode)")
    @Transactional
    public ResponseEntity<EmployeeResponse> deleteAvatar(@PathVariable String employeeCode) {
        Employee employee = employeeRepository.findByEmployeeId(employeeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "employeeId", employeeCode));

        String key = employee.getAvatarKey();
        employee.setAvatarKey(null);
        employee.setAvatarContentType(null);
        employeeRepository.save(employee);

        if (key != null && !key.isBlank()) {
            objectStorage.delete(key);
            log.info("Avatar removed for employee {} (was key={})", employeeCode, key);
        }

        return ResponseEntity.status(HttpStatus.OK).body(employeeMapper.toResponse(employee));
    }

    // ---------------------------------------------------------------------

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file uploaded");
        }
        if (file.getSize() > storageProps.getMaxUploadBytes()) {
            long maxMb = storageProps.getMaxUploadBytes() / (1024 * 1024);
            throw new BadRequestException("File is too large. Maximum size is " + maxMb + " MB");
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        var allowed = storageProps.allowedContentTypeList();
        if (!allowed.contains(contentType)) {
            throw new BadRequestException("Unsupported image type '" + contentType
                    + "'. Allowed: " + String.join(", ", allowed));
        }
    }

    /**
     * Pick a file extension to use in the S3 key. Prefer the content type
     * (the client can't lie about that as easily as the filename), fall back
     * to the original filename, then to "bin" as a last resort.
     */
    private String extensionFor(String contentType, String originalFilename) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> {
                if (originalFilename != null && originalFilename.contains(".")) {
                    String candidate = originalFilename
                            .substring(originalFilename.lastIndexOf('.') + 1)
                            .toLowerCase(Locale.ROOT)
                            .replaceAll("[^a-z0-9]", "");
                    yield candidate.isBlank() ? "bin" : candidate;
                }
                yield "bin";
            }
        };
    }
}
