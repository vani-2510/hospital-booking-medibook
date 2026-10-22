package com.medibook.doctor;

import com.medibook.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "doctors", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 80)
    private String department;

    @Column(nullable = false, length = 120)
    private String specialization;

    @Column(length = 200)
    private String qualifications;

    private int experienceYears;

    @Column(nullable = false, length = 500)
    private String availableDays = "Monday,Tuesday,Wednesday,Thursday,Friday";

    @Column(nullable = false, length = 1000)
    private String availableSlots = "09:00,09:30,10:00,10:30,11:00,11:30,13:00,13:30,14:00,14:30,15:00";

    @Column(nullable = false)
    private double consultationFee;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 1000)
    private String bio;

    protected Doctor() {}

    public Doctor(User user, String department, String specialization, String qualifications,
                  int experienceYears, String availableDays, String availableSlots,
                  double consultationFee, String bio) {
        this.user = user;
        this.department = department;
        this.specialization = specialization;
        this.qualifications = qualifications;
        this.experienceYears = experienceYears;
        this.availableDays = availableDays;
        this.availableSlots = availableSlots;
        this.consultationFee = consultationFee;
        this.bio = bio;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getDepartment() { return department; }
    public String getSpecialization() { return specialization; }
    public String getQualifications() { return qualifications; }
    public int getExperienceYears() { return experienceYears; }
    public String getAvailableDays() { return availableDays; }
    public String getAvailableSlots() { return availableSlots; }
    public double getConsultationFee() { return consultationFee; }
    public boolean isActive() { return active; }
    public String getBio() { return bio; }

    public void updateSchedule(String days, String slots) {
        this.availableDays = days;
        this.availableSlots = slots;
    }

    public void updateConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public void updateProfile(String department, String specialization, String qualifications,
                              int experienceYears, String availableDays, String availableSlots,
                              double consultationFee, String bio) {
        this.department = department;
        this.specialization = specialization;
        this.qualifications = qualifications;
        this.experienceYears = experienceYears;
        this.availableDays = availableDays;
        this.availableSlots = availableSlots;
        this.consultationFee = consultationFee;
        this.bio = bio;
    }

    public void setActive(boolean active) { this.active = active; }
}