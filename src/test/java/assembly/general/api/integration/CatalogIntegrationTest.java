package assembly.general.api.integration;

import assembly.general.api.entity.Book;
import assembly.general.api.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    private Book seedBook(String title, int available) {
        Book book = new Book();
        book.setIsbn(UUID.randomUUID().toString());
        book.setTitle(title);
        book.setAuthor("Integration Test Author");
        book.setGenre("Technology");
        book.setPublicationYear(2020);
        book.setTotalCopies(5);
        book.setAvailableCopies(available);
        return bookRepository.save(book);
    }

    @Test
    void listBooks_withNoAuth_isPubliclyAccessible() throws Exception {
        seedBook("Public Catalog Test Book", 3);

        mockMvc.perform(get("/api/catalog/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void listBooks_filterByAvailableOnly_excludesZeroCopyBooks() throws Exception {
        String uniqueTitle = "Unavailable-" + UUID.randomUUID();
        seedBook(uniqueTitle, 0);

        mockMvc.perform(get("/api/catalog/books")
                        .param("query", uniqueTitle)
                        .param("availableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getBookById_whenExists_returnsFullDetail() throws Exception {
        Book book = seedBook("Detail Test Book", 2);

        mockMvc.perform(get("/api/catalog/books/" + book.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(book.getId().toString()))
                .andExpect(jsonPath("$.title").value("Detail Test Book"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void getBookById_whenNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/catalog/books/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}