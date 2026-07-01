package saas.com.br.resume_ai_saas.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase.storage")
public record SupabaseStorageProperties(
        String endpoint,
        String region,
        String accessKeyId,
        String secretAccessKey,
        String bucket
) {}
