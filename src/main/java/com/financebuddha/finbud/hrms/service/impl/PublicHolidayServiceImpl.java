package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.attendance.PublicHolidayRequest;
import com.financebuddha.finbud.hrms.dto.attendance.PublicHolidayResponse;
import com.financebuddha.finbud.hrms.entity.PublicHoliday;
import com.financebuddha.finbud.hrms.exception.DuplicateResourceException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.PublicHolidayMapper;
import com.financebuddha.finbud.hrms.repository.PublicHolidayRepository;
import com.financebuddha.finbud.hrms.service.PublicHolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicHolidayServiceImpl implements PublicHolidayService {

    private final PublicHolidayRepository publicHolidayRepository;
    private final PublicHolidayMapper publicHolidayMapper;

    @Override
    @Transactional
    public PublicHolidayResponse create(PublicHolidayRequest request) {
        if (publicHolidayRepository.existsByHolidayDate(request.getHolidayDate())) {
            throw new DuplicateResourceException(
                    "Public holiday already exists for " + request.getHolidayDate());
        }

        PublicHoliday entity = PublicHoliday.builder()
                .holidayDate(request.getHolidayDate())
                .name(request.getName().trim())
                .description(request.getDescription())
                .isOptional(Boolean.TRUE.equals(request.getIsOptional()))
                .build();

        PublicHoliday saved = publicHolidayRepository.save(entity);
        log.info("Created public holiday id={} date={}", saved.getId(), saved.getHolidayDate());
        return publicHolidayMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PublicHolidayResponse update(Long id, PublicHolidayRequest request) {
        PublicHoliday entity = publicHolidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PublicHoliday", "id", id));

        // If date is changing, ensure no other row already holds the new date
        if (!entity.getHolidayDate().equals(request.getHolidayDate())) {
            Optional<PublicHoliday> conflict = publicHolidayRepository.findByHolidayDate(request.getHolidayDate());
            if (conflict.isPresent() && !conflict.get().getId().equals(id)) {
                throw new DuplicateResourceException(
                        "Public holiday already exists for " + request.getHolidayDate());
            }
        }

        entity.setHolidayDate(request.getHolidayDate());
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        if (request.getIsOptional() != null) {
            entity.setIsOptional(request.getIsOptional());
        }

        PublicHoliday saved = publicHolidayRepository.save(entity);
        log.info("Updated public holiday id={}", saved.getId());
        return publicHolidayMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!publicHolidayRepository.existsById(id)) {
            throw new ResourceNotFoundException("PublicHoliday", "id", id);
        }
        publicHolidayRepository.deleteById(id);
        log.info("Deleted public holiday id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicHolidayResponse getById(Long id) {
        return publicHolidayRepository.findById(id)
                .map(publicHolidayMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("PublicHoliday", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicHolidayResponse> listAll() {
        return publicHolidayMapper.toResponseList(
                publicHolidayRepository.findAllByOrderByHolidayDateAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicHolidayResponse> listByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        return publicHolidayMapper.toResponseList(
                publicHolidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(startDate, endDate));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicHolidayResponse> listByYear(int year) {
        LocalDate start = LocalDate.of(year, Month.JANUARY, 1);
        LocalDate end = LocalDate.of(year, Month.DECEMBER, 31);
        return publicHolidayMapper.toResponseList(
                publicHolidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(start, end));
    }
}
