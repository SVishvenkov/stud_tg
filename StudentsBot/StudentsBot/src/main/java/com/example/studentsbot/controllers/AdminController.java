package com.example.studentsbot.controllers;

import com.example.studentsbot.services.RolesCRUDServices;
import com.example.studentsbot.services.UserCRUDServices;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {


    private final UserCRUDServices userService;
    private final RolesCRUDServices roleService;

    public AdminController(UserCRUDServices userService, RolesCRUDServices roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping
    public String adminPanel(Model model) {
        model.addAttribute("users", userService.getAll());
        model.addAttribute("roles", roleService.getAll());
        return "forward:/index.html";
    }
}
