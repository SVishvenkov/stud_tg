package com.example.studentsbot.repositories;

import com.example.studentsbot.entity.Users;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepositories extends JpaRepository<Users, Integer> {
    boolean existsByChatIdAndUserNumber(Long chatId, String userNumber);

    @EntityGraph(attributePaths = {"role"}) // Жадная загрузка роли
    Optional<Users> findByChatId(Long chatId);

    // Добавляем метод для проверки существования пользователя с определенной ролью
    boolean existsByChatIdAndRole_Name(Long chatId, String roleName);

//    Optional<Users> findByChatId(Long chatId);
}
