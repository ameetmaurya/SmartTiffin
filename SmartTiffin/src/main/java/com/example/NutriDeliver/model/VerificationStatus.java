package com.example.NutriDeliver.model;

public enum VerificationStatus {
    NOT_SUBMITTED,   // just registered, hasn't uploaded docs yet
    PENDING,         // docs uploaded, waiting for admin review
    APPROVED,        // admin approved — can use platform
    REJECTED         // admin rejected — must resubmit
}