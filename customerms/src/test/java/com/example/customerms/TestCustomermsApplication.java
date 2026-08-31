package com.example.customerms;

import org.springframework.boot.SpringApplication;

public class TestCustomermsApplication {

	public static void main(String[] args) {
		SpringApplication.from(CustomermsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
