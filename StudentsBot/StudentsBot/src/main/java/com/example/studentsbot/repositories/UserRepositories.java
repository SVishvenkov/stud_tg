package com.example.studentsbot.repositories;

import com.example.studentsbot.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepositories extends JpaRepository<Users, Integer> {
    boolean existsByChatIdAndUserNumber(Long chatId, String userNumber);
    Optional<Users> findByChatId(Long chatId);
}
