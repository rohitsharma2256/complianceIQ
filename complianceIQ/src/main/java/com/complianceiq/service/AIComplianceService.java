package com.complianceiq.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class AIComplianceService {

    private final ChatClient chatClient;
    private final RagService ragService;
    private final ToolCallbackProvider toolCallbackProvider;

    private static final String SYSTEM_PROMPT = """
            You are an expert Indian payroll compliance assistant
            with deep knowledge of:
            - EPF Act 1952: Employee 12% + Employer 12% of basic salary
              Due date: 15th of following month
            - ESI Act 1948: Employee 0.75% + Employer 3.25%
              Applicable if salary <= Rs 21,000/month
              Due date: 15th of following month
            - Labour Code 2025: Basic salary must be 50% of total CTC
            - Income Tax Act 2025: TDS as per new regime slabs
              Standard deduction Rs 75,000
              Due date: 7th of following month
            - Professional Tax: State-wise rates
              Maharashtra: Rs 200/month (above Rs 10,000)
            - Labour Welfare Fund: State-wise

            You have access to tools that can check compliance,
            fetch violations, and list companies. Use these tools
            when the user asks you to perform an action.

            Always provide:
            - Clear explanation in simple English
            - Exact calculations where possible
            - Relevant law/section reference
            - Practical fix or recommendation
            """;

    public AIComplianceService(ChatClient.Builder chatClientBuilder,
                               RagService ragService,
                               ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
        this.ragService = ragService;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    // Normal question — LLM memory se
    public String askComplianceQuestion(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    // RAG question — actual law documents se
    public String askWithRAG(String question) {
        String relevantLaw = ragService.searchRelevantLaw(question);

        String prompt = String.format("""
                Use the following Indian law reference to answer
                the question accurately. If the reference contains
                the answer, use it and cite the source.

                LAW REFERENCE:
                %s

                QUESTION:
                %s
                """, relevantLaw, question);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    // AGENTIC — AI khud tools call karega (actions leta hai)
    public String askAgentic(String question) {
        return chatClient.prompt()
                .user(question)
                .toolCallbacks(toolCallbackProvider)
                .call()
                .content();
    }

    public String analyzeViolation(String employeeName,
                                   String violationType,
                                   String description) {
        String prompt = String.format("""
                Analyze this payroll compliance violation:

                Employee: %s
                Violation Type: %s
                Details: %s

                Provide:
                1. Why this is a violation (with law reference)
                2. Exact fix required
                3. Penalty risk if not fixed
                4. Step by step correction process
                """, employeeName, violationType, description);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    public String generateComplianceSummary(
            int totalEmployees,
            String epfTotal,
            String esiTotal,
            String tdsTotal,
            String ptTotal,
            int violationCount) {

        String prompt = String.format("""
                Generate a professional monthly payroll compliance
                summary for CA review:

                Total Employees: %d
                Total EPF: %s
                Total ESI: %s
                Total TDS: %s
                Total Professional Tax: %s
                Violations Found: %d

                Provide compliance status, deadlines, action items.
                """,
                totalEmployees, epfTotal, esiTotal,
                tdsTotal, ptTotal, violationCount);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}