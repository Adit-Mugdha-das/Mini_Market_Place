package com.example.mini_marketplace.config;

import com.example.mini_marketplace.entity.Category;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.Role;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.repository.CategoryRepository;
import com.example.mini_marketplace.repository.ProductRepository;
import com.example.mini_marketplace.repository.RoleRepository;
import com.example.mini_marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
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

        // ── Demo sellers + products ─────────────────────────────────────────────
        seedDemoProducts();
    }

    private void seedDemoProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        Role sellerRole = roleRepository.findByName(Role.RoleName.ROLE_SELLER).orElseThrow();

        User sellerOne = userRepository.findByUsername("haat_seller1").orElseGet(() -> {
            User user = new User();
            user.setFullName("Amin Traders");
            user.setUsername("haat_seller1");
            user.setEmail("seller1@marketplace.com");
            user.setPassword(passwordEncoder.encode("seller123"));
            user.setRoles(Set.of(sellerRole));
            return userRepository.save(user);
        });

        User sellerTwo = userRepository.findByUsername("haat_seller2").orElseGet(() -> {
            User user = new User();
            user.setFullName("Nila Store");
            user.setUsername("haat_seller2");
            user.setEmail("seller2@marketplace.com");
            user.setPassword(passwordEncoder.encode("seller123"));
            user.setRoles(Set.of(sellerRole));
            return userRepository.save(user);
        });

        User sellerThree = userRepository.findByUsername("haat_seller3").orElseGet(() -> {
            User user = new User();
            user.setFullName("Urban Bazaar");
            user.setUsername("haat_seller3");
            user.setEmail("seller3@marketplace.com");
            user.setPassword(passwordEncoder.encode("seller123"));
            user.setRoles(Set.of(sellerRole));
            return userRepository.save(user);
        });

        Map<String, Category> categoryMap = categoryRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Category::getName, c -> c));

        List<ProductSeed> productSeeds = List.of(
                new ProductSeed("Wireless Earbuds Pro", "Noise-cancelling earbuds with 36-hour battery backup.", "Electronics", "59.99", 18, "https://images.unsplash.com/photo-1572569511254-d8f925fe2cbb?auto=format&fit=crop&w=900&q=80", sellerOne),
                new ProductSeed("Smart Fitness Watch", "AMOLED display, heart-rate tracking, and water resistance.", "Electronics", "84.50", 12, "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?auto=format&fit=crop&w=900&q=80", sellerTwo),
                new ProductSeed("Minimal Desk Lamp", "Adjustable warm/cool brightness for study and office work.", "Home & Kitchen", "29.00", 25, "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=80", sellerThree),
                new ProductSeed("Ceramic Coffee Mug Set", "Set of 4 matte-finish mugs, dishwasher safe.", "Home & Kitchen", "22.90", 30, "https://images.unsplash.com/photo-1514228742587-6b1558fcf93a?auto=format&fit=crop&w=900&q=80", sellerOne),
                new ProductSeed("Everyday Cotton T-Shirt", "Soft breathable cotton tee, unisex regular fit.", "Clothing", "14.99", 40, "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80", sellerTwo),
                new ProductSeed("Denim Jacket Classic", "Mid-weight denim jacket for all-season styling.", "Clothing", "48.75", 15, "https://images.unsplash.com/photo-1495105787522-5334e3ffa0ef?auto=format&fit=crop&w=900&q=80", sellerThree),
                new ProductSeed("Productivity Notebook", "Hardcover dotted notebook with 240 premium pages.", "Books", "11.49", 50, "https://images.unsplash.com/photo-1517842645767-c639042777db?auto=format&fit=crop&w=900&q=80", sellerOne),
                new ProductSeed("Beginner Yoga Mat", "Non-slip yoga mat with carry strap included.", "Sports & Fitness", "26.80", 22, "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=900&q=80", sellerTwo),
                new ProductSeed("Hydration Bottle 1L", "Double-wall insulated steel bottle, leakproof lid.", "Sports & Fitness", "19.25", 35, "https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=900&q=80", sellerThree),
                new ProductSeed("Vitamin C Face Serum", "Brightening serum with hyaluronic acid.", "Beauty & Health", "17.90", 28, "https://images.unsplash.com/photo-1617897903246-719242758050?auto=format&fit=crop&w=900&q=80", sellerOne),
                new ProductSeed("STEM Building Blocks", "Creative engineering block set for ages 6+.", "Toys & Games", "33.40", 16, "https://images.unsplash.com/photo-1587654780291-39c9404d746b?auto=format&fit=crop&w=900&q=80", sellerTwo),
                new ProductSeed("Organic Snack Pack", "Healthy mixed dry snacks with no added sugar.", "Food & Grocery", "12.60", 32, "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=900&q=80", sellerThree)
        );

        for (ProductSeed seed : productSeeds) {
            Category category = categoryMap.get(seed.categoryName());
            if (category == null) {
                continue;
            }
            Product product = new Product();
            product.setName(seed.name());
            product.setDescription(seed.description());
            product.setCategory(category);
            product.setPrice(new BigDecimal(seed.price()));
            product.setQuantity(seed.quantity());
            product.setImageUrl(seed.imageUrl());
            product.setSeller(seed.seller());
            product.setActive(true);
            productRepository.save(product);
        }

        log.info("Seeded demo marketplace products for UI showcase.");
    }

    private record ProductSeed(
            String name,
            String description,
            String categoryName,
            String price,
            int quantity,
            String imageUrl,
            User seller
    ) {}
}

