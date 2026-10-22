package com.medibook.doctor;

import com.medibook.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {
    private final DoctorRepository doctors;

    public DoctorController(DoctorRepository doctors) {
        this.doctors = doctors;
    }

    @GetMapping
    public List<DoctorResponse> list(@RequestParam(required = false) String department) {
        List<Doctor> results = department == null || department.isBlank()
                ? doctors.findByActiveTrueOrderByUserNameAsc()
                : doctors.findByActiveTrueAndDepartmentIgnoreCaseOrderByUserNameAsc(department.strip());
        return results.stream().map(DoctorResponse::from).toList();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorResponse currentDoctor(@AuthenticationPrincipal User user) {
        return doctors.findByUserId(user.getId()).map(DoctorResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found"));
    }

    @PutMapping("/me/schedule")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorResponse updateSchedule(@AuthenticationPrincipal User user,
                                        @Valid @RequestBody ScheduleRequest request) {
        Doctor doctor = doctors.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found"));
        doctor.updateSchedule(String.join(",", request.availableDays()), String.join(",", request.availableSlots()));
        return DoctorResponse.from(doctors.save(doctor));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public DoctorResponse setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Doctor doctor = doctors.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        doctor.setActive(Boolean.TRUE.equals(body.get("active")));
        return DoctorResponse.from(doctors.save(doctor));
    }

    public record ScheduleRequest(@NotEmpty List<@NotBlank String> availableDays,
                                 @NotEmpty List<@NotBlank String> availableSlots) {}

    public record DoctorResponse(Long id, Long userId, String name, String email, String phone,
                                 String department, String specialization, String qualifications,
                                 int experienceYears, List<String> availableDays, List<String> availableSlots,
                                 double consultationFee, boolean active, String bio) {
        public static DoctorResponse from(Doctor doctor) {
            return new DoctorResponse(doctor.getId(), doctor.getUser().getId(), doctor.getUser().getName(),
                    doctor.getUser().getEmail(), doctor.getUser().getPhone(), doctor.getDepartment(),
                    doctor.getSpecialization(), doctor.getQualifications(), doctor.getExperienceYears(),
                    Arrays.asList(doctor.getAvailableDays().split(",")),
                    Arrays.asList(doctor.getAvailableSlots().split(",")), doctor.getConsultationFee(),
                    doctor.isActive(), doctor.getBio());
        }
    }
}