package com.example.customerms.application.customer.dto;

import java.time.LocalDateTime;

import com.example.customerms.domain.person.entity.Gender;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private String id;
    private String name;
    private Gender gender;
    private String identification;
    private String address;
    private String phone;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
