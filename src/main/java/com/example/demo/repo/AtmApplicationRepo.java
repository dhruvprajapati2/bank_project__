package com.example.demo.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.forentity.AtmApplication;

public interface AtmApplicationRepo extends JpaRepository<AtmApplication, Long> {
    List<AtmApplication> findAllByOrderByAppliedAtDesc();
    boolean existsByAccountNumber(Long accountNumber);
    boolean existsByAtmCardNumber(String atmCardNumber);
}
