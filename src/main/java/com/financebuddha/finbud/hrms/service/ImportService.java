package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.imports.ImportResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ImportService {

    ImportResponse importEmployees(MultipartFile file) throws Exception;
}
