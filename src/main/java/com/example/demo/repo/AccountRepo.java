package com.example.demo.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.forentity.Account;

public interface AccountRepo extends JpaRepository<Account, Long> {

    Account findByAccountNo(Long accountNo);

    // Custom query for dashboard
    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a")
    double getTotalBalance();
}
