package com.medibook.admin;

import com.medibook.appointment.AppointmentRepository;
import com.medibook.appointment.AppointmentController.AppointmentResponse;
import com.medibook.doctor.Doctor;
import com.medibook.doctor.DoctorController.DoctorResponse;
import com.medibook.doctor.DoctorRepository;
import com.medibook.user.User;
import com.medibook.user.UserRepository;
import com.medibook.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserRepository users;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository users, DoctorRepository doctors,
                           AppointmentRepository appointments, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.doctors = doctors;
        this.appointments = appointments;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return users.findAll().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/patients")
    public List<UserResponse> patients() {
        return users.findAll().stream()
                .filter(user -> user.getRole() == UserRole.PATIENT)
                .map(UserResponse::from).toList();
    }

    @GetMapping("/appointments")
    public List<AppointmentResponse> appointments() {
        return appointments.findAll().stream().map(AppointmentResponse::from).toList();
    }

    @GetMapping("/doctors")
    public List<DoctorResponse> doctors() {
        return doctors.findAll().stream()
                .filter(doctor -> doctor.getUser().getRole() == UserRole.DOCTOR)
                .map(DoctorResponse::from).toList();
    }

    @GetMapping("/doctor-requests")
    public List<DoctorResponse> doctorRequests() {
        return doctors.findAll().stream()
                .filter(doctor -> doctor.getUser().getRole() == UserRole.DOCTOR_PENDING)
                .map(DoctorResponse::from).toList();
    }

    @PatchMapping("/doctor-requests/{id}/approve")
    @Transactional
    public DoctorResponse approveDoctorRequest(@PathVariable Long id) {
        Doctor doctor = doctors.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor request not found"));
        if (doctor.getUser().getRole() != UserRole.DOCTOR_PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This account is not a pending doctor request");
        }
        doctor.getUser().updateRole(UserRole.DOCTOR);
        doctor.setActive(true);
        users.save(doctor.getUser());
        return DoctorResponse.from(doctors.save(doctor));
    }

    @DeleteMapping("/doctor-requests/{id}")
    @Transactional
    public void rejectDoctorRequest(@PathVariable Long id) {
        Doctor doctor = doctors.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor request not found"));
        if (doctor.getUser().getRole() != UserRole.DOCTOR_PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This account is not a pending doctor request");
        }
        doctors.delete(doctor);
        users.delete(doctor.getUser());
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        long pending = appointments.findAll().stream()
                .filter(appointment -> appointment.getStatus().name().equals("PENDING")).count();
        long activeDoctors = doctors.findAll().stream()
            .filter(doctor -> doctor.getUser().getRole() == UserRole.DOCTOR && doctor.isActive()).count();
        return Map.of("users", users.count(), "doctors", activeDoctors,
                "appointments", appointments.count(), "pendingAppointments", pending);
    }

    @PostMapping("/doctors")
    public DoctorResponse addDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        String phone = request.phone() == null ? null : request.phone().strip();
        if (phone != null && !phone.isBlank() && users.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this phone number already exists");
        }
        User account = users.save(new User(request.name().strip(), email,
                passwordEncoder.encode(request.password()), phone, UserRole.DOCTOR));
        Doctor doctor = new Doctor(account, request.department().strip(), request.specialization().strip(),
                request.qualifications(), request.experienceYears(), String.join(",", request.availableDays()),
                String.join(",", request.availableSlots()), request.consultationFee(), request.bio());
        return DoctorResponse.from(doctors.save(doctor));
    }

    public record CreateDoctorRequest(@NotBlank @Size(max = 120) String name,
                                     @NotBlank @Email @Size(max = 190) String email,
                                     @NotBlank @Size(min = 12, max = 72) String password,
                                     @Size(max = 30) String phone,
                                     @NotBlank @Size(max = 80) String department,
                                     @NotBlank @Size(max = 120) String specialization,
                                     @Size(max = 200) String qualifications,
                                     @PositiveOrZero int experienceYears,
                                     @NotEmpty List<@NotBlank String> availableDays,
                                     @NotEmpty List<@NotBlank String> availableSlots,
                                     @PositiveOrZero double consultationFee,
                                     @Size(max = 1000) String bio) {}

    public record UserResponse(Long id, String name, String email, String phone, String role,
                               java.time.Instant createdAt) {
        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                    user.getRole().name(), user.getCreatedAt());
        }
    }
}