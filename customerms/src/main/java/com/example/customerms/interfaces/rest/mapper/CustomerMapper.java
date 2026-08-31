package com.example.customerms.interfaces.rest.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.example.customerms.domain.customer.entity.Customer;
import com.example.customerms.domain.person.entity.Gender;
import com.example.customerms.interfaces.rest.dto.CustomerCreateDto;
import com.example.customerms.interfaces.rest.dto.CustomerDto;
import com.example.customerms.interfaces.rest.dto.CustomerUpdateDto;
import com.example.customerms.interfaces.rest.dto.GenderDto;

import org.springframework.stereotype.Component;

/**
 * Translates between the contract types generated from the OpenAPI spec and the
 * domain model. This is the boundary: everything to one side is HTTP, everything
 * to the other side is business.
 */
@Component
public class CustomerMapper {

    public Customer toDomain(CustomerCreateDto dto) {
        return Customer.builder()
                .name(dto.getName())
                .gender(toDomainGender(dto.getGender()))
                .identification(dto.getIdentification())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .password(dto.getPassword())
                .status(dto.getStatus())
                .build();
    }

    public Customer toDomainUpdate(CustomerUpdateDto dto) {
        return Customer.builder()
                .name(dto.getName())
                .gender(toDomainGender(dto.getGender()))
                .identification(dto.getIdentification())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .password(dto.getPassword())
                .status(dto.getStatus())
                .build();
    }

    /** The password is writeOnly in the contract, so it is never mapped back out. */
    public CustomerDto toDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(toUuid(customer.getId()));
        dto.setName(customer.getName());
        dto.setGender(toDtoGender(customer.getGender()));
        dto.setIdentification(customer.getIdentification());
        dto.setAddress(customer.getAddress());
        dto.setPhone(customer.getPhone());
        dto.setStatus(customer.getStatus());
        dto.setCreatedAt(toOffsetDateTime(customer.getCreatedAt()));
        dto.setUpdatedAt(toOffsetDateTime(customer.getUpdatedAt()));
        return dto;
    }

    private Gender toDomainGender(GenderDto gender) {
        return gender == null ? null : Gender.valueOf(gender.getValue());
    }

    private GenderDto toDtoGender(Gender gender) {
        return gender == null ? null : GenderDto.fromValue(gender.name());
    }

    private UUID toUuid(String id) {
        return id == null ? null : UUID.fromString(id);
    }

    /** The database stores naive timestamps; the contract exposes them as UTC. */
    private OffsetDateTime toOffsetDateTime(LocalDateTime timestamp) {
        return timestamp == null ? null : timestamp.atOffset(ZoneOffset.UTC);
    }
}
