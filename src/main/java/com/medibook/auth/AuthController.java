package com.medibook.auth;

import com.medibook.security.JwtService;
import com.medibook.doctor.Doctor;
import com.medibook.doctor.DoctorRepository;
import com.medibook.user.User;
import com.medibook.user.UserRepository;
import com.medibook.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final DoctorRepository doctors;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserRepository users, DoctorRepository doctors, PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users;
        this.doctors = doctors;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        String phone = request.phone().strip();
        if (users.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this phone number already exists");
        }
        User user = users.save(new User(request.name().strip(), email,
            passwordEncoder.encode(request.password()), phone, UserRole.PATIENT));
        return response(user);
    }

    @PostMapping("/register-doctor-request")
    public AuthResponse registerDoctorRequest(@Valid @RequestBody DoctorRegistrationRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        String phone = request.phone().strip();
        if (users.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this phone number already exists");
        }
        User account = users.save(new User(request.name().strip(), email,
            passwordEncoder.encode(request.password()), phone, UserRole.DOCTOR));
        Doctor doctor = new Doctor(account, request.department().strip(), request.specialization().strip(),
                request.qualifications(), request.experienceYears(), String.join(",", request.availableDays()),
                String.join(",", request.availableSlots()), request.consultationFee(), request.bio());
        doctor.setActive(false);
        doctors.save(doctor);
        return response(account);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        return authenticateUser(email, request.password());
    }

    @PostMapping("/login/phone")
    public AuthResponse loginWithPhone(@Valid @RequestBody PhoneLoginRequest request) {
        List<User> matches = users.findByPhone(request.phone().strip());
        if (matches.size() != 1) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return authenticateUser(matches.get(0).getEmail(), request.password());
    }

    private AuthResponse authenticateUser(String email, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        User user = users.findByEmail(email).orElseThrow();
        if (user.getRole() == UserRole.DOCTOR_PENDING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your doctor account is awaiting administrator approval.");
        }
        return response(user);
    }

    @GetMapping("/me")
    public Map<String, Object> currentUser(@AuthenticationPrincipal User user) {
        return Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(),
                "phone", user.getPhone() == null ? "" : user.getPhone(), "role", user.getRole().name(),
                "createdAt", user.getCreatedAt());
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwtService.issueToken(user), user.getId(), user.getName(),
                user.getEmail(), user.getRole().name());
    }

    public record RegisterRequest(@NotBlank @Size(max = 120) String name,
                                  @NotBlank @Email @Size(max = 190) String email,
                                  @NotBlank @Size(min = 8, max = 72) String password,
                                  @NotBlank @Size(max = 30) String phone) {}
    public record DoctorRegistrationRequest(@NotBlank @Size(max = 120) String name,
                                             @NotBlank @Email @Size(max = 190) String email,
                                             @NotBlank @Size(min = 12, max = 72) String password,
                                             @NotBlank @Size(max = 30) String phone,
                                             @NotBlank @Size(max = 80) String department,
                                             @NotBlank @Size(max = 120) String specialization,
                                             @Size(max = 200) String qualifications,
                                             @PositiveOrZero int experienceYears,
                                             @NotEmpty List<@NotBlank String> availableDays,
                                             @NotEmpty List<@NotBlank String> availableSlots,
                                             @PositiveOrZero double consultationFee,
                                             @Size(max = 1000) String bio) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record PhoneLoginRequest(@NotBlank @Size(max = 30) String phone, @NotBlank String password) {}
    public record AuthResponse(String token, Long id, String name, String email, String role) {}
}