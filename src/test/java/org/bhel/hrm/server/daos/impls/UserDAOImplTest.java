package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.common.dtos.UserDTO;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.config.Configuration;
import org.bhel.hrm.server.daos.UserDAO;
import org.bhel.hrm.server.domain.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@DisplayName("UserDAO implementation tests")
class UserDAOImplTest {

    @Container
    private static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.0");
    private static UserDAO userDAO;

    @BeforeAll
    static void setup() {
        Configuration mockConfig = mock(Configuration.class);

        when(mockConfig.getDbUrl()).thenReturn(mysql.getJdbcUrl());
        when(mockConfig.getDbUser()).thenReturn(mysql.getUsername());
        when(mockConfig.getDbPassword()).thenReturn(mysql.getPassword());

        DatabaseManager dbManager = new DatabaseManager(mockConfig);
        userDAO = new UserDAOImpl(dbManager);
    }

    @AfterEach
    void tearDown() {
        userDAO.findAll().forEach(user
            -> userDAO.deleteById(user.getId())
        );
    }

    @Nested
    @DisplayName("save and find operations")
    class SaveAndFindTests {
        @Test
        @DisplayName("save() should save a new user and correctly set the generated ID")
        void save_shouldInsertNewUserAndSetId() {
            // Given: A new user object without an ID
            User newUser = new User(0, "test_user", "password_hash_123", UserDTO.Role.EMPLOYEE);

            // When: The user is saved
            userDAO.save(newUser);

            // Then: The user object should have a non-zero ID assigned to it
            assertThat(newUser.getId()).isGreaterThan(0);
        }

        @Test
        @DisplayName("findById() should return the correct user when they exist")
        void findById_shouldReturnCorrectUser_whenUserExists() {
            // Given: A user is saved to the database
            User newUser = new User(0, "find_me", "password123", UserDTO.Role.HR_STAFF);
            userDAO.save(newUser);

            // When: We try to find that user by their generated ID
            Optional<User> foundUserOpt = userDAO.findById(newUser.getId());

            // Then: The user should be found and all properties should match
            assertThat(foundUserOpt).hasValueSatisfying(user ->
                assertThat(user)
                    .usingRecursiveComparison()
                    .isEqualTo(newUser)
            );
        }

        @Test
        @DisplayName("findById() should return an empty Optional when the user does not exist")
        void findById_shouldReturnEmpty_whenUserDoesNotExist() {
            // Given: No user exists with the given ID

            // When: We try to find a user with a non-existent ID
            Optional<User> foundUserOpt = userDAO.findById(99999);

            // Then: The result should be an empty Optional
            assertThat(foundUserOpt).isEmpty();
        }

        @Test
        @DisplayName("findByUsername() should return the correct user when they exist")
        void findByUsername_shouldReturnCorrectUser_whenUserExists() {
            // Given: A user is saved to the database
            User newUser = new User(0, "find_me_username", "password123", UserDTO.Role.HR_STAFF);
            userDAO.save(newUser);

            // When: We try to find that user by their generated username
            Optional<User> foundUserOpt = userDAO.findByUsername(newUser.getUsername());

            // Then: The user should be found and all properties should match
            assertThat(foundUserOpt).hasValueSatisfying(user ->
                assertThat(user)
                    .usingRecursiveComparison()
                    .isEqualTo(newUser)
            );
        }

        @Test
        @DisplayName("findByUsername() should return an empty Optional when the user does not exist")
        void findByUsername_shouldReturnEmpty_whenUserDoesNotExist() {
            // Given: No user exists with the given username

            // When: We try to find a user with a non-existent username
            Optional<User> foundUserOpt = userDAO.findByUsername("non-existent user");

            // Then: The result should be an empty Optional
            assertThat(foundUserOpt).isEmpty();
        }
    }

    @Nested
    @DisplayName("Update and delete operations")
    class UpdateAndDeleteTests {
        @Test
        @DisplayName("save() should update an existing user's details")
        void save_shouldUpdateExistingUser() {
            // Given: An existing user is saved
            User user = new User(0, "original_user", "pass", UserDTO.Role.EMPLOYEE);
            userDAO.save(user);
            int originalId = user.getId();

            // When: We change the user's properties and call save again
            user.setUsername("updated_user");
            user.setRole(UserDTO.Role.HR_STAFF);
            userDAO.save(user);

            // Then: The original ID should be unchanged, and the retrieved record should have the new data
            Optional<User> updatedUserOpt = userDAO.findById(originalId);
            assertThat(user.getId()).isEqualTo(originalId);
            assertThat(updatedUserOpt).hasValueSatisfying(u -> {
                assertThat(u.getUsername()).isEqualTo("updated_user");
                assertThat(u.getRole()).isEqualTo(UserDTO.Role.HR_STAFF);
            });
        }

        @Test
        @DisplayName("deleteById() should permanently remove a user from the database")
        void deleteById_shouldRemoveUser() {
            // Given: A user is saved
            User user = new User(0, "to_be_deleted", "pass", UserDTO.Role.EMPLOYEE);
            userDAO.save(user);
            assertThat(userDAO.findById(user.getId())).isPresent(); // Verify it exists first

            // When: We delete the user by its ID
            userDAO.deleteById(user.getId());

            // Then: Finding them again should return an empty Optional
            assertThat(userDAO.findById(user.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Query operations")
    class QueryTests {
        @ParameterizedTest
        @EnumSource(UserDTO.Role.class)
        @DisplayName("save() should correctly save and retrieve all role types")
        void save_shouldHandleAllRoleTypes(UserDTO.Role role) {
            // Given: A new user object without an ID
            User newUser = new User(0, "role_test_user", "pass", role);

            // When: The user is saved
            userDAO.save(newUser);

            // Then: The role assigned should be matched
            Optional<User> foundUserOpt = userDAO.findById(newUser.getId());
            assertThat(foundUserOpt).hasValueSatisfying(user ->
                assertThat(user.getRole()).isEqualTo(role)
            );

        }

        @Test
        @DisplayName("findAll() should return all users in alphabetical order of username")
        void findAll_shouldReturnAllUsers() {
            // Given: Multiple users are saved in a non-alphabetical order
            userDAO.save(new User(0, "charlie", "p", UserDTO.Role.EMPLOYEE));
            userDAO.save(new User(0, "alice", "p", UserDTO.Role.HR_STAFF));
            userDAO.save(new User(0, "bob", "p", UserDTO.Role.EMPLOYEE));

            // When: We retrieve all users
            List<User> users = userDAO.findAll();

            // Then: The list should be correct size and sorted by username
            assertThat(users).hasSize(3)
                .extracting(User::getUsername)
                .containsExactly("alice", "bob", "charlie");
        }

        @Test
        @DisplayName("count() should return the total number of users")
        void count_shouldReturnTotalNumberOfUsers() {
            // Given: We save a known number of users
            userDAO.save(new User(0, "user1", "p", UserDTO.Role.HR_STAFF));
            userDAO.save(new User(0, "user2", "p", UserDTO.Role.HR_STAFF));

            // When: We call count
            long userCount = userDAO.count();

            // Then: The count should be correct
            assertThat(userCount).isEqualTo(2);
        }
    }
}