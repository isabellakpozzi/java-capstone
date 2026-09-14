package assembly.general.api.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String TEST_SECRET = "test-secret-key-needs-to-be-long-enough-for-hs256-signing";

    @Test
    void generateToken_thenExtractClaims_returnsOriginalValues() {
        JwtUtil jwtUtil = new JwtUtil(TEST_SECRET, 86400000L); // 24 hours
        UUID userId = UUID.randomUUID();

        String token = jwtUtil.generateToken(userId, "test@example.com", "PATRON");

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("test@example.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("PATRON");
    }

    @Test
    void isTokenValid_returnsFalse_forMalformedToken() {
        JwtUtil jwtUtil = new JwtUtil(TEST_SECRET, 86400000L);

        assertThat(jwtUtil.isTokenValid("this.is.not-a-real-token")).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forExpiredToken() {
        JwtUtil jwtUtil = new JwtUtil(TEST_SECRET, -1000L);
        UUID userId = UUID.randomUUID();

        String expiredToken = jwtUtil.generateToken(userId, "test@example.com", "PATRON");

        assertThat(jwtUtil.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_whenSignedWithDifferentSecret() {
        JwtUtil signer = new JwtUtil(TEST_SECRET, 86400000L);
        JwtUtil verifier = new JwtUtil("a-completely-different-secret-key-value-here", 86400000L);

        String token = signer.generateToken(UUID.randomUUID(), "test@example.com", "PATRON");

        assertThat(verifier.isTokenValid(token)).isFalse();
    }

    @Test
    void getExpirationSeconds_convertsMillisecondsCorrectly() {
        JwtUtil jwtUtil = new JwtUtil(TEST_SECRET, 86400000L);

        assertThat(jwtUtil.getExpirationSeconds()).isEqualTo(86400L);
    }
}