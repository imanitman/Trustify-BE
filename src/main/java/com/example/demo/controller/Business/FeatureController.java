package com.example.demo.controller.Business;

import com.trustify.trustify.dto.Req.ReqFeatureDto;
import com.trustify.trustify.entity.Feature;
import com.trustify.trustify.service.Business.FeatureService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/feature")
public class FeatureController {

    private final FeatureService featureService;

    @GetMapping
    public ResponseEntity<List<Feature>> getAllFeatures() {
        return ResponseEntity.ok(featureService.getAllFeatures());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Feature> getFeatureById(@PathVariable Long id) {
        return ResponseEntity.ok(featureService.getFeatureById(id));
    }

    @GetMapping("/plan/{planId}")
    public ResponseEntity<List<Feature>> getFeaturesByPlan(@PathVariable Long planId) {
        return ResponseEntity.ok(featureService.getFeaturesByPlanId(planId));
    }

    @PostMapping
    public ResponseEntity<Feature> createFeature(@RequestBody ReqFeatureDto featureDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(featureService.saveFeature(featureDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Feature> updateFeature(@PathVariable Long id, @RequestBody Feature feature) {
        feature.setId(id);
        return ResponseEntity.ok(featureService.updateFeature(feature));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeature(@PathVariable Long id) {
        featureService.deleteFeature(id);
        return ResponseEntity.noContent().build();
    }
}
