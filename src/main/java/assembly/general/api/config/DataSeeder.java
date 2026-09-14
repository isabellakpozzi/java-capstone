package assembly.general.api.config;

import assembly.general.api.entity.*;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Runs once on application startup. mechanism used
 * to get demo data into the database without needing direct SQL access
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(BookRepository bookRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedBooks();
        seedLibrarian();
        logExistingBooks();
    }

    private void seedBooks() {
        if (bookRepository.count() > 0) {
            return; // already seeded — don't duplicate on restart/redeploy
        }

        bookRepository.save(buildBook(
                "978-0-13-468599-1", "Clean Code", "Robert C. Martin", "Technology",
                2008, "A handbook of agile software craftsmanship",
                "Prentice Hall", 464, "English", 5, 5
        ));
        bookRepository.save(buildBook(
                "978-0-13-475759-9", "Refactoring", "Martin Fowler", "Technology",
                2018, "Improving the design of existing code",
                "Addison-Wesley", 448, "English", 3, 3
        ));
        bookRepository.save(buildBook(
                "978-0-441-01359-3", "Dune", "Frank Herbert", "Sci-Fi",
                1965, "A stunning blend of adventure and mysticism",
                "Ace Books", 688, "English", 4, 4
        ));
    }

    /**
     * Prints every book's ID to the application log on every startup
     * a workaround so book IDs are retrievable from Render's Logs tab
     */
    private void logExistingBooks() {
        log.info("===== SEEDED BOOKS (for manual reservation testing) =====");
        bookRepository.findAll().forEach(book ->
                log.info("bookId={} title=\"{}\" availableCopies={}",
                        book.getId(), book.getTitle(), book.getAvailableCopies())
        );
        log.info("===========================================================");
    }

    private Book buildBook(String isbn, String title, String author, String genre, int year,
                           String description, String publisher, int pageCount, String language,
                           int totalCopies, int availableCopies) {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setGenre(genre);
        book.setPublicationYear(year);
        book.setDescription(description);
        book.setPublisher(publisher);
        book.setPageCount(pageCount);
        book.setLanguage(language);
        book.setTotalCopies(totalCopies);
        book.setAvailableCopies(availableCopies);
        return book;
    }

    private void seedLibrarian() {
        String email = "librarian@library.com";
        if (userRepository.existsByEmail(email)) {
            return; // already seeded
        }

        User librarian = new User();
        librarian.setEmail(email);
        librarian.setPassword(passwordEncoder.encode("Librarian123!"));
        librarian.setFirstName("Demo");
        librarian.setLastName("Librarian");
        librarian.setPhoneNumber("+1-555-0100");
        librarian.setRole(Role.LIBRARIAN);
        librarian.setMembershipStatus(MembershipStatus.ACTIVE);
        librarian.setMemberSince(LocalDateTime.now());

        userRepository.save(librarian);
    }
}