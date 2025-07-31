package com.example.studentsbot.security;

import com.example.studentsbot.SecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final SecurityProperties securityProperties;

    @Autowired
    public SecurityConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/api/roles/**").permitAll() // Разрешаем эндпоинты ролей
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "BARMEN", "WAITER")
                        .requestMatchers("/api/roles/**").hasRole("ADMIN")
                        .anyRequest().authenticated()  // Требует аутентификации для всех запросов
                )
                .formLogin(form -> form
                        .defaultSuccessUrl("/", true)  // Перенаправление после успешного входа
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails user = User.builder()
                .username(securityProperties.getAdminSec().getLogin())  // Логин
                .password(passwordEncoder().encode(securityProperties.getAdminSec().getPass()))   // Пароль (`{noop}` означает "без шифрования")
                .roles("ADMIN")  // Роль
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
