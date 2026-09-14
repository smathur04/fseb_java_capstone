package assembly.general.api.repository;

import assembly.general.api.entity.Book;
import org.springframework.data.jpa.domain.Specification;

public class BookSpecifications {

    private BookSpecifications() {
    }

    public static Specification<Book> titleOrAuthorContains(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String pattern = "%" + query.toLowerCase() + "%";
        return (root, cq, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("author")), pattern)
        );
    }

    public static Specification<Book> hasGenre(String genre) {
        if (genre == null || genre.isBlank()) {
            return null;
        }
        return (root, cq, cb) -> cb.equal(root.get("genre"), genre);
    }

    public static Specification<Book> hasIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return null;
        }
        return (root, cq, cb) -> cb.equal(root.get("isbn"), isbn);
    }

    public static Specification<Book> isAvailable(boolean availableOnly) {
        if (!availableOnly) {
            return null;
        }
        return (root, cq, cb) -> cb.greaterThan(root.get("availableCopies"), 0);
    }
}