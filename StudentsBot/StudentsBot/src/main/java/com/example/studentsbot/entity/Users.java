package com.example.studentsbot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_number", nullable = false)
    private String userNumber;

    @Column(name = "chat_id")
    private Long chatId;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Roles role;

    public boolean hasRole(String roleName) {
        return this.role != null && this.role.getName().equalsIgnoreCase(roleName);
    }

}
