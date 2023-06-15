package com.example.mp.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.mp.model.Cart;
import com.example.mp.model.LoginDto;
import com.example.mp.model.Product;
import com.example.mp.model.ProductQuantity;
import com.example.mp.model.User;
import com.example.mp.model.UserDto;
import com.example.mp.repository.ProductRepository;
import com.example.mp.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;


@CrossOrigin
@RestController
@RequestMapping("/api")
public class UserController {

  @Autowired
  private UserRepository userRepository;
  
  @Autowired
  private ProductRepository productRepository;
  
  @PostMapping("/signin")
  public ResponseEntity<String> signUp(@RequestBody UserDto userDto) {
    // Validate inputs and create a new user object
    User user = validateAndCreateUser(userDto);
    if (user == null) {
      return ResponseEntity.badRequest().body("Invalid user details.");
    }
    
    // Save the user to the database
    userRepository.save(user);
    
    return ResponseEntity.ok("User registered successfully.");
  }
  
  @PostMapping("/login")
  public ResponseEntity<String> login(@RequestBody LoginDto loginDto, HttpServletRequest request) {
      // Validate inputs
      if (loginDto.getUsername().isEmpty() || loginDto.getPassword().isEmpty()) {
          return ResponseEntity.badRequest().body("Username and password are required.");
      }

      // Find the user by username
      User user = userRepository.findByUsername(loginDto.getUsername());

      // Check if the user exists and the password matches
      if (user == null || !user.getPassword().equals(loginDto.getPassword())) {
          return ResponseEntity.badRequest().body("Invalid username or password.");
      }

      // Authentication successful
      HttpSession session = request.getSession();
      session.setAttribute("username", user.getUsername());
      return ResponseEntity.ok("Login successful.");
  }


  // Helper method to validate user inputs and create a new User object
  private User validateAndCreateUser(UserDto userDto) {
    if (userDto.getFirstName().isEmpty() ||
        userDto.getLastName().isEmpty() ||
        userDto.getUsername().isEmpty() ||
        userDto.getEmail().isEmpty() ||
        userDto.getPassword().isEmpty() ||
        userDto.getConfirmPassword().isEmpty()) {
      return null;
    }
    
    // Check if the username or email is already registered
    if (userRepository.existsByUsername(userDto.getUsername())) {
      return null;
    }
    
    if (userRepository.existsByEmail(userDto.getEmail())) {
      return null;
    }
    
    // Check if the password and confirm password match
    if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
      return null;
    }
    
    // Create a new user object
    User user = new User();
    user.setFirstName(userDto.getFirstName());
    user.setLastName(userDto.getLastName());
    user.setUsername(userDto.getUsername());
    user.setEmail(userDto.getEmail());
    user.setPassword(userDto.getPassword());
    
    return user;
  }
  
  @PostMapping("/logout")
  public ResponseEntity<String> logout(HttpServletRequest request) {
      // Check if the user is logged in
      HttpSession session = request.getSession(false);
      if (session == null || session.getAttribute("username") == null) {
          return ResponseEntity.badRequest().body("User is not logged in.");
      }

      // Invalidate the session to log out the user
      session.invalidate();
      return ResponseEntity.ok("Logout successful.");
  }

  
  @PostMapping("add-cart")
  public ResponseEntity<String> addToCart(@RequestBody String productId, HttpServletRequest request) {
      // Check if the user is logged in
      HttpSession session = request.getSession(false);
      if (session == null || session.getAttribute("username") == null) {
          return ResponseEntity.badRequest().body("User is not logged in.");
      }

      // Validate and parse the productId
      if (productId.isEmpty()) {
          return ResponseEntity.badRequest().body("Product ID is required.");
      }

      try {
          Long productIdLong = Long.parseLong(productId);
          // Find the user by username
          String username = (String) session.getAttribute("username");
          User user = userRepository.findByUsername(username);

          // Get or create the user's cart
          Cart cart = user.getCart();
          if (cart == null) {
              // Retrieve the User object or create a new User object
              user = userRepository.findById(user.getId()).orElseThrow(() -> new RuntimeException("User not found"));

              // Create a new Cart object and associate it with the User
              cart = new Cart();
              cart.setUser(user);

              // Add any other cart initialization logic here
              // ...

              user.setCart(cart);
          }

          // Find the product by ID
          Optional<Product> optionalProduct = productRepository.findById(productIdLong);
          if (optionalProduct.isPresent()) {
              Product product = optionalProduct.get();
              // Check if the product already exists in the cart
              boolean productExistsInCart = false;
              for (Product cartProduct : cart.getProducts()) {
                  if (cartProduct.getId().equals(productIdLong)) {
                      // If the product exists, increase the quantity
                      cartProduct.setQuantity(cartProduct.getQuantity() + 1);
                      productExistsInCart = true;
                      break;
                  }
              }
              if (!productExistsInCart) {
                  // If the product doesn't exist, add it to the cart with quantity 1
                  product.setQuantity(1);
                  cart.getProducts().add(product);
              }

              userRepository.save(user);
              return ResponseEntity.ok("Product added to cart successfully.");
          } else {
              return ResponseEntity.badRequest().body("Product not found.");
          }
      } catch (NumberFormatException e) {
          return ResponseEntity.badRequest().body("Invalid product ID.");
      }
  }


  @GetMapping("/cart")
  public ResponseEntity<List<ProductQuantity>> getCartItems(HttpServletRequest request) {
      // Check if the user is logged in
      HttpSession session = request.getSession(false);
      if (session == null || session.getAttribute("username") == null) {
          return ResponseEntity.badRequest().body(null); // Return a bad request status if the user is not logged in
      }

      // Find the user by username
      String username = (String) session.getAttribute("username");
      User user = userRepository.findByUsername(username);

      // Get the cart from the user
      Cart cart = user.getCart();
      if (cart == null || cart.getProducts().isEmpty()) {
          return ResponseEntity.ok(Collections.emptyList()); // Return an empty list if the cart is empty
      }

      // Create a map to store the product count
      Map<Product, Integer> productCountMap = new HashMap<>();

      // Iterate over the cart items and count each product occurrence
      for (Product product : cart.getProducts()) {
          productCountMap.put(product, productCountMap.getOrDefault(product, 0) + 1);
      }

      // Create a list of ProductQuantity objects
      List<ProductQuantity> cartItems = new ArrayList<>();
      for (Map.Entry<Product, Integer> entry : productCountMap.entrySet()) {
          Product product = entry.getKey();
          int count = entry.getValue();
          ProductQuantity productQuantity = new ProductQuantity(product, count);
          cartItems.add(productQuantity);
      }

      System.out.println("Cart ID: " + cart.getId());
      System.out.println("User ID: " + user.getId());
      System.out.println("Number of items in cart: " + cartItems.size());

      return ResponseEntity.ok(cartItems);
  }

}
