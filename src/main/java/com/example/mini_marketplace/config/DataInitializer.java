package com.example.mini_marketplace.config;

import com.example.mini_marketplace.entity.Category;
import com.example.mini_marketplace.entity.Role;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.repository.CategoryRepository;
import com.example.mini_marketplace.repository.RoleRepository;
import com.example.mini_marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // ── Roles ──────────────────────────────────────────────────────────────
        for (Role.RoleName roleName : Role.RoleName.values()) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        log.info("Creating role: {}", roleName);
                        return roleRepository.save(new Role(roleName));
                    });
        }

        // ── Default admin user ──────────────────────────────────────────────────
        if (!userRepository.existsByUsername("admin")) {
            Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN).orElseThrow();
            User admin = new User();
            admin.setFullName("System Admin");
            admin.setUsername("admin");
            admin.setEmail("admin@marketplace.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
            log.info("Default admin created → username: admin | password: admin123");
        }

        // ── Default categories ──────────────────────────────────────────────────
        List<String[]> defaultCategories = List.of(
                new String[]{"Electronics",     "Phones, laptops, gadgets and accessories"},
                new String[]{"Clothing",         "Men's, women's and kids' apparel"},
                new String[]{"Books",            "Textbooks, novels and educational materials"},
                new String[]{"Home & Kitchen",   "Furniture, appliances and cookware"},
                new String[]{"Sports & Fitness", "Equipment, gear and activewear"},
                new String[]{"Beauty & Health",  "Skincare, makeup and wellness products"},
                new String[]{"Toys & Games",     "Board games, toys and hobby items"},
                new String[]{"Food & Grocery",   "Fresh produce, snacks and beverages"},
                new String[]{"Automotive",       "Car parts, accessories and tools"},
                new String[]{"Other",            "Miscellaneous products"}
        );

        for (String[] cat : defaultCategories) {
            categoryRepository.findByName(cat[0]).orElseGet(() -> {
                log.info("Creating category: {}", cat[0]);
                return categoryRepository.save(new Category(cat[0], cat[1]));
            });
        }
    }
}

