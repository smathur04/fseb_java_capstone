package assembly.general.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "books",
        indexes = {
                @Index(
                        name = "idx_books_title",
                        columnList = "title"
                ),
                @Index(
                        name = "idx_books_author",
                        columnList = "author"
                ),
                @Index(
                        name = "idx_books_genre",
                        columnList = "genre"
                ),
                @Index(
                        name = "idx_books_publication_year",
                        columnList = "publication_year"
                ),
                @Index(
                        name = "idx_books_available_copies",
                        columnList = "available_copies"
                )
        }
)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            unique = true,
            nullable = false,
            length = 20
    )
    private String isbn;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String genre;

    @Min(0)
    @Column(
            name = "publication_year",
            nullable = false
    )
    private Integer publicationYear;

    @Column(
            nullable = false,
            length = 500
    )
    private String description;

    @Column(nullable = false)
    private String publisher;

    @Min(0)
    @Column(nullable = false)
    private Integer pageCount;

    @Column(
            nullable = false,
            length = 50
    )
    private String language;

    @Min(0)
    @Column(nullable = false)
    private Integer totalCopies;

    @Min(0)
    @Column(
            name = "available_copies",
            nullable = false
    )
    private Integer availableCopies;

    @CreatedDate
    @Column(
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Book() {
    }

    public Book(
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
            Integer availableCopies
    ) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.publicationYear = publicationYear;
        this.description = description;
        this.publisher = publisher;
        this.pageCount = pageCount;
        this.language = language;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public UUID getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(Integer totalCopies) {
        this.totalCopies = totalCopies;
    }

    public Integer getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(Integer availableCopies) {
        this.availableCopies = availableCopies;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void checkOutBook() {
        if (availableCopies == 0) {
            throw new IllegalStateException(
                    "No copies available to check out."
            );
        }

        availableCopies--;
    }

    public void returnBook() {
        if (Objects.equals(
                availableCopies,
                totalCopies
        )) {
            throw new IllegalStateException(
                    "Available copies cannot exceed total copies."
            );
        }

        availableCopies++;
    }
}