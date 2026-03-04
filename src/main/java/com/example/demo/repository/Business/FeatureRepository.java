package com.example.demo.repository.Business;

import com.trustify.trustify.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {

    List<Feature> findByPlansId(Long planId);
}
