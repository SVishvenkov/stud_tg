package com.example.studentsbot.services;

import com.example.studentsbot.DTO.DTOUser;
import com.example.studentsbot.entity.Roles;
import com.example.studentsbot.entity.Users;
import com.example.studentsbot.repositories.RoleRepositories;
import com.example.studentsbot.repositories.UserRepositories;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserCRUDServices implements CRUDServices<DTOUser> {

    @Autowired
    private final UserRepositories userRep;
    private final RoleRepositories roleRep;

    @Override
    public DTOUser getById(Integer id) {
        Optional<Users> user = Optional.of(userRep.findById(id).orElseThrow());
        log.info("Get By Id: " + id);
        return user.map(UserCRUDServices::mapToDTO).orElse(null);
    }

    @Override
    public Collection<DTOUser> getAll() {
        log.info("Get All");

        return userRep.findAll()
                .stream()
                .map(UserCRUDServices::mapToDTO)
                .toList();
    }

    @Override
    public void create(DTOUser dtoUser) {
        log.info("Create");
        Users users = mpaToEntity(dtoUser);
        userRep.save(users);
    }

    @Override
    public void update(DTOUser dtoUser) {
        log.info("Create");
        Users users = mpaToEntity(dtoUser);
        userRep.save(users);

    }

    @Override
    public void delete(Integer id) {
        log.info("Delete: " + id);
        userRep.deleteById(id);
    }

    public static Users mpaToEntity(DTOUser dtoUser) {
        if (dtoUser == null || dtoUser.getUserNumber() == null) {
            throw new IllegalArgumentException("UserNumber cannot be null");
        }
        Users users = new Users();
        users.setId(dtoUser.getId());
        users.setUserNumber(dtoUser.getUserNumber());
        users.setChatId(dtoUser.getChatId());

        if (dtoUser.getRoleId() != null) {
            Roles role = new Roles();
            role.setId(dtoUser.getRoleId());
            users.setRole(role);
        }
        return users;
    }

    /*Отправляет пользователю*/
    public static DTOUser mapToDTO(Users users) {
        DTOUser dtoUser = new DTOUser();
        dtoUser.setId(users.getId());
        dtoUser.setUserNumber(users.getUserNumber());
        dtoUser.setChatId(users.getChatId());
        if (users.getRole() != null) {
            dtoUser.setRoleId(users.getRole().getId());
            dtoUser.setRoleName(users.getRole().getName());
        }
        return dtoUser;
    }


}
