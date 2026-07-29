package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Physical office an employee reports to. Holds the optional geofence
 * (latitude + longitude + radius) that the attendance service uses to
 * reject out-of-range punches when {@link #enforceGeofence} is true.
 *
 * <p>One row is seeded by V11 ("Finbud HQ") with enforceGeofence=false so
 * existing deployments keep working until HR explicitly configures it.</p>
 */
@Entity
@Table(name = "office_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OfficeLocation extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "geofence_radius_meters", nullable = false)
    @Builder.Default
    private Integer geofenceRadiusMeters = 100;

    @Column(name = "enforce_geofence", nullable = false)
    @Builder.Default
    private Boolean enforceGeofence = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
