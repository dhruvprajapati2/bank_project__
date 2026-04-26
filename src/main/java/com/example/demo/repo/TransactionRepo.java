package com.example.demo.repo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.forentity.Transaction;

public interface TransactionRepo extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountNo(Long accountNo);

    List<Transaction> findByAccountNoOrderByDateDesc(Long accountNo);

    List<Transaction> findByAccountNoAndDateBetweenOrderByDateDesc(Long accountNo, LocalDateTime startDate, LocalDateTime endDate);

    // Custom queries for dashboard
    @Query("SELECT COUNT(t) FROM Transaction t WHERE DATE(t.date) = CURRENT_DATE")
    long countTodayTransactions();

    @Query("SELECT t FROM Transaction t WHERE DATE(t.date) = CURRENT_DATE ORDER BY t.date DESC")
    List<Transaction> findTodayTransactions();

    @Query("SELECT t FROM Transaction t WHERE t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    List<Transaction> findTransactionsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

}
