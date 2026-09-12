package assembly.general.api.dto;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wraps any Spring Data Page<T> into the exact pagination shape used across
 * content, page, size, totalElements, totalPages, last.
 */
@Getter
public class PagedResponse<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean last;

    public PagedResponse(List<T> content, Page<?> sourcePage) {
        this.content = content;
        this.page = sourcePage.getNumber();
        this.size = sourcePage.getSize();
        this.totalElements = sourcePage.getTotalElements();
        this.totalPages = sourcePage.getTotalPages();
        this.last = sourcePage.isLast();
    }
}