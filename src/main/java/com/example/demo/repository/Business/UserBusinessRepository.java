package com.example.demo.repository.Business;

import com.trustify.trustify.entity.UserBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBusinessRepository extends JpaRepository<UserBusiness, Long> {
    UserBusiness findByEmail(String email);
}
