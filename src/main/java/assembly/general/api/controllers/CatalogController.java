package assembly.general.api.controllers;

import assembly.general.api.dto.BookDetailResponse;
import assembly.general.api.dto.BookSummaryResponse;
import assembly.general.api.dto.PagedResponse;
import assembly.general.api.service.CatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/books")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public PagedResponse<BookSummaryResponse> listBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String isbn,
            @RequestParam(defaultValue = "false") boolean availableOnly
    ) {
        return catalogService.listBooks(page, size, sortBy, sortOrder, query, genre, isbn, availableOnly);
    }

    @GetMapping("/{bookId}")
    public BookDetailResponse getBook(@PathVariable UUID bookId) {
        return catalogService.getBookById(bookId);
    }
}