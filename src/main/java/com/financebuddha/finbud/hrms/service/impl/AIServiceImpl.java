package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.ai.AIQueryRequest;
import com.financebuddha.finbud.hrms.dto.ai.AIQueryResponse;
import com.financebuddha.finbud.hrms.entity.*;
import com.financebuddha.finbud.hrms.repository.*;
import com.financebuddha.finbud.hrms.service.AIService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final OpenAiService openAiService;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final PayrollRepository payrollRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AiEmbeddingRepository aiEmbeddingRepository;

    @Value("${openai.api.model}")
    private String model;

    @Override
    public AIQueryResponse processQuery(AIQueryRequest request) {
        log.info("Processing AI query: {}", request.getQuery());
        long startTime = System.currentTimeMillis();

        // Analyze query type
        String queryType = analyzeQueryType(request.getQuery());
        String context = buildContext(request.getQuery(), queryType);

        // Create chat completion request
        ChatMessage systemMessage = new ChatMessage("system",
                "You are an HR assistant for Finbud Financial. You help with employee data queries. " +
                "Answer based on the provided context. Be concise and accurate.");
        ChatMessage userMessage = new ChatMessage("user",
                "Context: " + context + "\n\nQuestion: " + request.getQuery());

        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(systemMessage, userMessage))
                .maxTokens(500)
                .temperature(0.7)
                .build();

        try {
            ChatCompletionResult result = openAiService.createChatCompletion(completionRequest);
            String response = result.getChoices().get(0).getMessage().getContent();

            long responseTime = System.currentTimeMillis() - startTime;

            return AIQueryResponse.builder()
                    .query(request.getQuery())
                    .response(response)
                    .dataType(queryType)
                    .tokensUsed(result.getUsage().getTotalTokens())
                    .responseTimeMs(responseTime)
                    .build();

        } catch (Exception e) {
            log.error("Error processing AI query: {}", e.getMessage());
            return AIQueryResponse.builder()
                    .query(request.getQuery())
                    .response("Sorry, I couldn't process your query. Please try again.")
                    .dataType("error")
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    public void indexEmployeeData(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) return;

        String content = String.format("Employee: %s, ID: %s, Department: %s, Designation: %s, Status: %s",
                employee.getFullName(), employee.getEmployeeId(),
                employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A",
                employee.getDesignation(), employee.getStatus());

        createEmbedding("employee", employeeId, content);
    }

    @Override
    public void indexAttendanceData(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId).orElse(null);
        if (attendance == null) return;

        String content = String.format("Attendance for %s on %s: Status: %s, Late: %s, Overtime: %s hours",
                attendance.getEmployee().getFullName(), attendance.getAttendanceDate(),
                attendance.getStatus(), attendance.getIsLate(), attendance.getOvertimeHours());

        createEmbedding("attendance", attendanceId, content);
    }

    @Override
    public void indexPayrollData(Long payrollId) {
        Payroll payroll = payrollRepository.findById(payrollId).orElse(null);
        if (payroll == null) return;

        String content = String.format("Payroll for %s - %s/%s: Net Pay: %s, Status: %s",
                payroll.getEmployee().getFullName(), payroll.getMonth(), payroll.getYear(),
                payroll.getNetPay(), payroll.getStatus());

        createEmbedding("payroll", payrollId, content);
    }

    @Override
    public void indexLeaveData(Long leaveId) {
        LeaveRequest leave = leaveRequestRepository.findById(leaveId).orElse(null);
        if (leave == null) return;

        String content = String.format("Leave for %s: Type: %s, From: %s to %s, Days: %s, Status: %s",
                leave.getEmployee().getFullName(), leave.getLeaveType(),
                leave.getStartDate(), leave.getEndDate(), leave.getDaysRequested(), leave.getStatus());

        createEmbedding("leave", leaveId, content);
    }

    private void createEmbedding(String entityType, Long entityId, String content) {
        try {
            EmbeddingRequest embeddingRequest = EmbeddingRequest.builder()
                    .model("text-embedding-3-small")
                    .input(List.of(content))
                    .build();

            var embeddings = openAiService.createEmbeddings(embeddingRequest).getData();
            if (!embeddings.isEmpty()) {
                List<Double> embeddingValues = embeddings.get(0).getEmbedding();
                float[] embeddingArray = new float[embeddingValues.size()];
                for (int i = 0; i < embeddingValues.size(); i++) {
                    embeddingArray[i] = embeddingValues.get(i).floatValue();
                }

                AiEmbedding aiEmbedding = AiEmbedding.builder()
                        .entityType(entityType)
                        .entityId(entityId)
                        .content(content)
                        .embedding(embeddingArray)
                        .build();

                aiEmbeddingRepository.save(aiEmbedding);
            }
        } catch (Exception e) {
            log.error("Error creating embedding: {}", e.getMessage());
        }
    }

    private String analyzeQueryType(String query) {
        String lowerQuery = query.toLowerCase();
        if (lowerQuery.contains("attendance") || lowerQuery.contains("present") || lowerQuery.contains("late") || lowerQuery.contains("absent")) {
            return "attendance";
        } else if (lowerQuery.contains("payroll") || lowerQuery.contains("salary") || lowerQuery.contains("pay")) {
            return "payroll";
        } else if (lowerQuery.contains("leave") || lowerQuery.contains("vacation") || lowerQuery.contains("holiday")) {
            return "leave";
        } else if (lowerQuery.contains("employee") || lowerQuery.contains("staff") || lowerQuery.contains("team")) {
            return "employee";
        }
        return "general";
    }

    private String buildContext(String query, String queryType) {
        StringBuilder context = new StringBuilder();

        switch (queryType) {
            case "attendance" -> {
                LocalDate today = LocalDate.now();
                List<Attendance> todayAttendance = attendanceRepository.findByAttendanceDate(today);
                context.append("Today's attendance records: ").append(todayAttendance.size()).append(" entries. ");

                List<Attendance> lateComers = attendanceRepository.findLateComersByDate(today);
                context.append("Late comers today: ").append(lateComers.size()).append(". ");
            }
            case "payroll" -> {
                // Add payroll context
                context.append("Payroll system information. ");
            }
            case "leave" -> {
                // Add leave context
                context.append("Leave management system. ");
            }
            case "employee" -> {
                long employeeCount = employeeRepository.count();
                context.append("Total employees: ").append(employeeCount).append(". ");
            }
            default -> context.append("General HR information. ");
        }

        return context.toString();
    }

    @Override
    public String generatePayrollSummary(Integer month, Integer year) {
        return "Payroll summary for " + month + "/" + year + ": Generated via AI";
    }

    @Override
    public String getLateComersReport(LocalDate date) {
        List<Attendance> lateComers = attendanceRepository.findLateComersByDate(date);
        return "Late comers on " + date + ": " + lateComers.size();
    }

    @Override
    public String getAbsentReport(LocalDate date) {
        List<Attendance> absent = attendanceRepository.findAbsentByDate(date);
        return "Absent employees on " + date + ": " + absent.size();
    }

    @Override
    public String getOvertimeReport(Integer month, Integer year) {
        return "Overtime report for " + month + "/" + year + ": Generated";
    }
}
