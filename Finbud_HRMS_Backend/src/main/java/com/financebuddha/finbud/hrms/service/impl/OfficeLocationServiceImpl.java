package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.attendance.OfficeLocationRequest;
import com.financebuddha.finbud.hrms.dto.attendance.OfficeLocationResponse;
import com.financebuddha.finbud.hrms.entity.OfficeLocation;
import com.financebuddha.finbud.hrms.exception.DuplicateResourceException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.OfficeLocationMapper;
import com.financebuddha.finbud.hrms.repository.OfficeLocationRepository;
import com.financebuddha.finbud.hrms.service.OfficeLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfficeLocationServiceImpl implements OfficeLocationService {

    private final OfficeLocationRepository officeLocationRepository;
    private final OfficeLocationMapper officeLocationMapper;

    @Override
    @Transactional
    public OfficeLocationResponse create(OfficeLocationRequest request) {
        officeLocationRepository.findByName(request.getName().trim())
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Office location with name '" + request.getName() + "' already exists");
                });

        OfficeLocation entity = OfficeLocation.builder()
                .name(request.getName().trim())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .geofenceRadiusMeters(
                        request.getGeofenceRadiusMeters() != null ? request.getGeofenceRadiusMeters() : 100)
                .enforceGeofence(Boolean.TRUE.equals(request.getEnforceGeofence()))
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();

        OfficeLocation saved = officeLocationRepository.save(entity);
        log.info("Created office location id={} name={}", saved.getId(), saved.getName());
        return officeLocationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OfficeLocationResponse update(Long id, OfficeLocationRequest request) {
        OfficeLocation entity = officeLocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OfficeLocation", "id", id));


        // Enforce unique name
        String newName = request.getName().trim();
        Optional<OfficeLocation> conflict = officeLocationRepository.findByName(newName);
        if (conflict.isPresent() && !conflict.get().getId().equals(id)) {
            throw new DuplicateResourceException(
                    "Office location with name '" + newName + "' already exists");
        }

        entity.setName(newName);
        entity.setAddress(request.getAddress());
        entity.setLatitude(request.getLatitude());
        entity.setLongitude(request.getLongitude());
        if (request.getGeofenceRadiusMeters() != null) {
            entity.setGeofenceRadiusMeters(request.getGeofenceRadiusMeters());
        }
        if (request.getEnforceGeofence() != null) {
            entity.setEnforceGeofence(request.getEnforceGeofence());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }

        OfficeLocation saved = officeLocationRepository.save(entity);
        log.info("Updated office location id={}", saved.getId());
        return officeLocationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        OfficeLocation entity = officeLocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OfficeLocation", "id", id));
        // Soft-delete by deactivating to keep the FK intact on employees.office_location_id.
        entity.setIsActive(false);
        officeLocationRepository.save(entity);
        log.info("Deactivated office location id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public OfficeLocationResponse getById(Long id) {
        return officeLocationRepository.findById(id)
                .map(officeLocationMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("OfficeLocation", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfficeLocationResponse> listAll() {
        List<OfficeLocation> all = officeLocationRepository.findAll();
        all.sort(Comparator.comparing(OfficeLocation::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return officeLocationMapper.toResponseList(all);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfficeLocationResponse> listActive() {
        return officeLocationMapper.toResponseList(
                officeLocationRepository.findByIsActiveTrueOrderByNameAsc());
    }
}
