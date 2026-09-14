package assembly.general.api.dto;

import java.util.UUID;

public record BookSummaryResponse(
        UUID bookId,
        String isbn,
        String title,
        String author,
        String genre,
        Integer publicationYear,
        String description,
        Integer totalCopies,
        Integer availableCopies,
        String status
) {
}