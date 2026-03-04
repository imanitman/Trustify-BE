package com.example.demo.repository.Business;

import com.trustify.trustify.entity.Payment;
import com.trustify.trustify.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTxnRef(String txnRef);

    List<Payment> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<Payment> findByStatus(PaymentStatus status);
}