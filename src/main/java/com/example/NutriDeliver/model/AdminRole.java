package com.example.NutriDeliver.model;

public enum AdminRole {
    SUPER_ADMIN,   // Full access — create, edit, delete everything
    HUB_MANAGER,   // Can only see and manage their assigned hub's data
    VIEWER         // Read-only — no create/edit/delete buttons anywhere
}