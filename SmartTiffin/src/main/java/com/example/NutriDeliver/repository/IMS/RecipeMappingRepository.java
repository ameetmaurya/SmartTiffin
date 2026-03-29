package com.example.NutriDeliver.repository.IMS;
import com.example.NutriDeliver.model.IMS.RecipeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecipeMappingRepository extends JpaRepository<RecipeMapping, Long> {
    List<RecipeMapping> findByMealKeywordIgnoreCase(String mealKeyword);
}