package assembly.general.api.controllers;

import assembly.general.api.dto.BookDetailResponse;
import assembly.general.api.dto.BookSummaryResponse;
import assembly.general.api.dto.PageResponse;
import assembly.general.api.service.BookService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/books")
    public ResponseEntity<PageResponse<BookSummaryResponse>> getBooks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String isbn,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder
    ) {
        PageResponse<BookSummaryResponse> books =
                bookService.searchBooks(
                        query,
                        genre,
                        isbn,
                        availableOnly,
                        page,
                        size,
                        sortBy,
                        sortOrder
                );

        return ResponseEntity.ok(books);
    }

    @GetMapping("/books/{bookId}")
    public ResponseEntity<BookDetailResponse> getBookById(
            @PathVariable UUID bookId
    ) {
        BookDetailResponse book =
                bookService.getBookById(bookId);

        return ResponseEntity.ok(book);
    }
}