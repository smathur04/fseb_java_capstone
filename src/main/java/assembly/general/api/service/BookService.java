package assembly.general.api.service;

import assembly.general.api.dto.BookDetailResponse;
import assembly.general.api.dto.BookSummaryResponse;
import assembly.general.api.dto.PageResponse;
import assembly.general.api.entity.Book;
import assembly.general.api.exception.ResourceNotFoundException;
import assembly.general.api.mapper.BookMapper;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.BookSpecifications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BookService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "title",
                    "author",
                    "publicationYear"
            );

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    public BookService(
            BookRepository bookRepository,
            BookMapper bookMapper
    ) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    public PageResponse<BookSummaryResponse> searchBooks(
            String query,
            String genre,
            String isbn,
            boolean availableOnly,
            int page,
            int size,
            String sortBy,
            String sortOrder
    ) {
        validatePagination(page, size);
        validateSort(sortBy, sortOrder);

        Sort.Direction direction =
                sortOrder.equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy)
        );

        Specification<Book> specification =
                Specification.allOf(
                        BookSpecifications.titleOrAuthorContains(query),
                        BookSpecifications.hasGenre(genre),
                        BookSpecifications.hasIsbn(isbn),
                        BookSpecifications.isAvailable(availableOnly)
                );

        Page<Book> books =
                bookRepository.findAll(
                        specification,
                        pageable
                );

        return new PageResponse<>(
                books.getContent()
                        .stream()
                        .map(bookMapper::toSummary)
                        .toList(),
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isLast()
        );
    }

    public BookDetailResponse getBookById(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Book not found with ID: " + bookId
                        )
                );

        return bookMapper.toDetail(book);
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size <= 0) {
            throw new IllegalArgumentException(
                    "Page size must be greater than zero"
            );
        }
    }

    private void validateSort(
            String sortBy,
            String sortOrder
    ) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "sortBy must be one of: title, author, publicationYear"
            );
        }

        if (!sortOrder.equalsIgnoreCase("asc")
                && !sortOrder.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException(
                    "sortOrder must be either asc or desc"
            );
        }
    }
}