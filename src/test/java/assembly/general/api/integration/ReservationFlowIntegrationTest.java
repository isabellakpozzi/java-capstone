package assembly.general.api.integration;

import assembly.general.api.entity.*;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ReservationFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Book seedBook(int available) {
        Book book = new Book();
        book.setIsbn(UUID.randomUUID().toString());
        book.setTitle("Reservation Test Book " + UUID.randomUUID());
        book.setAuthor("Test Author");
        book.setGenre("Fiction");
        book.setPublicationYear(2021);
        book.setTotalCopies(5);
        book.setAvailableCopies(available);
        return bookRepository.save(book);
    }

    private String patronToken() throws Exception {
        String email = "patron-" + UUID.randomUUID() + "@example.com";
        Map<String, String> registerBody = Map.of(
                "email", email, "password", "Test123!@#",
                "firstName", "Pat", "lastName", "Ron", "phoneNumber", "+1-555-0100"
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(registerBody)));

        return login(email, "Test123!@#");
    }

    private String librarianToken() throws Exception {
        String email = "librarian-" + UUID.randomUUID() + "@example.com";
        User librarian = new User();
        librarian.setEmail(email);
        librarian.setPassword(passwordEncoder.encode("Test123!@#"));
        librarian.setFirstName("Lib");
        librarian.setLastName("Rarian");
        librarian.setPhoneNumber("+1-555-0200");
        librarian.setRole(Role.LIBRARIAN);
        librarian.setMembershipStatus(MembershipStatus.ACTIVE);
        librarian.setMemberSince(LocalDateTime.now());
        userRepository.save(librarian);

        return login(email, "Test123!@#");
    }

    private String login(String email, String password) throws Exception {
        Map<String, String> loginBody = Map.of("email", email, "password", password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void fullLifecycle_reserveCheckoutReturn_succeedsEndToEnd() throws Exception {
        String patron = patronToken();
        String librarian = librarianToken();
        Book book = seedBook(3);

        // Reserve
        String reserveResponse = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + patron)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("bookId", book.getId().toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andReturn().getResponse().getContentAsString();

        String reservationId = objectMapper.readTree(reserveResponse).get("reservationId").asText();

        // View active reservations
        mockMvc.perform(get("/api/reservations").header("Authorization", "Bearer " + patron))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActive").value(1));

        // PATRON cannot checkout
        mockMvc.perform(post("/api/reservations/" + reservationId + "/checkout")
                        .header("Authorization", "Bearer " + patron)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        // LIBRARIAN can checkout
        mockMvc.perform(post("/api/reservations/" + reservationId + "/checkout")
                        .header("Authorization", "Bearer " + librarian)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("notes", "Good condition"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"));

        // Cannot checkout again (wrong status)
        mockMvc.perform(post("/api/reservations/" + reservationId + "/checkout")
                        .header("Authorization", "Bearer " + librarian)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS"));

        // PATRON cannot return
        mockMvc.perform(post("/api/reservations/" + reservationId + "/return")
                        .header("Authorization", "Bearer " + patron)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("condition", "GOOD"))))
                .andExpect(status().isForbidden());

        // LIBRARIAN returns it on time
        mockMvc.perform(post("/api/reservations/" + reservationId + "/return")
                        .header("Authorization", "Bearer " + librarian)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("condition", "GOOD", "notes", "fine"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lateDays").value(0))
                .andExpect(jsonPath("$.lateFee").value(0.0));

        // Now appears in history
        mockMvc.perform(get("/api/reservations/history").header("Authorization", "Bearer " + patron))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("RETURNED"))
                .andExpect(jsonPath("$.content[0].wasLate").value(false));
    }

    @Test
    void reserve_unavailableBook_returns400() throws Exception {
        String patron = patronToken();
        Book book = seedBook(0); // no copies available

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + patron)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("bookId", book.getId().toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BOOK_UNAVAILABLE"))
                .andExpect(jsonPath("$.availableCopies").value(0));
    }

    @Test
    void reserve_sixthBook_whenAtLimit_returns400() throws Exception {
        String patron = patronToken();

        for (int i = 0; i < 5; i++) {
            Book book = seedBook(2);
            mockMvc.perform(post("/api/reservations")
                            .header("Authorization", "Bearer " + patron)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(Map.of("bookId", book.getId().toString()))))
                    .andExpect(status().isCreated());
        }

        Book sixthBook = seedBook(2);
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + patron)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("bookId", sixthBook.getId().toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("RESERVATION_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.currentReservations").value(5));
    }

    @Test
    void reservations_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }
}