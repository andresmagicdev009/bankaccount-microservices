package com.example.customerms.application.customer.dto;

import com.example.customerms.domain.person.entity.Gender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    private Gender gender;

    @NotBlank
    @Size(max = 20)
    private String identification;

    @Size(max = 255)
    private String address;

    @Size(max = 20)
    private String phone;

    @NotBlank
    @Size(max = 255)
    private String password;

    private Boolean status;
}
