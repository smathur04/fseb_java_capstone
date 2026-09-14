package assembly.general.api.dto;

import java.time.Instant;
import java.util.UUID;

public record BookDetailResponse(
        UUID bookId,
        String isbn,
        String title,
        String author,
        String genre,
        Integer publicationYear,
        String description,
        String publisher,
        Integer pageCount,
        String language,
        Integer totalCopies,
        Integer availableCopies,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}