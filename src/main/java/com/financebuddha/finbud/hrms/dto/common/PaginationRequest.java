package com.financebuddha.finbud.hrms.dto.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PaginationRequest {

    @Min(0)
    private int page = 0;

    /**
     * Upper bound raised from 100 → 2000 so the Employees UI can fetch the
     * entire roster in a single request while proper pagination is still on
     * the roadmap. A finbud-sized directory (~430 active employees today,
     * headroom up to a few thousand) fits comfortably; anything beyond this
     * should paginate.
     */
    @Min(1)
    @Max(2000)
    private int size = 20;

    private String sortBy = "id";

    private String sortDirection = "desc";
}
