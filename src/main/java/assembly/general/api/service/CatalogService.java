package assembly.general.api.service;

import assembly.general.api.dto.BookDetailResponse;
import assembly.general.api.dto.BookSummaryResponse;
import assembly.general.api.dto.PagedResponse;
import assembly.general.api.entity.Book;
import assembly.general.api.exception.BookNotFoundException;
import assembly.general.api.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CatalogService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "author", "publicationYear");

    private final BookRepository bookRepository;

    public CatalogService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public PagedResponse<BookSummaryResponse> listBooks(
            int page, int size, String sortBy, String sortOrder,
            String query, String genre, String isbn, boolean availableOnly
    ) {
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "title";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, safeSortBy));

        String normalizedQuery = (query == null || query.isBlank()) ? null : query;
        String normalizedGenre = (genre == null || genre.isBlank()) ? null : genre;
        String normalizedIsbn = (isbn == null || isbn.isBlank()) ? null : isbn;

        Page<Book> result = bookRepository.search(
                normalizedQuery, normalizedGenre, normalizedIsbn, availableOnly, pageable
        );

        List<BookSummaryResponse> content = result.getContent().stream()
                .map(BookSummaryResponse::new)
                .toList();

        return new PagedResponse<>(content, result);
    }

    public BookDetailResponse getBookById(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + bookId));

        return new BookDetailResponse(book);
    }
}