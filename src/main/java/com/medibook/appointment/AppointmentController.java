package com.medibook.appointment;

import com.medibook.doctor.Doctor;
import com.medibook.doctor.DoctorRepository;
import com.medibook.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;

    public AppointmentController(AppointmentRepository appointments, DoctorRepository doctors) {
        this.appointments = appointments;
        this.doctors = doctors;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<AppointmentResponse> patientAppointments(@AuthenticationPrincipal User user) {
        return appointments.findByPatientIdOrderByAppointmentDateDescTimeSlotDesc(user.getId())
                .stream().map(AppointmentResponse::from).toList();
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<AppointmentResponse> doctorAppointments(@AuthenticationPrincipal User user) {
        Doctor doctor = doctors.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found"));
        return appointments.findByDoctorIdOrderByAppointmentDateDescTimeSlotDesc(doctor.getId())
                .stream().map(AppointmentResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Transactional
    public AppointmentResponse book(@AuthenticationPrincipal User patient,
                                    @Valid @RequestBody CreateAppointmentRequest request) {
        Doctor doctor = doctors.findByIdForBooking(request.doctorId())
                .filter(Doctor::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Available doctor not found"));
        if (request.date().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a future date");
        }
        if (!doctor.getAvailableDays().contains(request.date().getDayOfWeek().toString().charAt(0)
                + request.date().getDayOfWeek().toString().substring(1).toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor is not available on that day");
        }
        if (!List.of(doctor.getAvailableSlots().split(",")).contains(request.timeSlot().toString())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose one of the doctor's available time slots");
        }
        if (request.date().equals(LocalDate.now()) && !request.timeSlot().isAfter(LocalTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a future time");
        }
        if (appointments.existsByDoctorIdAndAppointmentDateAndTimeSlotAndStatusNot(
                doctor.getId(), request.date(), request.timeSlot(), AppointmentStatus.CANCELLED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That appointment slot is already booked");
        }
        return AppointmentResponse.from(appointments.save(new Appointment(
                patient, doctor, request.date(), request.timeSlot(), request.symptoms().strip())));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public AppointmentResponse cancel(@AuthenticationPrincipal User patient, @PathVariable Long id) {
        Appointment appointment = appointments.findByIdAndPatientId(id, patient.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed appointments cannot be cancelled");
        }
        appointment.updateStatus(AppointmentStatus.CANCELLED, null);
        return AppointmentResponse.from(appointments.save(appointment));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public AppointmentResponse updateStatus(@AuthenticationPrincipal User user, @PathVariable Long id,
                                            @Valid @RequestBody UpdateAppointmentRequest request) {
        Appointment appointment;
        if (user.getRole().name().equals("ADMIN")) {
            appointment = appointments.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        } else {
            Doctor doctor = doctors.findByUserId(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found"));
            appointment = appointments.findByIdAndDoctorId(id, doctor.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        }
        appointment.updateStatus(request.status(), request.notes());
        return AppointmentResponse.from(appointments.save(appointment));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AppointmentResponse> allAppointments() {
        return appointments.findAll().stream().map(AppointmentResponse::from).toList();
    }

    public record CreateAppointmentRequest(@NotNull Long doctorId, @NotNull @FutureOrPresent LocalDate date,
                                          @NotNull LocalTime timeSlot,
                                          @NotBlank @Size(max = 2000) String symptoms) {}
    public record UpdateAppointmentRequest(@NotNull AppointmentStatus status,
                                           @Size(max = 2000) String notes) {}
    public record AppointmentResponse(Long id, Long patientId, String patientName, String patientEmail,
                                      String patientPhone, Long doctorId, String doctorName, String department,
                                      LocalDate date, LocalTime timeSlot, String symptoms, String status,
                                      String notes, LocalDateTime createdAt) {
        public static AppointmentResponse from(Appointment appointment) {
            return new AppointmentResponse(appointment.getId(), appointment.getPatient().getId(),
                    appointment.getPatient().getName(), appointment.getPatient().getEmail(),
                    appointment.getPatient().getPhone(), appointment.getDoctor().getId(),
                    appointment.getDoctor().getUser().getName(), appointment.getDoctor().getDepartment(),
                    appointment.getAppointmentDate(), appointment.getTimeSlot(), appointment.getSymptoms(),
                    appointment.getStatus().name(), appointment.getDoctorNotes(),
                    appointment.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDateTime());
        }
    }
}