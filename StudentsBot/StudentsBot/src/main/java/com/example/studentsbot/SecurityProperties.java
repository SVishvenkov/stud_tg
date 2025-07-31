package com.example.studentsbot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private AdminSec adminSec = new AdminSec();


    public AdminSec getAdminSec() {
        return adminSec;
    }

    public void setAdminSec(AdminSec adminSec) {
        this.adminSec = adminSec;
    }

    public static class AdminSec {
        private String login;
        private String pass;

        public String getLogin() {
            return login;
        }

        public String getPass() {
            return pass;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public void setPass(String pass) {
            this.pass = pass;
        }
    }
}
