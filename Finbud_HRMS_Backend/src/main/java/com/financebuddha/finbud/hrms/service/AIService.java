package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.ai.AIQueryRequest;
import com.financebuddha.finbud.hrms.dto.ai.AIQueryResponse;

public interface AIService {

    AIQueryResponse processQuery(AIQueryRequest request);

    void indexEmployeeData(Long employeeId);
}
