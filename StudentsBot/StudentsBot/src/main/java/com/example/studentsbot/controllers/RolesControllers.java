package com.example.studentsbot.controllers;

import com.example.studentsbot.services.RolesCRUDServices;
import com.example.studentsbot.DTO.DTORoles;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;


@RestController
@RequestMapping("/api/roles")
@CrossOrigin
public class RolesControllers {


    private final RolesCRUDServices rCs;

    public RolesControllers(RolesCRUDServices rCs) {
        this.rCs = rCs;
    }

    @GetMapping("/{id}")
    public DTORoles getRolesById(@PathVariable Integer id) {
        return rCs.getById(id);
    }


    @RequestMapping
    public Collection<DTORoles> getAllRoles() {
        return rCs.getAll();
    }

    @PostMapping
    public void createRoles(@RequestBody DTORoles dtoRoles) {
        rCs.create(dtoRoles);
    }
    @PutMapping("/{id}")
    public void updateRoles(@PathVariable Integer id, @RequestBody DTORoles dtoRoles) {
        dtoRoles.setId(id);
        rCs.update(dtoRoles);
    }

    @DeleteMapping("/{id}")
    public void deleteRoles(@PathVariable Integer id) {
        rCs.delete(id);
    }

    @GetMapping("/test")
    public String test() {
        return "OK!";
    }
}
