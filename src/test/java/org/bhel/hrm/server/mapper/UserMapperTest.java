package org.bhel.hrm.server.mapper;

import org.bhel.hrm.common.dtos.UserDTO;
import org.bhel.hrm.server.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper unit tests")
class UserMapperTest {

    @Nested
    @DisplayName("toDto conversion")
    class MapToDtoTests {

        @Test
        @DisplayName("should correctly map a non-null User domain object to a UserDTO")
        void shouldMapDomainToDto() {
            // Given: A fully populated User domain object
            User domain = new User(42, "john_doe", "hashed_password", UserDTO.Role.EMPLOYEE);

            // When: The user is mapped to a DTO
            UserDTO dto = UserMapper.mapToDto(domain);

            // Then: The DTO should contain the correct, publicly-safe data
            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(42);
            assertThat(dto.username()).isEqualTo("john_doe");
            assertThat(dto.role()).isEqualTo(UserDTO.Role.EMPLOYEE);
        }

        @Test
        @DisplayName("should return null when the input domain object is null")
        void shouldReturnNullForNullDomain() {
            // Given, When, Then
            assertThat(UserMapper.mapToDto(null)).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain conversion")
    class MapToDomainTests {
        @Test
        @DisplayName("should correctly map a non-null UserDTO to a User domain object")
        void shouldMapToDomain() {
            // Given: A fully populated UserDTO record
            UserDTO dto = new UserDTO(101, "jane_doe", UserDTO.Role.HR_STAFF);

            // When: The mapper converts it to a domain object
            User domain = UserMapper.mapToDomain(dto);

            // Then: The domain object should be correctly populated
            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isEqualTo(101);
            assertThat(domain.getUsername()).isEqualTo("jane_doe");
            assertThat(domain.getRole()).isEqualTo(UserDTO.Role.HR_STAFF);
            assertThat(domain.getPasswordHash()).isNull(); // The password hash should be null as it's not part of the DTO
        }

        @Test
        @DisplayName("should return null when the input DTO is null")
        void shouldReturnNullForNullDto() {
            // Given, When, Then
            assertThat(UserMapper.mapToDomain(null)).isNull();
        }
    }
}