package com.example.bankapp.service;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private static final String BANK_ACCOUNT = "999999";
    private static final BigDecimal FEE_RATE = new BigDecimal("0.01");

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    /** Transfers the requested amount and a 1% fee in one atomic transaction. */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(String fromAccount, String toAccount,
                         BigDecimal amount, boolean simulateFailure) {
        validateAmount(amount);
        if (fromAccount == null || toAccount == null || fromAccount.equals(toAccount)) {
            throw new IllegalArgumentException("Sender and receiver accounts must be different.");
        }

        Customer sender = findCustomer(fromAccount, "Sender");
        findCustomer(toAccount, "Receiver");
        Customer bank = findCustomer(BANK_ACCOUNT, "Bank fee");

        BigDecimal fee = amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDebit = amount.add(fee);
        if (customerRepository.debit(sender.getAccountNumber(), totalDebit) == 0) {
            throw new IllegalArgumentException("Insufficient balance (including the 1% fee).");
        }

        if (simulateFailure) {
            throw new RuntimeException("All changes were rolled back (simulated system failure).");
        }

        if (customerRepository.credit(toAccount, amount) == 0
                || customerRepository.credit(BANK_ACCOUNT, fee) == 0) {
            throw new RuntimeException("Could not complete the transfer.");
        }

        saveTransaction(fromAccount, toAccount, totalDebit, "SUCCESS",
                "Successful transfer; amount LKR " + amount.toPlainString()
                        + " plus 1% fee LKR " + fee.toPlainString());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deposit(String accountNumber, BigDecimal amount) {
        validateAmount(amount);
        findCustomer(accountNumber, "Customer");
        if (customerRepository.credit(accountNumber, amount) == 0) {
            throw new IllegalArgumentException("Could not deposit into this account.");
        }
        saveTransaction("CASH", accountNumber, amount, "SUCCESS", "Cash deposit");
    }

    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String accountNumber, BigDecimal amount) {
        validateAmount(amount);
        findCustomer(accountNumber, "Customer");
        if (customerRepository.debit(accountNumber, amount) == 0) {
            throw new IllegalArgumentException("Insufficient balance.");
        }
        saveTransaction(accountNumber, "CASH", amount, "SUCCESS", "Cash withdrawal");
    }

    private Customer findCustomer(String accountNumber, String role) {
        return customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException(role + " account not found."));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
    }

    private void saveTransaction(String from, String to, BigDecimal amount,
                                 String status, String description) {
        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(from);
        transaction.setToAccount(to);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.valueOf(status));
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription(description);
        transactionRepository.save(transaction);
    }
}
