package saas.com.br.resume_ai_saas.regeneration.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Service;
import saas.com.br.resume_ai_saas.analyse.dto.response.Feedback;
import saas.com.br.resume_ai_saas.regeneration.dto.response.GeneratedResume;

@Service
public class AiResumeRegenerationService {

    private final ChatClient chatClient;

    public AiResumeRegenerationService(ChatClient.Builder chatClientBuilder) {
        GoogleGenAiChatOptions.Builder options = GoogleGenAiChatOptions.builder()
                .model("gemini-2.5-flash")
                .responseMimeType("application/json")
                .temperature(0.1);

        this.chatClient = chatClientBuilder
                .defaultOptions(options)
                .build();
    }

    public GeneratedResume regenerate(String resumeText, Feedback feedback, String jobDescription) {
        String promptTemplate = """
                You are an expert resume writer and career coach.
                Rewrite the resume below into a structured JSON document, incorporating the
                improvement feedback it received for the target job description.

                REWRITING RULES:
                - Rewrite "resumo_profissional" and the experience bullets to address the
                  improvements and gaps listed in the feedback, highlighting the strengths.
                - NEVER invent facts: no new employers, roles, dates, degrees, certifications,
                  skills or tools that are not present in the original resume.
                - Keep ALL real information from the original resume; improve wording only.
                - Dates: copy them exactly as written in the original resume (free-form text,
                  e.g. 'Jan 2020', '01/2020'). Do NOT normalize or convert formats.
                - Sections with no content in the original resume MUST be returned as an
                  empty array []. NEVER fill them with invented content.
                - Write in the SAME LANGUAGE used in the original resume.

                SINGLE-PAGE CONSTRAINT (the output is typeset into a one-page PDF):
                - "resumo_profissional": at most 3 short sentences.
                - Bullets: concise, at most ~20 words each, and at most 4 bullets per
                  experience — condense wording instead of listing every detail, but
                  NEVER drop an employer, role, date or section to save space.

                OUTPUT: respond ONLY with the JSON object, following the requested schema.

                ORIGINAL RESUME:
                {resumeText}

                JOB DESCRIPTION:
                {jobDescription}

                FEEDBACK — STRENGTHS:
                {strengths}

                FEEDBACK — GAPS:
                {gaps}

                FEEDBACK — IMPROVEMENTS TO INCORPORATE:
                {improvements}
                """;

        return chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(promptTemplate)
                        .param("resumeText", resumeText)
                        .param("jobDescription", jobDescription == null ? "" : jobDescription)
                        .param("strengths", String.join("\n", feedback.strengths()))
                        .param("gaps", String.join("\n", feedback.gaps()))
                        .param("improvements", String.join("\n", feedback.improvements()))
                )
                .call()
                .entity(GeneratedResume.class);
    }
}
