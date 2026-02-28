package com.example.mini_marketplace.repository;

import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySellerOrderByCreatedAtDesc(User seller);

    Optional<Product> findByIdAndSeller(Long id, User seller);

    List<Product> findByActiveTrue();
}
