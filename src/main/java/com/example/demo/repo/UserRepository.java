package com.example.demo.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.forentity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByAccountNumberAndPin(String accountNumber, String pin);
}

