package fr.lpreaux.usermanager.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    @DisplayName("Should create UserId from UUID")
    void shouldCreateUserIdFromUUID() {
        // Given
        UUID uuid = UUID.randomUUID();

        // When
        UserId userId = UserId.of(uuid);

        // Then
        assertThat(userId).isNotNull();
        assertThat(userId.getValue()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("Should create UserId from String")
    void shouldCreateUserIdFromString() {
        // Given
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();

        // When
        UserId userId = UserId.of(uuidString);

        // Then
        assertThat(userId).isNotNull();
        assertThat(userId.getValue()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("Should generate new UserId")
    void shouldGenerateNewUserId() {
        // When
        UserId userId = UserId.generate();

        // Then
        assertThat(userId).isNotNull();
        assertThat(userId.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception for null UUID")
    void shouldThrowExceptionForNullUUID() {
        assertThatThrownBy(() -> UserId.of((UUID) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for invalid UUID string")
    void shouldThrowExceptionForInvalidUUIDString() {
        assertThatThrownBy(() -> UserId.of("not-a-valid-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        UUID uuid = UUID.randomUUID();
        UserId userId1 = UserId.of(uuid);
        UserId userId2 = UserId.of(uuid);
        UserId userId3 = UserId.generate();

        // Then
        assertThat(userId1).isEqualTo(userId2);
        assertThat(userId1).isNotEqualTo(userId3);
        assertThat(userId1.hashCode()).isEqualTo(userId2.hashCode());
        assertThat(userId1.hashCode()).isNotEqualTo(userId3.hashCode());
    }

    @Test
    @DisplayName("Should have proper string representation")
    void shouldHaveProperStringRepresentation() {
        // Given
        UUID uuid = UUID.randomUUID();
        UserId userId = UserId.of(uuid);

        // When
        String toString = userId.toString();

        // Then
        assertThat(toString).contains(uuid.toString());
    }
}