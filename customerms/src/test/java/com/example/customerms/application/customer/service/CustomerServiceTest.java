package com.example.customerms.application.customer.service;

import com.example.customerms.domain.customer.entity.Customer;
import com.example.customerms.domain.customer.exception.CustomerNotFoundException;
import com.example.customerms.domain.customer.exception.DuplicateIdentificationException;
import com.example.customerms.domain.customer.exception.InvalidaPageSizeException;
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
 * Pruebas unitarias de CustomerService. Sin Spring, sin base de datos, sin
 * Docker: solo la clase bajo prueba y un doble del puerto de persistencia.
 *
 * Aqui se prueban las decisiones que toma el servicio -el default de status, el
 * rechazo del duplicado, los limites de paginacion, que campos sobreescribe el
 * update- que en el test de integracion quedan mezcladas con HTTP y SQL. Cuando
 * uno de estos falla, el fallo apunta a una linea de negocio, no a una capa.
 *
 * Complementa a CustomerIntegrationTest, no lo reemplaza: un mock siempre
 * responde lo que le dijeron, asi que nada de esto prueba que el SQL funcione.
 *
 * MockitoExtension corre en modo STRICT_STUBS: un stub que ningun test usa
 * hace fallar la prueba. Es a proposito, evita que queden stubs muertos
 * mintiendo sobre lo que el test cubre.
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
    @DisplayName("create guarda el cliente y devuelve lo que responde el repositorio")
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

        // El servicio devuelve lo que sale del repositorio, no lo que entro: es
        // ahi donde aparecen el id y los timestamps.
        assertThat(result).isSameAs(persisted);
        assertThat(result.getId()).isNotNull();
        verify(repository).save(input);
    }

    @Test
    @DisplayName("create pone status en true cuando el cliente llega sin status")
    void create_defaultsStatusToTrue_whenNull() {
        Customer input = newCustomer();
        assertThat(input.getStatus()).isNull();   // el contrato lo declara opcional

        when(repository.existsByIdentification(input.getIdentification())).thenReturn(false);
        when(repository.save(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        service.create(input);

        // Se captura lo que llego al repositorio: el default debe aplicarse ANTES
        // de guardar, no al mapear la respuesta.
        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isTrue();
    }

    @Test
    @DisplayName("create respeta status=false si viene explicito en la peticion")
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
    @DisplayName("create rechaza identification duplicada y no llega a guardar")
    void create_throwsDuplicate_andDoesNotSave() {
        Customer input = newCustomer();
        when(repository.existsByIdentification(input.getIdentification())).thenReturn(true);

        assertThatThrownBy(() -> service.create(input))
                .isInstanceOf(DuplicateIdentificationException.class)
                .hasMessageContaining(input.getIdentification())
                .extracting("type").isEqualTo(ErrorType.CONFLICT);   // el advice lo traduce a 409

        // Lo importante del caso: el rechazo corta antes del INSERT.
        verify(repository, never()).save(any());
    }

    // ------------------------------------------------------------ update

    @Test
    @DisplayName("update sobreescribe los campos editables sobre el cliente existente")
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

        // El update muta la instancia existente, no crea una nueva: id y createdAt
        // sobreviven porque nunca se tocan.
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);

        // El password tambien se reemplaza: el PUT es sustitucion completa y el
        // contrato lo declara required (@NotNull en CustomerUpdateDto).
        assertThat(result.getPassword()).isEqualTo("nuevo-password");
    }

    @Test
    @DisplayName("update sobre un id inexistente lanza 404 y no guarda nada")
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
    @DisplayName("findAll aplica pagina 0 y tamano 20 cuando no se envian parametros")
    void findAll_appliesDefaults_whenParamsAreNull() {
        Customer customer = newCustomer();
        when(repository.findAll(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer)));

        Page<Customer> result = service.findAll(null, null, null);

        assertThat(result.getContent()).containsExactly(customer);

        // Los defaults son decision del servicio, no del repositorio: se comprueba
        // el Pageable que efectivamente se le pasa.
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(eq(null), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("findAll normaliza una pagina negativa a 0 y propaga el filtro status")
    void findAll_normalizesNegativePage_andPassesStatusFilter() {
        when(repository.findAll(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.findAll(-5, 10, true);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(eq(true), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("findAll rechaza tamanos fuera del rango 1..100 sin consultar la BD")
    void findAll_throwsInvalidPageSize_whenSizeOutOfRange() {
        assertThatThrownBy(() -> service.findAll(0, 0, null))
                .isInstanceOf(InvalidaPageSizeException.class)
                .extracting("type").isEqualTo(ErrorType.INVALID_INPUT);   // el advice lo traduce a 400

        assertThatThrownBy(() -> service.findAll(0, 101, null))
                .isInstanceOf(InvalidaPageSizeException.class);

        // La validacion corta antes de tocar el repositorio: una consulta con
        // size=101 nunca deberia salir hacia la base.
        verifyNoMoreInteractions(repository);
    }
}
