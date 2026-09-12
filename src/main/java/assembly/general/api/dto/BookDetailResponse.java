package assembly.general.api.dto;

import assembly.general.api.entity.Book;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class BookDetailResponse {
    private final UUID bookId;
    private final String isbn;
    private final String title;
    private final String author;
    private final String genre;
    private final Integer publicationYear;
    private final String description;
    private final String publisher;
    private final Integer pageCount;
    private final String language;
    private final Integer totalCopies;
    private final Integer availableCopies;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public BookDetailResponse(Book book) {
        this.bookId = book.getId();
        this.isbn = book.getIsbn();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.genre = book.getGenre();
        this.publicationYear = book.getPublicationYear();
        this.description = book.getDescription();
        this.publisher = book.getPublisher();
        this.pageCount = book.getPageCount();
        this.language = book.getLanguage();
        this.totalCopies = book.getTotalCopies();
        this.availableCopies = book.getAvailableCopies();
        this.status = book.getStatus();
        this.createdAt = book.getCreatedAt();
        this.updatedAt = book.getUpdatedAt();
    }
}