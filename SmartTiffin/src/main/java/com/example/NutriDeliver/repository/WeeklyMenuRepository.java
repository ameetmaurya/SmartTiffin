package com.example.NutriDeliver.repository;

import com.example.NutriDeliver.model.MealOrder;
import com.example.NutriDeliver.model.MenuPlan;
import com.example.NutriDeliver.model.WeeklyMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface WeeklyMenuRepository extends JpaRepository<WeeklyMenu, Long> {

    // ── existing queries (no plan filter — used as fallback) ──
    List<WeeklyMenu> findByDayOfWeek(DayOfWeek dayOfWeek);

    Optional<WeeklyMenu> findByDayOfWeekAndMealTypeAndMealCategory(
            DayOfWeek dayOfWeek,
            MealOrder.MealType mealType,
            String mealCategory);

    // ── NEW: plan-scoped queries ──
    List<WeeklyMenu> findByMenuPlan(MenuPlan menuPlan);

    List<WeeklyMenu> findByMenuPlanAndDayOfWeek(MenuPlan menuPlan, DayOfWeek dayOfWeek);

    Optional<WeeklyMenu> findByMenuPlanAndDayOfWeekAndMealTypeAndMealCategory(
            MenuPlan menuPlan,
            DayOfWeek dayOfWeek,
            MealOrder.MealType mealType,
            String mealCategory);

    // ── delete all menus for a plan (used when admin deletes a plan) ──
    void deleteByMenuPlan(MenuPlan menuPlan);
}