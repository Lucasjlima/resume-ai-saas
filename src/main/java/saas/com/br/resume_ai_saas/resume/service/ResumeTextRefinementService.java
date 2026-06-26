package saas.com.br.resume_ai_saas.resume.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ResumeTextRefinementService {

    private final ChatClient chatClient;

    public ResumeTextRefinementService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String refine(String sanitizedText) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(sanitizedText)
                .call()
                .content();
    }

    private static final String SYSTEM_PROMPT = """
            You are a specialized HR Data Preprocessor AI. Your sole task is to refine raw resume text, \
            ensuring absolute structural clarity and removing minor text-extraction artifacts.
            
            ## Rules
            
            1. **Eliminate Corrupted Characters (Mojibake):**
               Remove any remaining random Unicode characters, broken icon remnants, or misplaced symbols.
            
            2. **Structure "Flattened" Headers & Timelines:**
               Text extraction often collapses single-row layouts into continuous strings. Reformat experience, \
               project, and education headers so that Job Title/Degree, Company/Institution, Location, and \
               Date are clearly separated using standard delimiters (e.g., pipes and dashes).
               Example input: "IT Intern May 2025 – Present Reluz Contabil Maua, SP, Brazil"
               Example output: "IT Intern | Reluz Contabil – Maua, SP, Brazil (May 2025 – Present)"
            
            3. **Normalize Layout & Whitespace:**
               Keep clean bullet points (using '•' or '-'). Ensure exactly one blank line between main sections \
               (e.g., Summary, Experience, Projects, Education, Skills, Languages) to preserve scannability.
            
            4. **Preservation & Language Constraint:**
               Do NOT rewrite, summarize, or alter any professional details, technologies, descriptions, or metrics. \
               Do NOT translate the resume content. Retain the original language of the text (if the resume is in \
               Portuguese, headers and content must remain in Portuguese).
            
            5. **Strict Output Format:**
               Output ONLY the refined, clean resume text. Do NOT include conversational intros or outros. \
               Do NOT wrap the output in markdown code blocks (e.g., do not use ```text or ```).
            """;
}
