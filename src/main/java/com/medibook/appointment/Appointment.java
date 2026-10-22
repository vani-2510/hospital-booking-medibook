package com.medibook.appointment;

import com.medibook.doctor.Doctor;
import com.medibook.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "time_slot", nullable = false)
    private LocalTime timeSlot;

    @Column(nullable = false, length = 2000)
    private String symptoms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(length = 2000)
    private String doctorNotes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Appointment() {}

    public Appointment(User patient, Doctor doctor, LocalDate appointmentDate,
                       LocalTime timeSlot, String symptoms) {
        this.patient = patient;
        this.doctor = doctor;
        this.appointmentDate = appointmentDate;
        this.timeSlot = timeSlot;
        this.symptoms = symptoms;
    }

    public Long getId() { return id; }
    public User getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public LocalTime getTimeSlot() { return timeSlot; }
    public String getSymptoms() { return symptoms; }
    public AppointmentStatus getStatus() { return status; }
    public String getDoctorNotes() { return doctorNotes; }
    public Instant getCreatedAt() { return createdAt; }

    public void updateStatus(AppointmentStatus status, String notes) {
        this.status = status;
        if (notes != null) this.doctorNotes = notes;
    }
}