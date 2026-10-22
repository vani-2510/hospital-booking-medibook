package com.medibook.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email:}") String email,
                          @Value("${app.admin.password:}") String password,
                          @Value("${app.admin.name:MediBook Admin}") String name) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(String... args) {
        String normalizedEmail = email.strip().toLowerCase();
        if (normalizedEmail.isBlank() || password.length() < 12 || users.existsByEmail(normalizedEmail)) return;
        users.save(new User(name, normalizedEmail, passwordEncoder.encode(password), null, UserRole.ADMIN));
    }
}