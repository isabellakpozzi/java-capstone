package assembly.general.api.service;

import assembly.general.api.dto.*;
import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.ReservationStatus;
import assembly.general.api.entity.Role;
import assembly.general.api.entity.User;
import assembly.general.api.exception.AuthenticationFailedException;
import assembly.general.api.exception.DuplicateEmailException;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;
import assembly.general.api.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserRepository userRepository,
            ReservationRepository reservationRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // never store raw password
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(Role.PATRON);
        user.setMembershipStatus(MembershipStatus.ACTIVE);
        user.setMemberSince(LocalDateTime.now());

        User saved = userRepository.save(user);

        return new RegisterResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getRole(),
                saved.getMembershipStatus(),
                saved.getCreatedAt(),
                "Registration successful"
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                // same message to not reveal which was the issue
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        UserSummary summary = new UserSummary(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole()
        );

        return new AuthResponse(token, "Bearer", jwtUtil.getExpirationSeconds(), summary);
    }

    public ProfileResponse getProfile(java.util.UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        long active = reservationRepository.countByUserAndStatusIn(
                user, List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT)
        );
        long history = reservationRepository.countByUser(user);

        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getMembershipStatus(),
                user.getMemberSince(),
                active,
                history
        );
    }
}