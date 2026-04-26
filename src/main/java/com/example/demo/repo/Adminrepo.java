package com.example.demo.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.forentity.Admin;

public interface Adminrepo extends JpaRepository<Admin, Long> {

	 Optional<Admin> findByEmail(String email);

}
