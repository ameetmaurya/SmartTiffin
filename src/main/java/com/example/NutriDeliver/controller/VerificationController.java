package com.example.NutriDeliver.controller;

import com.example.NutriDeliver.model.*;
import com.example.NutriDeliver.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class VerificationController {

    @Autowired private UserRepository userRepo;
    @Autowired private VerificationDocumentRepository verDocRepo;

    // ── HELPER: safe status getter (handles null for old DB rows) ─────
    private VerificationStatus getStatus(User user) {
        if (user.getVerificationStatus() == null)
            return VerificationStatus.NOT_SUBMITTED;
        return user.getVerificationStatus();
    }

    // ── VERIFICATION PAGE (Cook + Driver) ────────────────────────────

    @GetMapping("/verify")
    public String verifyPage(Principal principal, Model model) {
        User user = userRepo.findByEmail(principal.getName());
        model.addAttribute("user", user);

        VerificationDocument doc = verDocRepo.findByUser(user).orElse(null);
        model.addAttribute("doc", doc);

        VerificationStatus status = getStatus(user);
        model.addAttribute("status", status);

        return "verify";
    }

    // ── STEP 1: SEND MOBILE OTP ───────────────────────────────────────

    @PostMapping("/verify/send-otp")
    public String sendOtp(Principal principal) {
        User user = userRepo.findByEmail(principal.getName());

        VerificationDocument doc = verDocRepo.findByUser(user).orElseGet(() -> {
            VerificationDocument d = new VerificationDocument();
            d.setUser(user);
            return d;
        });

        String otp = String.format("%06d", new Random().nextInt(900000) + 100000);
        doc.setMobileOtp(otp);
        doc.setMobileVerified(false);
        verDocRepo.save(doc);

        // TODO: replace with real SMS (Twilio / Fast2SMS)
        // Dev mode: OTP shown on screen
        return "redirect:/verify?msg=OTPSent&devOtp=" + otp;
    }

    // ── STEP 2: VERIFY MOBILE OTP ─────────────────────────────────────

    @PostMapping("/verify/confirm-otp")
    public String confirmOtp(@RequestParam String otp, Principal principal) {
        User user = userRepo.findByEmail(principal.getName());
        VerificationDocument doc = verDocRepo.findByUser(user).orElse(null);

        if (doc == null || doc.getMobileOtp() == null || !otp.trim().equals(doc.getMobileOtp()))
            return "redirect:/verify?error=InvalidOTP";

        doc.setMobileVerified(true);
        verDocRepo.save(doc);
        return "redirect:/verify?msg=MobileVerified";
    }

    // ── STEP 3: UPLOAD DOCUMENTS ──────────────────────────────────────

    @PostMapping("/verify/upload-docs")
    public String uploadDocs(
            @RequestParam String idProofType,
            @RequestParam MultipartFile idProofFile,
            @RequestParam MultipartFile selfieFile,
            Principal principal) throws IOException {

        User user = userRepo.findByEmail(principal.getName());
        VerificationDocument doc = verDocRepo.findByUser(user).orElseGet(() -> {
            VerificationDocument d = new VerificationDocument();
            d.setUser(user);
            return d;
        });

        if (!doc.isMobileVerified())
            return "redirect:/verify?error=MobileNotVerified";

        String uploadDir = "uploads/verification/";
        new File(uploadDir).mkdirs();

        if (idProofFile != null && !idProofFile.isEmpty()) {
            String idFileName = UUID.randomUUID() + "_" + sanitize(idProofFile.getOriginalFilename());
            Files.copy(idProofFile.getInputStream(),
                    Paths.get(uploadDir + idFileName),
                    StandardCopyOption.REPLACE_EXISTING);
            doc.setIdProofUrl("/uploads/verification/" + idFileName);
            doc.setIdProofType(idProofType);
        }

        if (selfieFile != null && !selfieFile.isEmpty()) {
            String selfieFileName = UUID.randomUUID() + "_" + sanitize(selfieFile.getOriginalFilename());
            Files.copy(selfieFile.getInputStream(),
                    Paths.get(uploadDir + selfieFileName),
                    StandardCopyOption.REPLACE_EXISTING);
            doc.setSelfieUrl("/uploads/verification/" + selfieFileName);
        }

        doc.setSubmittedAt(LocalDateTime.now());
        verDocRepo.save(doc);

        user.setVerificationStatus(VerificationStatus.PENDING);
        userRepo.save(user);

        return "redirect:/verify?msg=DocsSubmitted";
    }

    // ── HELPER ────────────────────────────────────────────────────────

    private String sanitize(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}