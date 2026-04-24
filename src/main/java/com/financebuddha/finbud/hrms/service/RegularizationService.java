package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.attendance.RegularizationRequestDto;
import com.financebuddha.finbud.hrms.dto.attendance.RegularizationResponse;
import com.financebuddha.finbud.hrms.dto.attendance.RegularizationReviewRequest;
import com.financebuddha.finbud.hrms.security.UserPrincipal;

import java.util.List;

public interface RegularizationService {

    /** Employee files a regularization for one of their own days. */
    RegularizationResponse submit(RegularizationRequestDto request, UserPrincipal principal);

    /** Employee cancels their own pending request. */
    void cancelOwnRequest(Long id, UserPrincipal principal);

    /** Approve or reject a request. TL sees only direct-report requests; HR/Admin see all. */
    RegularizationResponse review(Long id, RegularizationReviewRequest request, UserPrincipal principal);

    /** History for the currently authenticated employee. */
    List<RegularizationResponse> listMyRequests(UserPrincipal principal);

    /** Pending queue for the caller (scoped by role). */
    List<RegularizationResponse> listPendingForApprover(UserPrincipal principal);

    RegularizationResponse getById(Long id, UserPrincipal principal);
}
