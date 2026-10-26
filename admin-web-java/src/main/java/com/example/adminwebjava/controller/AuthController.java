package com.example.adminwebjava.controller;

import com.example.adminwebjava.entity.OwnerProfile;
import com.example.adminwebjava.entity.Role;
import com.example.adminwebjava.entity.User;
import com.example.adminwebjava.entity.Tourist;
import com.example.adminwebjava.repository.OwnerProfileRepository;
import com.example.adminwebjava.repository.RoleRepository;
import com.example.adminwebjava.repository.TouristRepository;
import com.example.adminwebjava.repository.UserRepository;
import com.example.adminwebjava.services.PasswordHashService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@Controller
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final TouristRepository touristRepository;
    private final RoleRepository roleRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PasswordHashService passwordHashService;

    public AuthController(UserRepository userRepository, TouristRepository touristRepository,
                          RoleRepository roleRepository, OwnerProfileRepository ownerProfileRepository,
                          PasswordHashService passwordHashService) {
        this.userRepository = userRepository;
        this.touristRepository = touristRepository;
        this.roleRepository = roleRepository;
        this.ownerProfileRepository = ownerProfileRepository;
        this.passwordHashService = passwordHashService;
    }

    @GetMapping({"", "/login"})
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String registered,
                            Model model) {
        if (error != null) model.addAttribute("error", "Sai tài khoản/email hoặc mật khẩu, hoặc tài khoản đã bị khóa.");
        if (registered != null) model.addAttribute("success", "Đăng ký thành công. Bro có thể đăng nhập ngay.");
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String identifier, @RequestParam String password,
                        HttpSession session, Model model) {
        String value = identifier == null ? "" : identifier.trim();
        User user = userRepository.findByUsername(value).orElseGet(() -> userRepository.findByEmail(value).orElse(null));
        if (user != null && "active".equalsIgnoreCase(user.getStatus())
                && passwordHashService.matches(password, user.getPasswordHash())) {
            String role = user.getRole() == null ? "" : user.getRole().getRoleName();
            session.setAttribute("authUserId", user.getId());
            session.setAttribute("authUsername", user.getUsername());
            session.setAttribute("authEmail", user.getEmail());
            session.setAttribute("authRole", role);
            session.setAttribute("authUserType", "SYSTEM");
            if ("Owner".equalsIgnoreCase(role)) return "redirect:/owner/dashboard";
            return "redirect:/admin/dashboard";
        }

        Tourist tourist = touristRepository.findByEmail(value).orElse(null);
        if (tourist != null && "local".equalsIgnoreCase(tourist.getAuthProvider())
                && passwordHashService.matches(password, tourist.getPasswordHash())) {
            session.setAttribute("authUserId", tourist.getId());
            session.setAttribute("authUsername", tourist.getFullName() == null || tourist.getFullName().isBlank() ? tourist.getEmail() : tourist.getFullName());
            session.setAttribute("authEmail", tourist.getEmail());
            session.setAttribute("authRole", "User");
            session.setAttribute("authUserType", "TOURIST");
            return "redirect:/";
        }

        return "redirect:/auth/login?error=1";
    }

    @GetMapping("/register")
    public String registerHome() { return "auth/register"; }

    @GetMapping("/register/admin")
    public String registerAdmin() { return "auth/register-admin"; }

    @GetMapping("/register/user")
    public String registerUser() { return "auth/register-user"; }

    @GetMapping("/register/owner")
    public String registerOwner() { return "auth/register-owner"; }

    @PostMapping("/register/admin")
    public String registerAdmin(@RequestParam String username, @RequestParam String email,
                                @RequestParam String password, @RequestParam String confirmPassword,
                                Model model) {
        if (!password.equals(confirmPassword)) return registerError("auth/register-admin", model, "Mật khẩu xác nhận không khớp.");
        if (userRepository.existsByUsername(username.trim()) || userRepository.existsByEmail(email.trim()))
            return registerError("auth/register-admin", model, "Username hoặc email đã tồn tại.");
        Role role = roleRepository.findByRoleName("Admin").orElseThrow(() -> new IllegalStateException("Thiếu role Admin trong database."));
        User user = new User();
        user.setUsername(username.trim()); user.setEmail(email.trim().toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordHashService.hash(password)); user.setStatus("active"); user.setRole(role);
        userRepository.save(user);
        return "redirect:/auth/login?registered=1";
    }

    @PostMapping("/register/user")
    public String registerUser(@RequestParam String fullName, @RequestParam String email,
                               @RequestParam String password, @RequestParam String confirmPassword,
                               Model model) {
        if (!password.equals(confirmPassword)) return registerError("auth/register-user", model, "Mật khẩu xác nhận không khớp.");
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (touristRepository.findByEmail(normalizedEmail).isPresent())
            return registerError("auth/register-user", model, "Email đã được đăng ký.");
        Tourist tourist = new Tourist();
        tourist.setFullName(fullName.trim()); tourist.setEmail(normalizedEmail);
        tourist.setPasswordHash(passwordHashService.hash(password)); tourist.setAuthProvider("local");
        touristRepository.save(tourist);
        return "redirect:/auth/login?registered=1";
    }

    @Transactional
    @PostMapping("/register/owner")
    public String registerOwner(@RequestParam String username, @RequestParam String email,
                                @RequestParam String businessName, @RequestParam(required = false) String representativeName,
                                @RequestParam(required = false) String phone, @RequestParam(required = false) String address,
                                @RequestParam String password, @RequestParam String confirmPassword,
                                Model model) {
        if (!password.equals(confirmPassword)) return registerError("auth/register-owner", model, "Mật khẩu xác nhận không khớp.");
        if (userRepository.existsByUsername(username.trim()) || userRepository.existsByEmail(email.trim()))
            return registerError("auth/register-owner", model, "Username hoặc email đã tồn tại.");
        Role role = roleRepository.findByRoleName("Owner").orElseThrow(() -> new IllegalStateException("Thiếu role Owner trong database."));
        User user = new User();
        user.setUsername(username.trim()); user.setEmail(email.trim().toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordHashService.hash(password)); user.setStatus("active"); user.setRole(role);
        userRepository.save(user);

        OwnerProfile profile = new OwnerProfile();
        profile.setUserId(user.getId()); profile.setBusinessName(businessName.trim());
        profile.setRepresentativeName(blankToNull(representativeName)); profile.setPhone(blankToNull(phone));
        profile.setAddress(blankToNull(address)); profile.setStatus("Pending");
        ownerProfileRepository.save(profile);
        return "redirect:/auth/login?registered=1";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/?logout=1";
    }

    private String registerError(String view, Model model, String message) {
        model.addAttribute("error", message);
        return view;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
