package com.financebuddha.finbud.hrms.dto.attendance;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OfficeLocationRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String address;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitude;

    @Positive(message = "Geofence radius must be positive")
    private Integer geofenceRadiusMeters;

    private Boolean enforceGeofence;

    private Boolean isActive;
}
