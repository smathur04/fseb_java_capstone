package assembly.general.api.service;

import assembly.general.api.dto.BookDetailResponse;
import assembly.general.api.dto.BookSummaryResponse;
import assembly.general.api.entity.Book;
import assembly.general.api.exception.ResourceNotFoundException;
import assembly.general.api.mapper.BookMapper;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.BookSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    public Page<BookSummaryResponse> searchBooks(String query, String genre, String isbn,
                                                 boolean availableOnly, Pageable pageable) {
        Specification<Book> spec = Specification.allOf(
                BookSpecifications.titleOrAuthorContains(query),
                BookSpecifications.hasGenre(genre),
                BookSpecifications.hasIsbn(isbn),
                BookSpecifications.isAvailable(availableOnly)
        );

        return bookRepository.findAll(spec, pageable)
                .map(bookMapper::toSummary);
    }

    public BookDetailResponse getBookById(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
        return bookMapper.toDetail(book);
    }
}