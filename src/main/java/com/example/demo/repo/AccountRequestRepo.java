package com.example.demo.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.forentity.AccountRequest;

public interface AccountRequestRepo
        extends JpaRepository<AccountRequest, Integer> {

    Optional<AccountRequest>
            findByAccountNumberAndPinAndStatus(
                    String accountNumber,
                    String pin,
                    String status);

    Optional<AccountRequest> findByAccountNumberAndStatus(String accountNumber, String status);

    // Custom query methods for dashboard
    long countByStatus(String status);

    List<AccountRequest> findByStatus(String status);

    // Find account details by account number (Long)
    Optional<AccountRequest> findByAccountNumber(Long accountNumber);

    Optional<AccountRequest> findByEmail(String email);

    boolean existsByAdharNo(String adharNo);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByMobile(String mobile);

}
