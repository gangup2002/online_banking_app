package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Seed the three assignment customers only when the database is empty.
        if (customerRepository.count() == 0) {
            customerRepository.saveAll(List.of(
                    createCustomer("100001", "Kamal", "kamal@gmail.com", "50000.00"),
                    createCustomer("100002", "Nimali", "nimali@gmail.com", "25000.00"),
                    createCustomer("100003", "Saman", "saman@gmail.com", "10000.00")
            ));
            System.out.println("Assignment sample customers inserted. Initial total: LKR 85,000.00");
        }

        // Add the fee-collection account even if this is an existing database.
        if (customerRepository.countAccounts("999999") == 0) {
            customerRepository.save(createCustomer(
                    "999999", "Bank Fee Account", "bankfees@bank.local", "0.00"));
        }
    }

    private Customer createCustomer(String accountNumber, String fullName,
                                    String email, String balance) {
        Customer customer = new Customer();
        customer.setAccountNumber(accountNumber);
        customer.setFullName(fullName);
        customer.setEmail(email);
        customer.setBalance(new BigDecimal(balance));
        return customer;
    }
}
