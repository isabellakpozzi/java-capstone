package assembly.general.api.dto;

import assembly.general.api.entity.Book;
import lombok.Getter;

import java.util.UUID;

@Getter
public class BookSummaryResponse {
    private final UUID bookId;
    private final String isbn;
    private final String title;
    private final String author;
    private final String genre;
    private final Integer publicationYear;
    private final String description;
    private final Integer totalCopies;
    private final Integer availableCopies;
    private final String status;

    public BookSummaryResponse(Book book) {
        this.bookId = book.getId();
        this.isbn = book.getIsbn();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.genre = book.getGenre();
        this.publicationYear = book.getPublicationYear();
        this.description = book.getDescription();
        this.totalCopies = book.getTotalCopies();
        this.availableCopies = book.getAvailableCopies();
        this.status = book.getStatus(); // derived getter already on the entity
    }
}