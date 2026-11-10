
package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    // Get all transactions, newest first
    @Query("SELECT t FROM BankTransaction t ORDER BY t.createdAt DESC")
    List<BankTransaction> findAllLatest();

    // Get transaction history for an account
    @Query("""
        SELECT t FROM BankTransaction t
        WHERE t.fromAccount = :accountNumber
        OR t.toAccount = :accountNumber
        ORDER BY t.createdAt DESC
        """)
    List<BankTransaction> findHistory(
            @Param("accountNumber") String accountNumber);

    // Find transactions by status, newest first (assignment method name)
    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<BankTransaction> findByStatusLatest(@Param("status") TransactionStatus status);

    // Count transactions by status
    long countByStatus(TransactionStatus status);

    // Find transactions belonging to an account
    List<BankTransaction> findByFromAccountOrToAccountOrderByCreatedAtDesc(
            String fromAccount, String toAccount);

    // Find successful transfers above a minimum amount
    @Query("""
        SELECT t FROM BankTransaction t
        WHERE t.status = :status
        AND t.amount > :minimumAmount
        AND t.fromAccount <> 'CASH'
        AND t.toAccount <> 'CASH'
        ORDER BY t.createdAt DESC
        """)
    List<BankTransaction> findLargeTransfers(
            @Param("status") TransactionStatus status,
            @Param("minimumAmount") BigDecimal minimumAmount);

    // Calculate total transfers sent by an account for a given status
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM BankTransaction t
        WHERE t.fromAccount = :accountNumber
        AND t.toAccount <> 'CASH'
        AND t.status = :status
        """)
    BigDecimal calculateTotalSent(
            @Param("accountNumber") String accountNumber,
            @Param("status") TransactionStatus status);
}
