package com.example.studentsbot.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor()
public class DTOUser {
    private Integer id;
    @JsonProperty("user_number")
    private String userNumber;
    @JsonProperty("role_id")
    private Integer roleId;
    @JsonProperty("chat_id")
    private Long chatId;
    @JsonProperty("role_name") // Новое поле
    private String roleName;


    @Override
    public String toString() {
        return "DTOUser{" +
                "id=" + id +
                ", userNumber='" + userNumber + '\'' +
                ", roleId=" + roleId +
                ", chatId=" + chatId +
                ", roleName='" + roleName + '\'' +
                '}';
    }
}
