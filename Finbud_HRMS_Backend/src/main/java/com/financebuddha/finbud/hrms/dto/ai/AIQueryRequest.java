package com.financebuddha.finbud.hrms.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AIQueryRequest {

    @NotBlank(message = "Query is required")
    private String query;

    private String context; // Optional context like department, date range, etc.
}
