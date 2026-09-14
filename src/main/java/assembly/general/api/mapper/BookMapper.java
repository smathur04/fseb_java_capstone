package assembly.general.api.mapper;

import assembly.general.api.dto.BookDetailResponse;
import assembly.general.api.dto.BookSummaryResponse;
import assembly.general.api.entity.Book;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public BookSummaryResponse toSummary(Book book) {
        return new BookSummaryResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getPublicationYear(),
                book.getDescription(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                calculateStatus(book)
        );
    }

    public BookDetailResponse toDetail(Book book) {
        return new BookDetailResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getPublicationYear(),
                book.getDescription(),
                book.getPublisher(),
                book.getPageCount(),
                book.getLanguage(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                calculateStatus(book),
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }

    private String calculateStatus(Book book) {
        return book.getAvailableCopies() > 0 ? "AVAILABLE" : "CHECKED_OUT";
    }
}