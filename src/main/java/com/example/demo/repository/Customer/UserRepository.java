package com.example.demo.repository.Customer;

import com.trustify.trustify.entity.User;
import com.trustify.trustify.enums.AuthProvider;
import com.trustify.trustify.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    boolean existsByEmail(String email);

    List<User> findByStatus(UserStatus userStatus);
}

