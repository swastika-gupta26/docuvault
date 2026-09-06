package com.docuvault.docuvault.repository;

import com.docuvault.docuvault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
