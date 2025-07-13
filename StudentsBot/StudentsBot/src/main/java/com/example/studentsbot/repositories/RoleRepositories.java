package com.example.studentsbot.repositories;

import com.example.studentsbot.entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepositories extends JpaRepository<Roles, Integer> {

    Optional<Roles> findById(Integer id);
}
