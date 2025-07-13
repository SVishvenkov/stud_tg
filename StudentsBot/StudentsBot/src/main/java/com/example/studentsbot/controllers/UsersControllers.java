package com.example.studentsbot.controllers;

import com.example.studentsbot.entity.Roles;
import com.example.studentsbot.repositories.RoleRepositories;
import com.example.studentsbot.services.RolesCRUDServices;
import com.example.studentsbot.services.UserCRUDServices;
import com.example.studentsbot.DTO.DTOUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/users")
@CrossOrigin
@RequiredArgsConstructor
public class UsersControllers {

    private final UserCRUDServices ucs;
    private final RoleRepositories roleRep;

    @GetMapping("/test")
    public String test() {
        return "OK!";
    }

    @GetMapping
    public Collection<DTOUser> getAll() {
        return ucs.getAll();
    }

    @PostMapping
    public void create(@RequestBody DTOUser dtoUser) {
        log.info("Received DTO: {}", dtoUser);
        ucs.create(dtoUser);
    }


    @PutMapping("/{id}")
    public void update(@PathVariable Integer id, @RequestBody DTOUser dtoUser) {
        dtoUser.setId(id);
        ucs.update(dtoUser);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Integer id) {
        ucs.delete(id);
    }
}

