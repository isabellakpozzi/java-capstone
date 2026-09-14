package assembly.general.api.repository;

import assembly.general.api.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("dev")
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    private Book buildBook(String isbn, String title, String author, String genre,
                           int year, int total, int available) {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setGenre(genre);
        book.setPublicationYear(year);
        book.setDescription("Test description");
        book.setTotalCopies(total);
        book.setAvailableCopies(available);
        return book;
    }

    @BeforeEach
    void seedBooks() {
        bookRepository.save(buildBook("111", "Clean Code", "Robert Martin", "Technology", 2008, 5, 3));
        bookRepository.save(buildBook("222", "Refactoring", "Martin Fowler", "Technology", 2018, 3, 0));
        bookRepository.save(buildBook("333", "Dune", "Frank Herbert", "Sci-Fi", 1965, 4, 4));
    }

    @Test
    void search_withNoFilters_returnsAllBooks() {
        Page<Book> result = bookRepository.search(null, null, null, false, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void search_byQuery_matchesTitleOrAuthor() {
        Page<Book> result = bookRepository.search("clean", null, null, false, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void search_byGenre_filtersExactMatch() {
        Page<Book> result = bookRepository.search(null, "Technology", null, false, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void search_byIsbn_returnsExactMatchOnly() {
        Page<Book> result = bookRepository.search(null, null, "222", false, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsbn()).isEqualTo("222");
    }

    @Test
    void search_availableOnly_excludesZeroCopyBooks() {
        Page<Book> result = bookRepository.search(null, null, null, true, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2); // excludes "Refactoring" (0 available)
    }

    @Test
    void search_combinedFilters_applyTogether() {
        Page<Book> result = bookRepository.search("code", "Technology", null, true, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void search_sortedByPublicationYearDescending() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "publicationYear"));

        Page<Book> result = bookRepository.search(null, null, null, false, pageable);

        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Refactoring"); // 2018, newest
    }

    @Test
    void search_paginationMetadata_isCorrect() {
        Page<Book> result = bookRepository.search(null, null, null, false, PageRequest.of(0, 2));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isLast()).isFalse();
    }

    @Test
    void search_withNoMatches_returnsEmptyPageNotError() {
        Page<Book> result = bookRepository.search("nonexistent-title-xyz", null, null, false, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void save_throwsConstraintViolation_whenIsbnIsDuplicated() {
        assertThatThrownBy(() ->
                bookRepository.saveAndFlush(
                        buildBook("111", "Duplicate ISBN Book", "Someone", "Fiction", 2020, 1, 1)
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}