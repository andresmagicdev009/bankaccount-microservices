package com.example.customerms.domain.customer.exception;

public class DuplicateIdentificationException extends RuntimeException{
    public DuplicateIdentificationException(String identification){
        super("Customer already exists with identification: " + identification);
    }
}
