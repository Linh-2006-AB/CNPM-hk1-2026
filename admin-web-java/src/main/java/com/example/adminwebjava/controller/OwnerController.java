package com.example.adminwebjava.controller;

import com.example.adminwebjava.entity.OwnerProfile;
import com.example.adminwebjava.repository.OwnerProfileRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/owner")
public class OwnerController {
    private final OwnerProfileRepository ownerProfileRepository;
    public OwnerController(OwnerProfileRepository ownerProfileRepository) { this.ownerProfileRepository = ownerProfileRepository; }

    @GetMapping({"", "/dashboard"})
    public String dashboard(HttpSession session, Model model) {
        Integer userId = (Integer) session.getAttribute("authUserId");
        OwnerProfile profile = userId == null ? null : ownerProfileRepository.findByUserId(userId).orElse(null);
        model.addAttribute("profile", profile);
        return "owner/dashboard";
    }
}
