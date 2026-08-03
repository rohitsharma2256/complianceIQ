package com.complianceiq.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AIComplianceService {

    private final ChatClient agentClient;   // tools ke saath  (bhaari)
    private final ChatClient plainClient;   // tools ke bina   (halka)
    private final RagService ragService;

    /* ================= AGENT PROMPT (tools wale path ke liye) ================= */
    private static final String AGENT_PROMPT = """
            ComplianceIQ assistant for Indian payroll compliance.

            HARD RULES
            - You NEVER calculate a rupee figure. Tools and the rule engine do that.
              If you don't have a computed number, say so and name the action needed.
            - Call each tool at most ONCE per answer. Never repeat the same call.
            - If the question has no salary figures, use the PT slabs tool.
              Do NOT invent salary numbers to satisfy a tool.
            - If no tool fits, answer briefly from the facts below.

            FACTS (explain these, don't compute)
            - EPF 12% + 12% on Basic+DA, capped at the Rs 15,000 wage ceiling
              (a Rs 50,000 basic still gives Rs 1,800 each).
            - ESI 0.75% + 3.25%; eligibility on monthly GROSS wages, not CTC;
              threshold Rs 21,000.
            - Professional Tax is state-specific. Haryana, Delhi, UP, Rajasthan,
              Punjab and Uttarakhand levy none - a zero there is correct.
            - Loss of Pay reduces PF and ESI, but not the PT slab.
            - Missing UAN blocks ECR, missing ESIC IP blocks the ESI return,
              missing PAN blocks Form 16.

            Be concise and practical.
            """;

    /* ================= PLAIN PROMPT (no tools, no RAG) ================= */
    private static final String PLAIN_PROMPT = """
            You are an Indian payroll compliance assistant for chartered accountants.
            Explain concepts clearly in simple English, under 150 words.
            Do NOT state specific statutory rates or amounts from memory - rates change
            by financial year. If asked for a rate, say it should be verified against
            the configured rules or the official portal.
            """;

    public AIComplianceService(ChatClient.Builder builder,
                               RagService ragService,
                               ToolCallbackProvider toolCallbackProvider) {

        // Tools SIRF yahan register hote hain - kahin aur .toolCallbacks() mat lagana
        this.agentClient = builder.clone()
                .defaultSystem(AGENT_PROMPT)
                .defaultToolCallbacks(toolCallbackProvider)
                .build();

        this.plainClient = builder.clone()
                .defaultSystem(PLAIN_PROMPT)
                .build();

        this.ragService = ragService;
    }

    /* ==================================================================
       ROUTER - single entry point. Keyword based, 0 tokens.
       ================================================================== */
    public String ask(String question) {
        if (question == null || question.isBlank())
            return "Please type a question.";

        String q = question.toLowerCase();

        // LANE 3 - user ke apne data ka sawaal -> tools
        if (has(q, "my ", "our ", "this company", "attendance", "violation",
                "compliance check", "employees", "companies", "this month",
                "ready to file", "pending", "blocker"))
            return askAgentic(question);

        // LANE 4 - draft / explain request
        if (has(q, "draft", "explain to", "write to", "letter", "email to",
                "tell my client", "in simple"))
            return askPlain(question);

        // ===== NAYA: PT + state ka sawaal -> DB tool, RAG nahi =====
        // DB mein saare 20+ states hain (Haryana included, applicable=false).
        // RAG documents mein sirf kuch states hain - isliye DB authoritative hai.
        if (has(q, "professional tax", " pt ", "pt of", "pt in", "pt slab")
                || (has(q, "lwf", "labour welfare")))
            return askAgentic(question);

        // LANE 2 - baaki statutory (EPF/ESI/TDS) -> RAG citation ke saath
        if (has(q, "rate", "slab", "limit", "ceiling", "threshold", "due date",
                "percentage", "how much", "epf", "esi", "pf ", "tds",
                "gratuity", "bonus", "section", "act", "penalty", "notification"))
            return askWithRAG(question);

        // LANE 1 - concept
        return askPlain(question);
    }

    /* ==================================================================
       LANE 1 & 4 - plain LLM (sabse sasta)
       ================================================================== */
    public String askPlain(String question) {
        try {
            return plainClient.prompt().user(question).call().content();
        } catch (Exception e) {
            log.error("Plain call failed", e);
            return friendlyError(e);
        }
    }

    /** Backward compatible alias */
    public String askComplianceQuestion(String question) {
        return askPlain(question);
    }

    /* ==================================================================
       LANE 2 - RAG with mandatory citation
       ================================================================== */
    public String askWithRAG(String question) {
        try {
            String law = ragService.searchRelevantLaw(question);

            // Verified source nahi mila -> jhooth mat bolo
            if (law == null || law.isBlank() || law.startsWith("No relevant law")
                    || law.startsWith("Could not search")) {
                return "I don't have a verified source for that in the knowledge base. "
                        + "Add the relevant notification under 'Add Law Update', or check "
                        + "the official EPFO / ESIC / state portal before relying on it.";
            }

            String prompt = """
                    REFERENCE (the only source you may use):
                    %s

                    QUESTION: %s

                    Rules:
                    1. Answer ONLY from the reference above.
                    2. Cite the source for every factual claim, like [Source: <file>].
                    3. If the reference does not cover it, say exactly:
                       "This is not covered in the current knowledge base."
                    4. Do NOT compute any rupee amount - quote rates and slabs only.
                    5. Under 150 words. Mention the effective date if present.
                    """.formatted(law, question);

            return plainClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.error("RAG call failed", e);
            return friendlyError(e);
        }
    }

    /* ==================================================================
       LANE 3 - agentic. Tools constructor mein lage hain, dobara mat lagana.
       429 pe Retry-After padh ke wait, tool loop pe plain fallback.
       ================================================================== */
    public String askAgentic(String question) {
        int attempt = 0;
        while (attempt < 3) {
            try {
                return agentClient.prompt().user(question).call().content();

            } catch (Exception e) {
                String m = e.getMessage() == null ? "" : e.getMessage();

                // Model tool call mein fas gaya -> bina tools ke jawab do
                if (m.contains("tool_use_failed") || m.contains("invalid_request_error")) {
                    log.warn("Tool call failed, falling back to plain answer");
                    try {
                        return plainClient.prompt().user(question).call().content();
                    } catch (Exception ignored) {
                        return "I couldn't process that. Try rephrasing it, or use the "
                                + "quick actions above - they run instantly.";
                    }
                }

                boolean rateLimited = m.contains("rate_limit") || m.contains("429");
                if (rateLimited && attempt < 2) {
                    attempt++;
                    try {
                        Thread.sleep(extractWaitMillis(m));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                log.error("Agentic call failed", e);
                return friendlyError(e);
            }
        }
        return "The AI is busy right now. Please try again in a minute - "
                + "the quick actions above still work instantly.";
    }

    /* ==================================================================
       Existing helpers (plain client use karte hain - saste)
       ================================================================== */
    public String analyzeViolation(String employeeName, String violationType,
                                   String description) {
        return askPlain("""
                Analyse this payroll compliance violation briefly:
                Employee: %s | Type: %s
                Details: %s

                Give: why it is a violation, the exact fix, and the penalty risk.
                Do not compute any new amounts.
                """.formatted(employeeName, violationType, description));
    }

    public String generateComplianceSummary(int totalEmployees, String epfTotal,
                                            String esiTotal, String tdsTotal,
                                            String ptTotal, int violationCount) {
        return askPlain("""
                Write a short monthly payroll compliance summary for a CA.
                These figures are already computed - use them as given, do not recalculate:
                Employees %d | EPF %s | ESI %s | TDS %s | PT %s | Violations %d

                Cover status, upcoming deadlines and action items. Under 200 words.
                """.formatted(totalEmployees, epfTotal, esiTotal, tdsTotal,
                ptTotal, violationCount));
    }

    /* ---------------- utils ---------------- */
    private boolean has(String text, String... keys) {
        for (String k : keys) if (text.contains(k)) return true;
        return false;
    }

    /** Groq batata hai "try again in 9.32s" - wahi padh ke wait karo */
    private long extractWaitMillis(String message) {
        try {
            int idx = message.indexOf("try again in ");
            if (idx > 0) {
                String num = message.substring(idx + 13).split("s")[0].trim();
                return (long) (Double.parseDouble(num) * 1000) + 3000;
            }
        } catch (Exception ignored) { }
        return 15000;
    }

    private String friendlyError(Exception e) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        if (m.contains("rate_limit") || m.contains("429"))
            return "The AI is rate-limited right now. Please wait about half a minute "
                    + "and try again - the quick actions above work instantly.";
        if (m.contains("401") || m.contains("invalid_api_key"))
            return "The AI service is not configured. Please check the API key.";
        return "Something went wrong with the AI service. Please try again.";
    }
}