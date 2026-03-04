package com.example.demo.service.Business;

import com.trustify.trustify.entity.Feature;
import com.trustify.trustify.entity.Plan;
import com.trustify.trustify.repository.Business.FeatureRepository;
import com.trustify.trustify.repository.Business.PlanRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@AllArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final FeatureRepository featureRepository;  // ← THÊM

    public List<Plan> getAllPlans() {
        return planRepository.findAll();
    }

    public Plan getPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found with id: " + id));
    }

    @Transactional
    public Plan savePlan(Plan plan) {
        Plan savedPlan = new Plan();
        savedPlan.setName(plan.getName());
        savedPlan.setDescription(plan.getDescription());
        savedPlan.setPrice(plan.getPrice());
        savedPlan.setDurationDays(plan.getDurationDays());
        savedPlan.setActive(plan.getActive());
        savedPlan.setFeatures(new HashSet<>());
        savedPlan = planRepository.save(savedPlan);

        if (plan.getFeatures() != null && !plan.getFeatures().isEmpty()) {
            for (Feature featureRef : plan.getFeatures()) {
                // Chỉ fetch feature theo ID, không insert mới
                if (featureRef.getId() == null) {
                    throw new RuntimeException("Feature ID is required");
                }

                Feature feature = featureRepository.findById(featureRef.getId())
                        .orElseThrow(() -> new RuntimeException("Feature not found with id: " + featureRef.getId()));

                savedPlan.getFeatures().add(feature);
            }
            savedPlan = planRepository.save(savedPlan);
        }

        return savedPlan;
    }


    @Transactional  // ← THÊM
    public Plan updatePlan(Plan plan) {
        Plan existingPlan = planRepository.findById(plan.getId())
                .orElseThrow(() -> new RuntimeException("Plan not found with id: " + plan.getId()));

        existingPlan.setName(plan.getName());
        existingPlan.setDescription(plan.getDescription());
        existingPlan.setPrice(plan.getPrice());
        existingPlan.setDurationDays(plan.getDurationDays());
        existingPlan.setActive(plan.getActive());

        // Cập nhật features nếu có
        if (plan.getFeatures() != null) {
            // Xóa tất cả features cũ
            existingPlan.getFeatures().clear();

            // Thêm features mới
            for (Feature featureRef : plan.getFeatures()) {
                Feature feature = featureRepository.findById(featureRef.getId())
                        .orElseThrow(() -> new RuntimeException("Feature not found"));
                existingPlan.getFeatures().add(feature);
            }
        }

        return planRepository.save(existingPlan);
    }

    @Transactional  // ← THÊM
    public void deletePlan(Long id) {
        if (!planRepository.existsById(id)) {
            throw new RuntimeException("Plan not found with id: " + id);
        }
        planRepository.deleteById(id);
    }
}