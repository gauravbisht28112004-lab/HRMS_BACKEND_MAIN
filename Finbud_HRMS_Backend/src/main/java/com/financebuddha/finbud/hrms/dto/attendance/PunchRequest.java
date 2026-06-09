package com.financebuddha.finbud.hrms.dto.attendance;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Punch-in / punch-out request from the portal. The employee is resolved
 * from the JWT (never trust a client-supplied id), and the timestamp is
 * set server-side to prevent clock tampering. The browser only sends the
 * optional geo-coordinates and a short note.
 */
@Data
public class PunchRequest {

    @DecimalMin(value = "-90.0", message = "Latitude out of range")
    @DecimalMax(value = "90.0", message = "Latitude out of range")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude out of range")
    @DecimalMax(value = "180.0", message = "Longitude out of range")
    private BigDecimal longitude;

    /** Reported GPS accuracy in metres; useful when debugging out-of-geofence rejections. */
    @DecimalMin(value = "0.0", message = "Accuracy must be non-negative")
    private BigDecimal accuracyMeters;

    /** Short human-readable label from the browser (e.g. "Near Andheri West"). */
    private String locationLabel;

    private String deviceId;

    private String notes;
}
