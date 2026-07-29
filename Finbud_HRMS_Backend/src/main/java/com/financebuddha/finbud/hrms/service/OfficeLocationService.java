package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.attendance.OfficeLocationRequest;
import com.financebuddha.finbud.hrms.dto.attendance.OfficeLocationResponse;

import java.util.List;

public interface OfficeLocationService {

    OfficeLocationResponse create(OfficeLocationRequest request);

    OfficeLocationResponse update(Long id, OfficeLocationRequest request);

    void delete(Long id);

    OfficeLocationResponse getById(Long id);

    List<OfficeLocationResponse> listAll();

    List<OfficeLocationResponse> listActive();
}
