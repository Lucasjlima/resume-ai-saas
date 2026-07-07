package saas.com.br.resume_ai_saas.resume.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable pagination envelope. Wrapping {@link Page} avoids serializing Spring's
 * internal {@code PageImpl} structure (which is not part of its API contract).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
