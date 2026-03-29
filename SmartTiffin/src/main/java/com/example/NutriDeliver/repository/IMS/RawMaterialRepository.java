package com.example.NutriDeliver.repository.IMS;

import com.example.NutriDeliver.model.IMS.RawMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawMaterialRepository extends JpaRepository<RawMaterial, Long> {}