package com.example.NutriDeliver.model;

import jakarta.persistence.*;
import java.time.DayOfWeek;

@Entity
public class WeeklyMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    @Enumerated(EnumType.STRING)
    private MealOrder.MealType mealType;

    private String mealCategory;   // "Pure Veg" | "Non-Veg"
    private String items;
    private int    calories;
    private int    protein;
    private String imageUrl;

    // NEW: which plan this menu row belongs to.
    // Nullable for backwards-compatibility — rows without a plan are treated
    // as belonging to the default/active plan.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_plan_id")
    private MenuPlan menuPlan;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public MealOrder.MealType getMealType() { return mealType; }
    public void setMealType(MealOrder.MealType mealType) { this.mealType = mealType; }

    public String getMealCategory() { return mealCategory; }
    public void setMealCategory(String mealCategory) { this.mealCategory = mealCategory; }

    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public int getProtein() { return protein; }
    public void setProtein(int protein) { this.protein = protein; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public MenuPlan getMenuPlan() { return menuPlan; }
    public void setMenuPlan(MenuPlan menuPlan) { this.menuPlan = menuPlan; }
}