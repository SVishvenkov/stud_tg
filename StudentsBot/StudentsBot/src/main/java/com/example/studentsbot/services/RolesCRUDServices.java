package com.example.studentsbot.services;

import com.example.studentsbot.DTO.DTORoles;
import com.example.studentsbot.entity.Roles;
import com.example.studentsbot.repositories.RoleRepositories;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
@Service
public class RolesCRUDServices implements CRUDServices<DTORoles> {

    @Autowired
    private final RoleRepositories roleRep;

    @Override
    public DTORoles getById(Integer id) {
        log.info("Get by id: " + id);
        Optional<Roles> roles = roleRep.findById(id);
        return mapToDTO(roleRep.findById(id).orElseThrow());
    }

    @Override
    public Collection<DTORoles> getAll() {
        return roleRep.findAll()
                .stream()
                .map(RolesCRUDServices::mapToDTO)
                .toList();
    }

    @Override
    public void create(DTORoles dtoRoles) {
        Roles roles = mapToEntity(dtoRoles);
        roleRep.save(roles);
    }

    @Override
    public void update(DTORoles dtoRoles) {
        Roles roles = mapToEntity(dtoRoles);
        roleRep.save(roles);
    }

    @Override
    public void delete(Integer id) {
        roleRep.deleteById(id);
    }

    public static Roles mapToEntity(DTORoles dtoRoles) {
        Roles roles = new Roles();
        roles.setId(dtoRoles.getId());
        roles.setName(dtoRoles.getName());
        return roles;

    }

    public static DTORoles mapToDTO(Roles roles) {
        if (roles == null) return null;
        DTORoles dtoRoles = new DTORoles();
        dtoRoles.setId(roles.getId());
        dtoRoles.setName(roles.getName());
        return dtoRoles;
    }

}
