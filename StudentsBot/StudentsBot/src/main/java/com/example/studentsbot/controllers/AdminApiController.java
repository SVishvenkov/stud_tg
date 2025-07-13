package com.example.studentsbot.controllers;

import com.example.studentsbot.DTO.DTORoles;
import com.example.studentsbot.DTO.DTOUser;
import com.example.studentsbot.services.RolesCRUDServices;
import com.example.studentsbot.services.UserCRUDServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final UserCRUDServices userService;
    private final RolesCRUDServices roleService;

    public AdminApiController(UserCRUDServices userService, RolesCRUDServices roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<DTOUser>> getAllUsers() {
        return ResponseEntity.ok(new ArrayList<>(userService.getAll()));
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody DTOUser userDto) {
        userService.create(userDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        userService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/roles")
    public ResponseEntity<List<DTORoles>> getAllRoles() {
        return ResponseEntity.ok(new ArrayList<>(roleService.getAll()));
    }
}