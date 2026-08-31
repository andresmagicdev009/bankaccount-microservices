package com.example.customerms.domain.customer.entity;

import com.example.customerms.domain.person.entity.Person;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Customer extends Person {

    private String password;
    private Boolean status;
}
