package com.example.customerms.application.customer.service;

import com.example.customerms.domain.customer.entity.Customer;
import com.example.customerms.domain.customer.exception.CustomerNotFoundException;
import com.example.customerms.domain.customer.exception.DuplicateIdentificationException;
import com.example.customerms.domain.customer.exception.InvalidPageSizeException;
import com.example.customerms.domain.customer.repository.CustomerRepositoryPort;
import com.example.customerms.domain.person.entity.Gender;
import com.example.customerms.domain.shared.exception.ErrorType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomerService. No Spring, no database, no Docker: only the
 * class under test and a double of the persistence port.
 *
 * What is tested here are the decisions the service makes -the status default,
 * the rejection of a duplicate, the pagination limits, which fields the update
 * overwrites- which in the integration test come mixed with HTTP and SQL. When
 * one of these fails, the failure points at a line of business logic, not at a
 * layer.
 *
 * It complements CustomerIT, it does not replace it: a mock always answers what
 * it was told to, so none of this proves the SQL works.
 *
 * MockitoExtension runs in STRICT_STUBS mode: a stub no test uses fails the
 * suite. That is on purpose, it keeps dead stubs from lying about what the test
 * covers.
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepositoryPort repository;

    @InjectMocks
    private CustomerService service;

    private Customer newCustomer() {
        return Customer.builder()
                .name("Jose Lema")
                .gender(Gender.MALE)
                .identification("0102030405")
                .address("Otavalo sn y principal")
                .phone("098254785")
                .password("1234")
                .build();
    }

    // ------------------------------------------------------------ create

    @Test
    @DisplayName("create saves the customer and returns what the repository answers")
    void create_savesAndReturnsPersistedCustomer() {
        Customer input = newCustomer();
        Customer persisted = Customer.builder()
                .id("11111111-1111-1111-1111-111111111111")
                .name(input.getName())
                .identification(input.getIdentification())
                .password(input.getPassword())
                .status(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(repository.existsByIdentification(input.getIdentification())).thenReturn(false);
        when(repository.save(input)).thenReturn(persisted);

        Customer result = service.create(input);

        // The service returns what comes out of the repository, not what went in:
        // that is where the id and the timestamps appear.
        assertThat(result).isSameAs(persisted);
        assertThat(result.getId()).isNotNull();
        verify(repository).save(input);
    }

    @Test
    @DisplayName("create sets status to true when the customer arrives without one")
    void create_defaultsStatusToTrue_whenNull() {
        Customer input = newCustomer();
        assertThat(input.getStatus()).isNull();   // the contract declares it optional

        when(repository.existsByIdentification(input.getIdentification())).thenReturn(false);
        when(repository.save(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        service.create(input);

        // What reached the repository is captured: the default must be applied
        // BEFORE saving, not while mapping the response.
        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isTrue();
    }

    @Test
    @DisplayName("create honours status=false when it comes explicitly in the request")
    void create_keepsExplicitStatus() {
        Customer input = newCustomer();
        input.setStatus(false);

        when(repository.existsByIdentification(input.getIdentification())).thenReturn(false);
        when(repository.save(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        service.create(input);

        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isFalse();
    }

    @Test
    @DisplayName("create rejects a duplicate identification and never saves")
    void create_throwsDuplicate_andDoesNotSave() {
        Customer input = newCustomer();
        when(repository.existsByIdentification(input.getIdentification())).thenReturn(true);

        assertThatThrownBy(() -> service.create(input))
                .isInstanceOf(DuplicateIdentificationException.class)
                .hasMessageContaining(input.getIdentification())
                .extracting("type").isEqualTo(ErrorType.CONFLICT);   // the advice turns it into a 409

        // The point of this case: the rejection stops before the INSERT.
        verify(repository, never()).save(any());
    }

    // ------------------------------------------------------------ update

    @Test
    @DisplayName("update overwrites the editable fields on the existing customer")
    void update_overwritesEditableFields() {
        String id = "11111111-1111-1111-1111-111111111111";
        LocalDateTime createdAt = LocalDateTime.now().minusDays(3);

        Customer existing = Customer.builder()
                .id(id)
                .name("Jose Lema")
                .gender(Gender.MALE)
                .identification("0102030405")
                .address("Otavalo sn y principal")
                .phone("098254785")
                .password("1234")
                .status(true)
                .createdAt(createdAt)
                .build();

        Customer changes = Customer.builder()
                .name("Jose Lema Editado")
                .gender(Gender.OTHER)
                .identification("0999999999")
                .address("Amazonas y NNUU")
                .phone("097777777")
                .password("nuevo-password")
                .status(false)
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        Customer result = service.update(id, changes);

        assertThat(result.getName()).isEqualTo("Jose Lema Editado");
        assertThat(result.getGender()).isEqualTo(Gender.OTHER);
        assertThat(result.getIdentification()).isEqualTo("0999999999");
        assertThat(result.getAddress()).isEqualTo("Amazonas y NNUU");
        assertThat(result.getPhone()).isEqualTo("097777777");
        assertThat(result.getStatus()).isFalse();

        // The update mutates the existing instance, it does not create a new one:
        // id and createdAt survive because they are never touched.
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);

        // The password is replaced too: PUT is a full substitution and the
        // contract declares it required (@NotNull on CustomerUpdateDto).
        assertThat(result.getPassword()).isEqualTo("nuevo-password");
    }

    @Test
    @DisplayName("update on an unknown id throws 404 and saves nothing")
    void update_throwsNotFound_andDoesNotSave() {
        String unknownId = "99999999-9999-9999-9999-999999999999";
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(unknownId, newCustomer()))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(unknownId)
                .extracting("type").isEqualTo(ErrorType.NOT_FOUND);

        verify(repository, never()).save(any());
    }

    // ----------------------------------------------------------- findAll

    @Test
    @DisplayName("findAll applies page 0 and size 20 when no parameters are sent")
    void findAll_appliesDefaults_whenParamsAreNull() {
        Customer customer = newCustomer();
        when(repository.findAll(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer)));

        Page<Customer> result = service.findAll(null, null, null);

        assertThat(result.getContent()).containsExactly(customer);

        // The defaults are the service's decision, not the repository's: the
        // Pageable actually handed to it is what gets checked.
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(eq(null), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("findAll normalises a negative page to 0 and propagates the status filter")
    void findAll_normalizesNegativePage_andPassesStatusFilter() {
        when(repository.findAll(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.findAll(-5, 10, true);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(eq(true), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("findAll rejects sizes outside the 1..100 range without querying the DB")
    void findAll_throwsInvalidPageSize_whenSizeOutOfRange() {
        assertThatThrownBy(() -> service.findAll(0, 0, null))
                .isInstanceOf(InvalidPageSizeException.class)
                .extracting("type").isEqualTo(ErrorType.INVALID_INPUT);   // the advice turns it into a 400

        assertThatThrownBy(() -> service.findAll(0, 101, null))
                .isInstanceOf(InvalidPageSizeException.class);

        // Validation stops before touching the repository: a query with size=101
        // should never reach the database.
        verifyNoMoreInteractions(repository);
    }
}
