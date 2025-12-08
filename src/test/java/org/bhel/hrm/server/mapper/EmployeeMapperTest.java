package org.bhel.hrm.server.mapper;

import org.bhel.hrm.common.dtos.EmployeeDTO;
import org.bhel.hrm.server.domain.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("EmployeeMapper unit tests")
class EmployeeMapperTest {

    @Nested
    @DisplayName("toDto conversion")
    class MapToDtoTests {

        @Test
        @DisplayName("should correctly map a non-null Employee domain object to a EmployeeDTO")
        void shouldMapDomainToDto() {
            // Given: A fully populated Employee domain object
            Employee domain = new Employee(1, 101, "John", "Doe", "S1234567A");

            // When: The employee is mapped to a DTO
            EmployeeDTO dto = EmployeeMapper.mapToDto(domain);

            // Then: The DTO should contain the correct, publicly-safe data
            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(1);
            assertThat(dto.userId()).isEqualTo(101);
            assertThat(dto.firstName()).isEqualTo("John");
            assertThat(dto.lastName()).isEqualTo("Doe");
            assertThat(dto.icPassport()).isEqualTo("S1234567A");
        }

        @Test
        @DisplayName("should return null when the input domain object is null")
        void shouldReturnNullForNullDomain() {
            // Given, When, Then
            assertThat(EmployeeMapper.mapToDto(null)).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain conversion")
    class MapToDomainTests {
        @Test
        @DisplayName("should correctly map a non-null EmployeeDTO to an Employee domain object")
        void shouldMapToDomain() {
            // Given: A fully populated EmployeeDTO record
            EmployeeDTO dto = new EmployeeDTO(1, 101, "Jane", "Doe", "G9876543B");

            // When: The mapper converts it to a domain object
            Employee domain = EmployeeMapper.mapToDomain(dto);

            // Then: The domain object should be correctly populated
            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isEqualTo(1);
            assertThat(domain.getUserId()).isEqualTo(101);
            assertThat(domain.getFirstName()).isEqualTo("Jane");
            assertThat(domain.getLastName()).isEqualTo("Doe");
            assertThat(domain.getIcPassport()).isEqualTo("G9876543B");
        }

        @Test
        @DisplayName("should return null when the input DTO is null")
        void shouldReturnNullForNullDto() {
            // Given, When, Then
            assertThat(EmployeeMapper.mapToDomain(null)).isNull();
        }
    }

    @Nested
    @DisplayName("List conversion")
    class ListMapperTests {

        @Test
        @DisplayName("should correctly map a list of domain objects to a list of DTOs")
        void shouldMapDomainListToDtoList() {
            // Given: A list of domain objects
            Employee emp1 = new Employee(1, 101, "Alice", "Smith", "A1");
            Employee emp2 = new Employee(2, 102, "Bob", "Johnson", "B2");
            List<Employee> domainList = List.of(emp1, emp2);

            // When: The list is mapped
            List<EmployeeDTO> dtoList = EmployeeMapper.mapToDtoList(domainList);

            // Then: The DTO list should have the same size and has the correct data
            assertThat(dtoList)
                .isNotNull()
                .hasSize(2)
                .extracting(
                    EmployeeDTO::id,
                    EmployeeDTO::userId,
                    EmployeeDTO::firstName,
                    EmployeeDTO::lastName,
                    EmployeeDTO::icPassport
                ).containsExactly(
                    tuple(1, 101, "Alice", "Smith", "A1"),
                    tuple(2, 102, "Bob", "Johnson", "B2")
                );
        }

        @Test
        @DisplayName("should return an empty list when the input list is empty")
        void shouldReturnEmptyListForEmptyInput() {
            // Given: An empty list of domain objects
            List<Employee> emptyList = Collections.emptyList();

            // When: The list is mapped
            List<EmployeeDTO> dtoList = EmployeeMapper.mapToDtoList(emptyList);

            // Then: The result should be an empty, non-null list
            assertThat(dtoList).isNotNull().isEmpty();
        }
    }
}