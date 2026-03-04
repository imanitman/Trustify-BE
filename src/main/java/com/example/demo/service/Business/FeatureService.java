package com.example.demo.service.Business;

import com.trustify.trustify.dto.Req.ReqFeatureDto;
import com.trustify.trustify.entity.Feature;
import com.trustify.trustify.entity.Plan;
import com.trustify.trustify.repository.Business.FeatureRepository;
import com.trustify.trustify.repository.Business.PlanRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class FeatureService {

    private final FeatureRepository featureRepository;
    private final PlanRepository planRepository;  // ← THÊM DÒNG NÀY

    public List<Feature> getAllFeatures() {
        return featureRepository.findAll();
    }

    public Feature getFeatureById(Long id) {
        return featureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feature not found with id: " + id));
    }

    public List<Feature> getFeaturesByPlanId(Long planId) {
        return featureRepository.findByPlansId(planId);
    }

    @Transactional  // ← THÊM @Transactional
    public Feature saveFeature(ReqFeatureDto featureDto) {
        // Lưu feature trước (không có plans để tránh lỗi)
        Feature savedFeature = new Feature();
        savedFeature.setName(featureDto.getName());
        savedFeature.setDescription(featureDto.getDescription());
        savedFeature = featureRepository.save(savedFeature);

        // Nếu có plans trong request, gán feature vào các plans đó
        if (featureDto.getPlans() != null && !featureDto.getPlans().isEmpty()) {
            for (Long  planRef : featureDto.getPlans()) {
                Plan plan = planRepository.findById(planRef)
                        .orElseThrow(() -> new RuntimeException("Plan not found with id: " + planRef));
                // Thêm feature vào plan
                plan.getFeatures().add(savedFeature);
                planRepository.save(plan);
            }
        }

        return savedFeature;
    }

    @Transactional  // ← THÊM @Transactional
    public Feature updateFeature(Feature feature) {
        Feature existingFeature = featureRepository.findById(feature.getId())
                .orElseThrow(() -> new RuntimeException("Feature not found with id: " + feature.getId()));

        existingFeature.setName(feature.getName());
        existingFeature.setDescription(feature.getDescription());

        // Cập nhật plans nếu có
        if (feature.getPlans() != null) {
            // Xóa feature khỏi tất cả plans cũ
            List<Plan> allPlans = planRepository.findAll();
            for (Plan plan : allPlans) {
                plan.getFeatures().remove(existingFeature);
            }
            planRepository.saveAll(allPlans);

            // Thêm vào plans mới
            for (Plan planRef : feature.getPlans()) {
                Plan plan = planRepository.findById(planRef.getId())
                        .orElseThrow(() -> new RuntimeException("Plan not found"));
                plan.getFeatures().add(existingFeature);
                planRepository.save(plan);
            }
        }

        return featureRepository.save(existingFeature);
    }

    @Transactional  // ← THÊM @Transactional
    public void deleteFeature(Long id) {
        Feature feature = featureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feature not found with id: " + id));

        // Xóa feature khỏi tất cả plans trước
        List<Plan> allPlans = planRepository.findAll();
        for (Plan plan : allPlans) {
            plan.getFeatures().remove(feature);
        }
        planRepository.saveAll(allPlans);

        // Sau đó xóa feature
        featureRepository.deleteById(id);
    }
}