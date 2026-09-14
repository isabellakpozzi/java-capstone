package assembly.general.api.repository;

import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.Role;
import assembly.general.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("dev")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User buildUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hashed-password");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPhoneNumber("+1-555-0100");
        user.setRole(Role.PATRON);
        user.setMembershipStatus(MembershipStatus.ACTIVE);
        user.setMemberSince(LocalDateTime.now());
        return user;
    }

    @Test
    void findByEmail_returnsUser_whenEmailExists() {
        userRepository.save(buildUser("findme@example.com"));

        Optional<User> found = userRepository.findByEmail("findme@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("findme@example.com");
    }

    @Test
    void findByEmail_returnsEmpty_whenEmailDoesNotExist() {
        Optional<User> found = userRepository.findByEmail("nobody@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_returnsTrue_whenEmailIsTaken() {
        userRepository.save(buildUser("taken@example.com"));

        assertThat(userRepository.existsByEmail("taken@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("free@example.com")).isFalse();
    }

    @Test
    void save_throwsConstraintViolation_whenEmailIsDuplicated() {
        userRepository.saveAndFlush(buildUser("duplicate@example.com"));

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(buildUser("duplicate@example.com"))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}