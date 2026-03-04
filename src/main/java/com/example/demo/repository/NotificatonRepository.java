package com.example.demo.repository;

import com.trustify.trustify.entity.Notification;
import com.trustify.trustify.entity.User;
import com.trustify.trustify.entity.UserBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificatonRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByIdDesc(User user);

    List<Notification> findByUserBusinessOrderByIdDesc(UserBusiness userBusiness);

}
