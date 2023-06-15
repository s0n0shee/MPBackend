package com.example.mp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.mp.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>{
	@Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE %?1% OR LOWER(p.description) LIKE %?1%")
    List<Product> findBySearchQuery(String searchQuery);
	
	@Query("SELECT p FROM Product p WHERE LOWER(p.category) = LOWER(?1)")
    List<Product> findByCategory(String category);
}
