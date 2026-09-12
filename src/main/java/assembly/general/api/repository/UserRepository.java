package assembly.general.api.repository;

import assembly.general.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // used during login and to check for duplicate emails at registration
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}