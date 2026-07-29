package com.financebuddha.finbud.hrms.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIQueryResponse {

    private String query;
    private String response;
    private String dataType; // leave, payroll, employee
    private Object data; // Structured data if applicable
    private Integer tokensUsed;
    private Long responseTimeMs;
}
