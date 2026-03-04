package com.example.demo.service.Business;

import com.trustify.trustify.entity.UserBusiness;
import com.trustify.trustify.repository.Business.UserBusinessRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserBusinessService {

    private final UserBusinessRepository userBusinessRepository;

    public UserBusiness createUserBusiness(UserBusiness userBusiness) {
        return userBusinessRepository.save(userBusiness);
    }

    public UserBusiness findByEmail(String email) {
        return userBusinessRepository.findByEmail(email);
    }

    public UserBusiness getUserBusinessById(Long userId) {
        return userBusinessRepository.findById(userId).orElse(null);
    }
}
